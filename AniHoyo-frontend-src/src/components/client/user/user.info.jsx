import { callUploadImage } from "@/config/api.handle";
import { AntDesignOutlined, UploadOutlined } from "@ant-design/icons"
import { Avatar, Button, Col, Form, Input, message, notification, Row, Upload } from "antd"
import { useState } from "react";
import { useDispatch, useSelector } from "react-redux";


const UserInfo = () => {
    const [form] = Form.useForm();
    const user = useSelector(state => state.account.user);
    const tempAvatar = 'user33.svg'
    const dispatch = useDispatch()

    const [userAvatar, setUserAvatar] = useState();
    const [isSubmit, setIsSubmit] = useState(false);

    const urlAvatar = `${import.meta.env.VITE_BACKEND_URL}/storage/avatar/${tempAvatar || user?.avatar}`
    const handleUploadAvatar = async ({ file, onSuccess, onError }) => {
        const res = await callUploadImage(file, 'avatar');
        if (res && res.data) {
            const avatarNew = res.data.fileName;
            dispatch(doUploadAvatarAction({ avatar: avatarNew }))
            setUserAvatar(avatarNew)
            onSuccess("well done")
        } else {
            onError('Đã có lỗi khi upload file');
        }
    }
    const propsUpload = {
        maxCount: 1,
        multiple: false,
        showUploadList: false,
        customRequest: handleUploadAvatar,
        onChange(info) {
            if (info.file.status !== 'uploading') {
            }
            if (info.file.status === 'done') {
                message.success(`Upload file thành công`);
            } else if (info.file.status === 'error') {
                message.error(`Upload file thất bại`);
            }
        },
    };

    const onFinish = async (values) => {
        const { fullName, id } = values
        setIsSubmit(true);
        const res = await callUpdateAccountUserAPI(id, email, fullName, userAvatar)
        if (res && res.data) {
            dispatch(doUpdateUserAction({ avatar: userAvatar, fullName }))
            message.success("Cập nhật thông tin user thành công");

            // force renew token to get new data update from redux
            localStorage.removeItem('access_token');
        } else {
            notification.error({
                message: "Đã có lỗi xảy ra",
                description: res.message
            })
        }
        setIsSubmit(false)
    }
    return (
        <div style={{ minHeight: 450 }}>
            <Row gutter={[20, 20]}>
                <Col sm={24} md={12}>
                    <Row gutter={[30, 30]}>
                        <Col span={24}>
                            <Avatar
                                size={{ xs: 32, sm: 64, md: 80, lg: 128, xl: 160, xxl: 200 }}
                                icon={<AntDesignOutlined />}
                                src={urlAvatar}
                                shape="circle"
                            />
                        </Col>
                        <Col span={24}>
                            <Upload {...propsUpload}>
                                <Button icon={<UploadOutlined />}>
                                    Upload Avatar
                                </Button>
                            </Upload>
                        </Col>

                    </Row>
                </Col>
                <Col sm={24} md={12}>
                    <Form
                        onFinish={onFinish}
                        form={form}
                    >
                        <Form.Item
                            hidden labelCol={{ span: 24 }}
                            label="ID"
                            name="id"
                            initialValue={user?.id}
                        >
                            <Input disabled hidden />
                        </Form.Item>

                        <Form.Item
                            labelCol={{ span: 24 }}
                            label="Email"
                            name="email"
                            initialValue={user?.email}>
                            <Input disabled />
                        </Form.Item>
                        <Form.Item
                            labelCol={{ span: 24 }}
                            label="Nickname / Họ Tên"
                            name="fullName"
                            initialValue={user?.fullName}
                            rules={[{ required: true, message: 'Tên hiển thị không được để trống!' }]}
                        >
                            <Input />
                        </Form.Item>
                        <Button loading={isSubmit} onClick={() => form.submit()}>Cập nhật</Button>
                    </Form>

                </Col>
            </Row>
        </div>
    )
}
export default UserInfo