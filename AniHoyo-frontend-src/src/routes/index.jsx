import { createBrowserRouter } from "react-router-dom";
import DetailsPage from "../pages/client/film.detail.fixed";
import SingInPage from "../pages/auth/login.page";
import RegisterPage from "../pages/auth/register.page";
import AdminHome from "../pages/admin/admin.dashboard";
import UserManage from "../pages/admin/user.table";
import ProductManage from "../pages/admin/product.table";
import MadContent from "../components/layout/admin/layout.admin";
import "../assets/styles/mainClient.scss"
import Layout from "../components/layout/client/layout";
import Error404 from "../components/errors/404-page";
import ClientHome from "../pages/client/client.home";

import RolePage from "@/pages/admin/role.table";
import ProtectedRoute from "../components/share/protected";
import PermissionPage from "@/pages/admin/permission.table";
import CategoryPage from "@/pages/admin/category.table";
import TagPage from "@/pages/admin/tag.table";
import FilmWatching from "@/pages/client/film.watching";
import LayoutApp from "../components/share/layout.app";

const router = createBrowserRouter([
  {
    path: "/",
    element: <LayoutApp><Layout /></LayoutApp>,
    errorElement: <Error404 />,
    children: [
      {
        index: true,
        element: <ClientHome />
      },
      {
        path: "/detail/:slug",
        element: <ProtectedRoute><DetailsPage /></ProtectedRoute>
      },
      {
        path: "/watching/:slug",
        element: <ProtectedRoute><FilmWatching /></ProtectedRoute>
      },
    ]
  },
  {
    path: "/admin",
    element: <LayoutApp><MadContent /></LayoutApp>,
    errorElement: <Error404 />,
    children: [
      {
        index: true,
        element: <ProtectedRoute>
          <AdminHome />
        </ProtectedRoute>
      },
      {
        path: "table-users",
        element: <ProtectedRoute>
          <UserManage />
        </ProtectedRoute>
      },
      {
        path: "table-role",
        element: <ProtectedRoute>
          <RolePage />
        </ProtectedRoute>
      },
      {
        path: "table-permission",
        element: <ProtectedRoute>
          <PermissionPage />
        </ProtectedRoute>
      },
      {
        path: "table-category",
        element: <ProtectedRoute>
          <CategoryPage />
        </ProtectedRoute>
      },
      {
        path: "table-tag",
        element: <ProtectedRoute>
          <TagPage />
        </ProtectedRoute>
      },
      {
        path: "table-films",
        element: <ProtectedRoute>
          <ProductManage />
        </ProtectedRoute>
      },
    ]
  },
  {
    path: "/sign-up",
    element: <RegisterPage />,
  },
  {
    path: "/login",
    element: <SingInPage />,
  },
]);
export default router