import { Modal, Tabs } from "antd"
import UserInfo from "./user.info"
import ChangePassword from "./user.change.password"

const AccountMange = (props) => {
    const { isModalOpen, setIsModalOpen } = props
    const items = [
        {
            key: 'info',
            label: `Xem & Cập nhật thông tin`,
            children: <UserInfo />
        },
        {
            key: 'password',
            label: `Đổi mật khẩu`,
            children: <ChangePassword />,
        },]
    return (
        <Modal
            title="Quản lý tài khoản"
            open={isModalOpen}
            footer={null}
            onCancel={() => setIsModalOpen(false)}
            maskClosable={false}
            width={"60vw"}
        >
            <Tabs
                defaultActiveKey="info"
                items={items}
            />
        </Modal>
    )
}
export default AccountMange