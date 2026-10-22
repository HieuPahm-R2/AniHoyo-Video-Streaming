import Error403 from "@/components/errors/403-page"
import Loading from "@/components/share/reloading/Loading"
import { useAppSelector } from "@/context/hooks"
import { Navigate, useLocation } from "react-router-dom"


const RoleCheck = (props) => {
    // Read the path from the router instead of window.location: window is read
    // during render, so it does not participate in React's update cycle and can
    // still hold the previous URL when the render is deferred/retried.
    const { pathname } = useLocation()
    const isLoading = useAppSelector(state => state.account.isLoading)
    const user = useAppSelector(state => state.account.user)
    const userRole = user?.role?.name
    const isAdmin = pathname.startsWith("/admin")

    // The role is still unknown while the account is being fetched (hard refresh,
    // right after login, token refresh). Showing 403 at that moment is what made
    // a normal navigation look forbidden until the page was reloaded.
    if (!userRole) {
        return isLoading ? <Loading /> : <Error403 />
    }

    const isAllowed = isAdmin
        ? userRole === 'ADMIN'
        : userRole === 'USER' || userRole === 'ADMIN'

    return isAllowed ? (<>{props.children}</>) : (<Error403 />)
}

const ProtectedRoute = (props) => {
    const isAuthenticated = useAppSelector(state => state.account.isAuthenticated)
    return (
        <>
            {isAuthenticated === true ?
                <>
                    <RoleCheck>{props.children}</RoleCheck>
                </> : <Navigate to="/login" replace />
            }
        </>
    )
}
export default ProtectedRoute;