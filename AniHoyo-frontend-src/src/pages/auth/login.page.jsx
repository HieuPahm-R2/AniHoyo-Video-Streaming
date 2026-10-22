import { Link, useNavigate } from "react-router-dom";
import { useDispatch } from "react-redux";
import {
  Layout, Menu, Button, Row, Col, Typography, Form, Input, Switch, message, notification,
} from "antd";
import signinbg from "../../assets/images/login-intro1.jpg";
import {
  DribbbleOutlined, TwitterOutlined, InstagramOutlined, GithubOutlined,
} from "@ant-design/icons";
import { loginAPI } from "../../config/api.handle";
import { useState } from "react";
import { runLoginAction } from "../../context/slice/accountSlice";

const { Title } = Typography;
const { Header, Footer, Content } = Layout;

const template = [
  <svg
    data-v-4ebdc598=""
    width="20"
    height="20"
    viewBox="0 0 20 20"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
  >
    <path
      data-v-4ebdc598=""
      d="M3 4C3 3.44772 3.44772 3 4 3H16C16.5523 3 17 3.44772 17 4V6C17 6.55228 16.5523 7 16 7H4C3.44772 7 3 6.55228 3 6V4Z"
      fill="#111827"
      className="fill-muted"
    ></path>
    <path
      data-v-4ebdc598=""
      d="M3 10C3 9.44771 3.44772 9 4 9H10C10.5523 9 11 9.44771 11 10V16C11 16.5523 10.5523 17 10 17H4C3.44772 17 3 16.5523 3 16V10Z"
      fill="#111827"
      className="fill-muted"
    ></path>
    <path
      data-v-4ebdc598=""
      d="M14 9C13.4477 9 13 9.44771 13 10V16C13 16.5523 13.4477 17 14 17H16C16.5523 17 17 16.5523 17 16V10C17 9.44771 16.5523 9 16 9H14Z"
      fill="#111827"
      className="fill-muted"
    ></path>
  </svg>,
];
const profile = [
  <svg
    data-v-4ebdc598=""
    width="20"
    height="20"
    viewBox="0 0 20 20"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
  >
    <path
      data-v-4ebdc598=""
      fillRule="evenodd"
      clipRule="evenodd"
      d="M18 10C18 14.4183 14.4183 18 10 18C5.58172 18 2 14.4183 2 10C2 5.58172 5.58172 2 10 2C14.4183 2 18 5.58172 18 10ZM12 7C12 8.10457 11.1046 9 10 9C8.89543 9 8 8.10457 8 7C8 5.89543 8.89543 5 10 5C11.1046 5 12 5.89543 12 7ZM9.99993 11C7.98239 11 6.24394 12.195 5.45374 13.9157C6.55403 15.192 8.18265 16 9.99998 16C11.8173 16 13.4459 15.1921 14.5462 13.9158C13.756 12.195 12.0175 11 9.99993 11Z"
      fill="#111827"
      className="fill-muted"
    ></path>
  </svg>,
];
const signup = [
  <svg
    data-v-4ebdc598=""
    width="20"
    height="20"
    viewBox="0 0 20 20"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
  >
    <path
      data-v-4ebdc598=""
      fillRule="evenodd"
      clipRule="evenodd"
      d="M6 2C5.44772 2 5 2.44772 5 3V4H4C2.89543 4 2 4.89543 2 6V16C2 17.1046 2.89543 18 4 18H16C17.1046 18 18 17.1046 18 16V6C18 4.89543 17.1046 4 16 4H15V3C15 2.44772 14.5523 2 14 2C13.4477 2 13 2.44772 13 3V4H7V3C7 2.44772 6.55228 2 6 2ZM6 7C5.44772 7 5 7.44772 5 8C5 8.55228 5.44772 9 6 9H14C14.5523 9 15 8.55228 15 8C15 7.44772 14.5523 7 14 7H6Z"
      fill="#111827"
      className="fill-muted"
    ></path>
  </svg>,
];


const SingInPage = () => {
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();
  const dispatch = useDispatch();

  const onFinish = async (values) => {
    const { username, password } = values;
    setIsLoading(true);
    const res = await loginAPI(username, password);
    setIsLoading(false);
    if (res?.data) {
      localStorage.setItem('access_token', res.data.access_token);
      dispatch(runLoginAction(res.data.user))
      message.success("Everything done");
      navigate("/")
    } else {
      notification.error({
        message: "Something went wrongs",
        description: "Failed action, try again..!",
      })
    }
  };

  return (
    <>
      <Layout className="layout-default layout-signin">
        <Header>
          <div className="header-col header-brand">
            <h5>AniHoyo VietSub</h5>
          </div>
          <div className="header-col header-nav">
            <Menu mode="horizontal" defaultSelectedKeys={["1"]}>
              <Menu.Item key="1">
                <Link to="/">
                  {template}
                  <span>Trang chủ</span>
                </Link>
              </Menu.Item>
              <Menu.Item key="2">
                <Link to="/profile">
                  {profile}
                  <span>Profile</span>
                </Link>
              </Menu.Item>
              <Menu.Item key="3">
                <Link to="/sign-up">
                  {signup}
                  <span> Đăng ký</span>
                </Link>
              </Menu.Item>
            </Menu>
          </div>
          <div className="header-col header-btn">
            <Button type="primary">FREE DOWNLOAD</Button>
          </div>
        </Header>
        <Content className="signin">
          <Row gutter={[24, 0]} justify="space-around">
            <Col
              xs={{ span: 24, offset: 0 }}
              lg={{ span: 6, offset: 2 }}
              md={{ span: 12 }}
            >
              <Title className="mb-15">Đăng nhập</Title>
              <Title className="font-regular text-muted" level={5}>
                Nhập email hoặc username đã đăng ký trước đó
              </Title>
              <Form
                onFinish={onFinish}
                layout="vertical"
                className="row-col"
                initialValues={{
                  remember: true
                }}
              >
                <Form.Item
                  className="username"
                  label="Email/username"
                  name="username"
                  rules={[
                    {
                      required: true,
                      message: "Hãy nhập chính xác email!",
                    },
                  ]}
                >
                  <Input placeholder="Email" />
                </Form.Item>

                <Form.Item
                  className="username"
                  label="Password"
                  name="password"
                  rules={[
                    {
                      required: true,
                      message: "Không để trống mật khẩu!",
                    },
                  ]}
                >
                  <Input.Password placeholder="Password" />
                </Form.Item>

                <Form.Item
                  name="remember"
                  className="aligin-center"
                  valuePropName="checked"
                >
                  <Switch defaultChecked />
                  Remember me
                </Form.Item>

                <Form.Item>
                  <Button
                    type="primary"
                    htmlType="submit"
                    style={{ width: "100%" }}
                  >
                    Đăng nhập ngay
                  </Button>
                </Form.Item>
                <p className="font-semibold text-muted">
                  Chưa có tài khoản?{" "}
                  <Link to="/sign-up" className="text-dark font-bold">
                    Đăng ký
                  </Link>
                </p>
              </Form>
            </Col>
            <Col
              className="sign-img"
              style={{ padding: 12 }}
              xs={{ span: 24 }}
              lg={{ span: 12 }}
              md={{ span: 12 }}
            >
              <img src={signinbg} alt="" />
            </Col>
          </Row>
        </Content>
        <Footer>
          <Menu mode="horizontal">
            <Menu.Item>Về AniHoyo Vietsub</Menu.Item>
            <Menu.Item>Teams</Menu.Item>
            <Menu.Item>Blogs</Menu.Item>
          </Menu>
          <Menu mode="horizontal" className="menu-nav-social">
            <Menu.Item>
              <Link to="#">{<DribbbleOutlined />}</Link>
            </Menu.Item>
            <Menu.Item>
              <Link to="#">{<TwitterOutlined />}</Link>
            </Menu.Item>
            <Menu.Item>
              <Link to="#">{<InstagramOutlined />}</Link>
            </Menu.Item>
            <Menu.Item>
              <Link to="#">
                <svg
                  width="18"
                  height="18"
                  xmlns="http://www.w3.org/2000/svg"
                  viewBox="0 0 512 512"
                >
                  <path d="M496 256c0 137-111 248-248 248-25.6 0-50.2-3.9-73.4-11.1 10.1-16.5 25.2-43.5 30.8-65 3-11.6 15.4-59 15.4-59 8.1 15.4 31.7 28.5 56.8 28.5 74.8 0 128.7-68.8 128.7-154.3 0-81.9-66.9-143.2-152.9-143.2-107 0-163.9 71.8-163.9 150.1 0 36.4 19.4 81.7 50.3 96.1 4.7 2.2 7.2 1.2 8.3-3.3.8-3.4 5-20.3 6.9-28.1.6-2.5.3-4.7-1.7-7.1-10.1-12.5-18.3-35.3-18.3-56.6 0-54.7 41.4-107.6 112-107.6 60.9 0 103.6 41.5 103.6 100.9 0 67.1-33.9 113.6-78 113.6-24.3 0-42.6-20.1-36.7-44.8 7-29.5 20.5-61.3 20.5-82.6 0-19-10.2-34.9-31.4-34.9-24.9 0-44.9 25.7-44.9 60.2 0 22 7.4 36.8 7.4 36.8s-24.5 103.8-29 123.2c-5 21.4-3 51.6-.9 71.2C65.4 450.9 0 361.1 0 256 0 119 111 8 248 8s248 111 248 248z"></path>
                </svg>
              </Link>
            </Menu.Item>
            <Menu.Item>
              <Link to="#">{<GithubOutlined />}</Link>
            </Menu.Item>
          </Menu>
          <p className="copyright">
            {" "}
            Copyright © 2025 AniHoyo by <a href="#pablo">HieuPahm-R2</a>.{" "}
          </p>
        </Footer>
      </Layout>
    </>
  )
}
export default SingInPage