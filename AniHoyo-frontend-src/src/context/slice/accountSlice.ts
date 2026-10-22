import { callFetchAccountAPI } from '@/config/api.handle';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';

// First, create the thunk
export const fetchAccount = createAsyncThunk(
    'account/fetchAccount',
    async () => {
        const response = await callFetchAccountAPI();
        return response.data;
    }
)
interface IState {
    isAuthenticated: boolean;
    isLoading: boolean;
    isRefreshToken: boolean;
    errorRefreshToken: string;
    user: {
        id: string;
        avatar: string;
        email: string;
        name: string;
        role: {
            id?: string;
            name?: string;
            permissions?: {
                id: string;
                name: string;
                apiPath: string;
                method: string;
                module: string;
            }[]
        }
    };
}
const initialState: IState = {
    isAuthenticated: false,
    isLoading: true,
    isRefreshToken: false,
    errorRefreshToken: "",
    user: {
        id: "",
        avatar: "",
        email: "",
        name: "",
        role: {
            id: "",
            name: "",
            permissions: [],
        },
    }
};
export const accountSlice = createSlice({
    name: 'account',
    initialState,
    // The `reducers` field lets us define reducers and generate associated actions
    reducers: {
        runLoginAction: (state, action) => {
            state.isAuthenticated = true;
            state.isLoading = false;
            state.user.id = action?.payload?.id;
            state.user.email = action?.payload?.email;
            state.user.name = action?.payload?.name;
            state.user.avatar = action?.payload?.avatar;
            state.user.role = action?.payload?.role ?? {
                id: "",
                name: "",
                permissions: []
            };
            if (!state.user.role.permissions) {
                state.user.role.permissions = [];
            }
        },
        runLogoutAction: (state, action) => {
            state.isAuthenticated = false;
            localStorage.removeItem('access_token');
            state.user = {
                id: "",
                email: "",
                name: "",
                avatar: "",
                role: {
                    id: "",
                    name: "",
                    permissions: [],
                },
            }
        },
        setRefreshTokenAction: (state, action) => {
            state.isRefreshToken = action.payload?.status ?? false;
            state.errorRefreshToken = action.payload?.message ?? "";
        },
        // doUploadAvatarAction: (state, action) => {
        //     state.avatar = action.payload.avatar
        // },
        doUpdateUserAction: (state, action) => {
            state.user.avatar = action.payload.avatar;
            state.user.name = action.payload.name;
        }
    },
    extraReducers: (builder) => {
        builder.addCase(fetchAccount.pending, (state) => {
            state.isLoading = true;
        })

        builder.addCase(fetchAccount.fulfilled, (state, action) => {
            // /api/v1/auth/account nằm trong whitelist permitAll của backend: khi
            // request không kèm token nó vẫn trả 200 với `user: null` (không phải
            // 401), nên phải kiểm tra payload.user chứ không chỉ payload — nếu
            // không sẽ coi là đã đăng nhập với role rỗng và trả 403 oan.
            const user = action.payload?.user;
            if (user) {
                state.isAuthenticated = true;
                state.user.id = user.id;
                state.user.email = user.email;
                state.user.name = user.name;
                state.user.avatar = user.avatar;

                // Gán role với default value là empty object nếu không có
                state.user.role = user.role ?? {
                    id: "",
                    name: "",
                    permissions: []
                };

                // Đảm bảo permissions luôn là array
                if (!state.user.role.permissions) {
                    state.user.role.permissions = [];
                }
            } else {
                state.isAuthenticated = false;
                state.user = {
                    id: "",
                    email: "",
                    name: "",
                    avatar: "",
                    role: {
                        id: "",
                        name: "",
                        permissions: [],
                    },
                };
            }
            state.isLoading = false;
        })

        builder.addCase(fetchAccount.rejected, (state) => {
            state.isAuthenticated = false;
            state.isLoading = false;
        })
    },
});

export const {
    runLoginAction,
    runLogoutAction,
    doUpdateUserAction,
    setRefreshTokenAction } = accountSlice.actions;

export default accountSlice.reducer;