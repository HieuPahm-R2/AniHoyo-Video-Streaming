import { IUser } from "@/types/backend";
import { Badge, Descriptions, Drawer } from "antd";
import dayjs from "dayjs";

interface IProps {
    onClose: (v: boolean) => void;
    open: boolean;
    dataInit: IUser | null;
    setDataInit: (v: any) => void;
}
const ViewDetailUser = (props: IProps) => {
    const { onClose, open, dataInit, setDataInit } = props;

    return (
        <>
            <Drawer
                title="Thông Tin User"
                placement="right"
                onClose={() => {
                    onClose(false);
                    setDataInit(null);
                }}
                open={open}
                width={"40vw"}
                maskClosable={false}
            >
                <Descriptions title="" bordered column={2} layout="vertical">
                    <Descriptions.Item label="Tên hiển thị">
                        {dataInit?.fullName}
                    </Descriptions.Item>
                    <Descriptions.Item label="Email">{dataInit?.email}</Descriptions.Item>

                    <Descriptions.Item label="Vai trò">
                        <Badge status="processing" text={<>{dataInit?.role}</>} />
                    </Descriptions.Item>


                    <Descriptions.Item label="Ngày tạo">
                        {dataInit && dataInit.createdTime
                            ? dayjs(dataInit.createdTime).format("DD-MM-YYYY HH:mm:ss")
                            : ""}
                    </Descriptions.Item>
                    <Descriptions.Item label="Ngày sửa">
                        {dataInit && dataInit.updatedTime
                            ? dayjs(dataInit.updatedTime).format("DD-MM-YYYY HH:mm:ss")
                            : ""}
                    </Descriptions.Item>
                </Descriptions>
            </Drawer>
        </>
    );
};

export default ViewDetailUser;
