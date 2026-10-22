import { RouterProvider } from "react-router-dom";
import router from "./routes/index.jsx";
import { useSelector } from 'react-redux';
import { useEffect } from "react";
import { fetchAccount } from './context/slice/accountSlice.ts';
import Loading from "./components/share/reloading/Loading.jsx";
import { useAppDispatch } from "./context/hooks.ts";
import { handleRefreshToken } from "./config/axios.customize";

function App() {
  const dispatch = useAppDispatch()
  const isLoading = useSelector(state => state.account.isLoading)

  useEffect(() => {
    const path = window.location.pathname;
    (async () => {

      let token = localStorage.getItem('access_token');
      if (!token) {
        token = await handleRefreshToken();
        // [debug auth] xoá được sau khi xác nhận luồng chạy
        console.info('[auth] bootstrap: không có access_token trong localStorage → gọi /auth/refresh:',
          token ? 'OK, đã lấy token mới' : 'THẤT BẠI (cookie hết hạn hoặc không gửi được)');
      }

      let user = null;
      try {
        user = (await dispatch(fetchAccount()).unwrap())?.user ?? null;
      } catch (error) {
        console.warn('[auth] fetchAccount lỗi:', error); // debug auth
      }

      // Đang login mà khôi phục được phiên vào thẳng trang chủ.
      if (user && (path === '/login' || path === '/sign-up')) {
        router.navigate('/', { replace: true });
      }
    })();
  }, [])
  return (
    <>
      {
        isLoading === false || window.location.pathname === '/login' || window.location.pathname === '/sign-up'
          ? <RouterProvider router={router} /> : <Loading />
      }
    </>
  );
}

export default App;
