import { Layout, Menu, Button, Typography, Card, Form, Input, Checkbox, message, notification } from "antd";
import logo1 from "../../assets/images/logos-facebook.svg";
import logo2 from "../../assets/images/logo-apple.svg";
import logo3 from "../../assets/images/Google__G__Logo.svg.png";

import { Link, useNavigate } from "react-router-dom";
import { DribbbleOutlined, TwitterOutlined, InstagramOutlined, GithubOutlined } from "@ant-design/icons";
import { registerAPI } from "../../config/api.handle";
import { useState } from "react";

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

const signin = [
  <svg
    xmlns="http://www.w3.org/2000/svg"
    width="14"
    height="14"
    viewBox="0 0 14 14"
  >
    <path
      className="fill-muted"
      d="M12.25,14H1.75A1.752,1.752,0,0,1,0,12.25V3.5A1.752,1.752,0,0,1,1.75,1.75h.876V.875a.875.875,0,0,1,1.75,0V1.75h5.25V.875a.875.875,0,0,1,1.75,0V1.75h.875A1.752,1.752,0,0,1,14,3.5v8.75A1.752,1.752,0,0,1,12.25,14ZM3.5,4.375a.875.875,0,0,0,0,1.75h7a.875.875,0,0,0,0-1.75Z"
    />
  </svg>,
];

const RegisterPage = () => {
  const [isSubmit, setIsSumbit] = useState(false);
  const navigate = useNavigate();

  const onFinish = async (values) => {
    const { fullName, email, password } = values;
    setIsSumbit(true);
    const result = await registerAPI(fullName, email, password, 2);
    setIsSumbit(false)
    if (result.data.id) {
      message.success('Đăng ký thành công');
      navigate("/login")
    } else {
      notification.error({
        message: "Something went wrongs",
        description: result.message && Array.isArray(result.message) ? result.message : "",
        duration: 3
      })
    }
  };

  return (
    <>
      <div className="layout-default ant-layout layout-sign-up">
        <Header>
          <div className="header-col header-brand">
            <h5>AniHoyo VietSub</h5>
          </div>
          <div className="header-col header-nav">
            <Menu mode="horizontal" defaultSelectedKeys={["1"]}>
              <Menu.Item key="1">
                <Link to="/">
                  {template}
                  <span> Trang chủ</span>
                </Link>
              </Menu.Item>
              <Menu.Item key="4">
                <Link to="/login">
                  {signin}
                  <span> Đăng nhập ngay</span>
                </Link>
              </Menu.Item>
            </Menu>
          </div>
          <div className="header-col header-btn">
            <Button type="false">Hello Summer 2025</Button>
          </div>
        </Header>

        <Content className="p-0">
          <div className="sign-up-header">
            <div className="content">
              <Title>Đăng ký tài khoản</Title>
              <p className="text-lg">
                Đăng ký tài khoản để có trải nghiệm xem với nhiều dịch vụ hỗ trợ tốt nhất (mà xem chùa thì cũng tạm tạm)
              </p>
            </div>
          </div>

          <Card
            className="card-signup header-solid h-full ant-card pt-0"
            title={<h5>Đăng ký với</h5>}
          >
            <div className="sign-up-gateways">
              <Button type="false">
                <img src={logo1} alt="logo 1" />
              </Button>
              <Button type="false">
                <img src={logo2} alt="logo 2" />
              </Button>
              <Button type="false">
                <img src={logo3} alt="logo 3" />
              </Button>
            </div>
            <p className="text-center my-25 font-semibold text-muted">Or</p>
            <Form
              name="basic"
              initialValues={{ remember: true }}
              onFinish={onFinish}
              className="row-col"
            >
              <Form.Item
                name="fullName"
                rules={[
                  { required: true, message: "Please input your username!" },
                ]}
              >
                <Input placeholder="Nhập tên người dùng" />
              </Form.Item>
              <Form.Item
                name="email"
                rules={[
                  {
                    type: 'email',
                    message: 'The input is not a valid E-mail!',
                  },
                  {
                    required: true,
                    message: 'Please input your E-mail!',
                  },
                ]}
              >
                <Input placeholder="email" />
              </Form.Item>
              <Form.Item
                name="password"
                rules={[
                  { required: true, message: "Please input your password!" },
                ]}
              >
                <Input placeholder="Mật khẩu" />
              </Form.Item>

              <Form.Item name="remember" valuePropName="checked">
                <Checkbox>
                  I agree the{" "}
                  <a href="#pablo" className="font-bold text-dark">
                    Terms and Conditions
                  </a>
                </Checkbox>
              </Form.Item>

              <Form.Item>
                <Button
                  style={{ width: "100%" }}
                  type="primary"
                  htmlType="submit"
                  loading={isSubmit}
                >
                  Đăng ký ngay
                </Button>
              </Form.Item>
            </Form>
            <p className="font-semibold text-muted text-center">
              Đã có tài khoản trước đó?{" "}
              <Link to="/login" className="font-bold text-dark">
                Đăng nhập
              </Link>
            </p>
          </Card>
        </Content>
        <Footer>
          <Menu mode="horizontal">
            <Menu.Item>Về AniHoyo VietSub</Menu.Item>
            <Menu.Item>Soicak media</Menu.Item>
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
      </div>
    </>
  )
}
export default RegisterPage