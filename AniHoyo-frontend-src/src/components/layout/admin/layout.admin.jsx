import { useState, useEffect } from "react";
import { useLocation, Outlet } from "react-router-dom";
import { Layout, Drawer, Affix } from "antd";
import HeaderAdmin from "../../admin/Header";
import FooterAdmin from "../../admin/Footer";
import SideNav from "../../admin/side.nav";
import { useSelector } from "react-redux";

const { Header: AntHeader, Content, Sider } = Layout;

const MadContent = (props) => {
  const [visible, setVisible] = useState(false);
  const [sidenavColor, setSideNavColor] = useState("#1890ff");
  // Derive everything from the router location: reading window.location during
  // render (and assuming user.role is always present) crashes the layout.
  const user = useSelector(state => state.account.user)
  const userRole = user?.role?.name

  const openDrawer = () => setVisible(!visible);
  const handleSidenavColor = (color) => setSideNavColor(color);
  const handleFixedNavbar = (type) => setFixed(type);

  let { pathname } = useLocation();
  const isAdminRoute = pathname.startsWith('/admin')
  pathname = pathname.replace("/", "");
  return (
    <Layout
      className={`layout-dashboard ${pathname === "profile" ? "layout-profile" : ""
        } `}
    >
      {isAdminRoute && userRole === 'ADMIN' &&
        <Drawer
          title={false}
          placement={"right"}
          closable={false}
          onClose={() => setVisible(false)}
          key={"right"}
          width={250}
          className={`drawer-sidebar`}
        >
          <Layout
            className={`layout-dashboard`}
          >
            <Sider
              trigger={null}
              width={250}
              theme="light"
              className={`sider-primary ant-layout-sider-primary `}
              style={{ background: "transparent" }}
            >
              <SideNav color={sidenavColor} />
            </Sider>
          </Layout>
        </Drawer>
      }
      {isAdminRoute && userRole === 'ADMIN' &&
        <Sider
          breakpoint="lg"
          collapsedWidth="0"
          onCollapse={(collapsed, type) => {
            console.log(collapsed, type);
          }}
          trigger={null}
          width={250}
          theme="light"
          className={`sider-primary ant-layout-sider-primary`}
          style={{ background: "transparent" }}
        >
          <SideNav color={sidenavColor} />
        </Sider>
      }

      <Layout>
        <Affix>
          <AntHeader className={`${"ant-header-fixed"}`}>
            <HeaderAdmin
              onPress={openDrawer}
              name={pathname}
              subName={pathname}
              handleSidenavColor={handleSidenavColor}
              handleFixedNavbar={handleFixedNavbar}
            />
          </AntHeader>
        </Affix>

        <Content className="content-ant">
          <Outlet />
        </Content>

        {isAdminRoute && userRole === 'ADMIN' &&
          <FooterAdmin />
        }
      </Layout>
    </Layout>
  )
}

export default MadContent