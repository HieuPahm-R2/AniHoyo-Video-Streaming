import { Button, Col, Form, Input, message, notification, Row } from "antd"
import { useState } from "react"
import { useSelector } from "react-redux"

const ChangePassword = () => {
    const [form] = Form.useForm()

    const user = useSelector(state => state.account.user)

    const onFinish = async (values) => {
        const { email, newpass, oldpass } = values
        setIsSubmit(true)
        const res = await callChangePasswordAPI(email, oldpass, newpass)
        if (res && res.data) {
            form.setFieldValue("oldpass", " ")
            form.setFieldValue("newpass", "")
            message.success("Update well done")
        } else {
            notification.error({
                message: "Đã có lỗi xảy ra",
                description: res.message
            })
        }
        setIsSubmit(false)
    }

    const [isSubmit, setIsSubmit] = useState(false);
    return (
        <div style={{ minHeight: 400 }}>
            <Row>
                <Col span={1}></Col>
                <Col span={12}>
                    <Form
                        name="change-password"
                        onFinish={onFinish}
                        autoComplete="off"
                        form={form}
                    >
                        <Form.Item
                            labelCol={{ span: 24 }} //whole column
                            label="Email"
                            name="email"
                            initialValue={user?.email}
                            rules={[{ required: true, message: 'Email không được để trống!' }]}
                        >
                            <Input disabled />
                        </Form.Item>

                        <Form.Item
                            labelCol={{ span: 24 }} //whole column
                            label="Mật khẩu hiện tại"
                            name="oldpass"
                            rules={[{ required: true, message: 'Mật khẩu không được để trống!' }]}
                        >
                            <Input.Password />
                        </Form.Item>

                        <Form.Item
                            labelCol={{ span: 24 }} //whole column
                            label="Mật khẩu mới"
                            name="newpass"
                            rules={[{ required: true, message: 'Mật khẩu không được để trống!' }]}
                        >
                            <Input.Password />
                        </Form.Item>
                        <Form.Item
                        // wrapperCol={{ offset: 6, span: 16 }}
                        >
                            <Button
                                type="primary"
                                htmlType="submit"
                                loading={isSubmit}
                            >
                                Xác nhận
                            </Button>
                        </Form.Item>

                    </Form>
                </Col>
            </Row>
        </div>

    )
}
export default ChangePassword