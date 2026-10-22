
import { setRefreshTokenAction } from "@/context/slice/accountSlice";
import { notification } from "antd";
import axios from "axios";

interface AccessTokenResponse {
    access_token: string;
}

/** Envelope mà backend bọc quanh mọi response 2xx (FormatRestResponse). */
interface BackendEnvelope<T> {
    statusCode: number;
    message: string;
    data: T;
}

const NO_RETRY_HEADER = 'x-no-retry';

const instance = axios.create({
    baseURL: import.meta.env.VITE_BACKEND_URL as string,
    withCredentials: true
});

// Client riêng, CHỈ dùng để gọi /auth/refresh. Cố tình không có request
// interceptor nên access token (đã hết hạn) không bao giờ bị gắn vào header.
// BearerTokenAuthenticationFilter của Spring trả 401 cho mọi request mang bearer
// token không hợp lệ *trước khi* xét rules phân quyền, nên nếu gửi kèm token hết
// hạn thì /auth/refresh luôn 401 dù path nằm trong permitAll và cookie còn hạn.
const refreshClient = axios.create({
    baseURL: import.meta.env.VITE_BACKEND_URL as string,
    withCredentials: true
});

// Cùng quy ước với `instance`: bóc sẵn body của backend để call-site làm việc
// với envelope { statusCode, message, data }. (src/types/file.d.ts khai báo
// `AxiosResponse<T> extends Promise<T>`, nên TS cũng coi `await ...get<T>()`
// là trả về T — nhờ vậy code và type khớp nhau.)
refreshClient.interceptors.response.use((res) => res.data);

// Deduplicate concurrent refresh calls — all 401 handlers share one in-flight promise
let refreshTokenPromise: Promise<string | null> | null = null;

/**
 * Đổi cookie `refresh-token` (httpOnly) lấy access token mới.
 *
 * Dùng được cả khi app vừa khởi động và chưa có access token nào trong
 * localStorage — nhờ vậy cookie còn hạn vẫn cứu được phiên.
 *
 * @returns access token mới, hoặc null nếu refresh token hết hạn/không gửi được.
 */
export const handleRefreshToken = async (): Promise<string | null> => {
    if (refreshTokenPromise) return refreshTokenPromise;

    refreshTokenPromise = (async () => {
        try {
            const res = await refreshClient.get<BackendEnvelope<AccessTokenResponse>>('/api/v1/auth/refresh');
            // Nhờ interceptor bóc body ở trên, `res` chính là envelope:
            // { statusCode, message, data: { access_token, user } }
            const access_token = res?.data?.access_token ?? null;
            if (access_token) {
                localStorage.setItem('access_token', access_token);
            }
            return access_token;
        } catch (error) {
            // [debug auth] xoá được sau khi xác nhận luồng chạy
            console.warn('[auth] gọi /api/v1/auth/refresh thất bại:', (error as any)?.message ?? error);
            return null;
        } finally {
            refreshTokenPromise = null;
        }
    })();

    return refreshTokenPromise;
};

// Request interceptor — attach access token
instance.interceptors.request.use(function (config) {
    if (typeof window !== "undefined" && window && window.localStorage && window.localStorage.getItem('access_token')) {
        config.headers.Authorization = 'Bearer ' + window.localStorage.getItem('access_token');
    }
    if (!config.headers.Accept) {
        config.headers.Accept = "application/json";
    }
    if (!config.headers["Content-Type"]) {
        config.headers["Content-Type"] = "application/json; charset=utf-8";
    }
    return config;
});

// Response interceptor — handle token refresh on 401
instance.interceptors.response.use(
    (res) => res.data,
    async (error) => {
        // Guard: no response means network error — reject immediately
        if (!error.response) {
            return Promise.reject(error);
        }

        const status = +error.response.status;
        const url: string = error.config?.url ?? '';

        // 401 on a normal API call (not login/refresh, not already retried) → try refresh
        if (
            status === 401
            && error.config
            && url !== '/api/v1/auth/login'
            && url !== '/api/v1/auth/refresh'
            && !error.config.headers[NO_RETRY_HEADER]
        ) {
            const access_token = await handleRefreshToken();
            // [debug auth] xoá được sau khi xác nhận luồng chạy
            console.info('[auth] gặp 401 ở', url, '→ gọi /auth/refresh:', access_token ? 'OK' : 'THẤT BẠI');
            // Đánh dấu trước khi retry: nếu request mới vẫn 401 thì không refresh vòng lặp.
            error.config.headers[NO_RETRY_HEADER] = 'true';
            if (access_token) {
                error.config.headers['Authorization'] = `Bearer ${access_token}`;
                return instance.request(error.config);
            }
            // Refresh thất bại (cookie hết hạn / không gửi được) → để LayoutApp xoá
            // token cũ và điều hướng về /login qua Redux.
            const message = error?.response?.data?.error ?? "Phiên đăng nhập hết hạn, vui lòng đăng nhập lại.";
            dispatch(setRefreshTokenAction({ status: true, message }));
            return error?.response?.data ?? Promise.reject(error);
        }

        // 401 on the refresh endpoint itself — refresh token is expired.
        // (handleRefreshToken đi qua refreshClient nên không rơi vào đây; nhánh này
        // giữ lại phòng khi có chỗ khác gọi refresh bằng `instance`.)
        if (status === 401 && url === '/api/v1/auth/refresh') {
            const message = error?.response?.data?.error ?? "Có lỗi xảy ra, vui lòng login.";
            dispatch(setRefreshTokenAction({ status: true, message }));
        }

        // Hiển thị đúng thông điệp backend trả về cho MỌI lỗi 4xx/5xx, không chỉ
        // 403 như trước: một lỗi dữ liệu (vd "Data too long for column 'trailer'")
        // trước đây bị nuốt im lặng nên rất khó đoán nguyên nhân.
        // 401 đã do luồng refresh phiên phía trên xử lý (retry hoặc dispatch
        // setRefreshTokenAction), riêng lỗi đăng nhập thì vẫn báo để người dùng
        // biết sai tài khoản/mật khẩu.
        const handledBySessionFlow = status === 401
            && (error.config?.url ?? '') !== '/api/v1/auth/login';
        if (status >= 400 && !handledBySessionFlow) {
            const body = error?.response?.data;
            const title = typeof body?.message === 'string' && body.message
                ? body.message
                : `Lỗi HTTP ${status}`;
            const description = typeof body?.error === 'string' && body.error
                ? body.error
                : (typeof body?.message === 'string' ? body.message : '');
            notification.error({
                message: title,
                description: description || undefined
            });
        }

        // Giữ nguyên "hợp đồng" cũ của app: lỗi HTTP được resolve về body lỗi thay
        // vì reject, vì mọi call-site đều kiểm tra `if (res && res.data)` và nhiều
        // chỗ không có try/catch (vd: refetchData trong product.table.jsx sẽ kẹt
        // loading nếu promise bị reject).
        return error?.response?.data ?? Promise.reject(error);
    }
);

export default instance;

let dispatch: any;

export const injectStore = (_dispatch: any) => {
    dispatch = _dispatch;
};
