import { useAppDispatch, useAppSelector } from "@/context/hooks";
import { setRefreshTokenAction } from "@/context/slice/accountSlice";
import { message } from "antd";
import { useEffect } from "react";
import { useLocation, useNavigate } from "react-router-dom";

interface IProps {
    children: React.ReactNode
}
const LayoutApp = (props: IProps) => {
    const isRefreshToken = useAppSelector(state => state.account.isRefreshToken);
    const errorRefreshToken = useAppSelector(state => state.account.errorRefreshToken);
    const navigate = useNavigate();
    const dispatch = useAppDispatch();
    const { pathname } = useLocation();

    useEffect(() => {
        window.scrollTo(0, 0);
    }, [pathname]);

    useEffect(() => {
        if (isRefreshToken == true) {
            localStorage.removeItem('access_token')
            message.error(errorRefreshToken)
            dispatch(setRefreshTokenAction({ status: false, message: "yêu cầu đăng nhập!" }))
            navigate("/login");
        }
    }, [isRefreshToken]);
    return (
        <>
            {props.children}
        </>
    )
}
export default LayoutApp