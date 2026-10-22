import { ModalForm, ProFormText } from "@ant-design/pro-components";
import { Col, Form, Row, message, notification } from "antd";
import { isMobile } from 'react-device-detect';
import { callCreateTag, callUpdateTag } from "@/config/api";
import { invalidateFilmCatalog } from "@/config/catalog";
import { ITag } from "@/types/backend";

interface IProps {
    openModal: boolean;
    setOpenModal: (v: boolean) => void;
    dataInit?: ITag | null;
    setDataInit: (v: any) => void;
    reloadTable: () => void;
}

const ModalTag = (props: IProps) => {
    const { openModal, setOpenModal, reloadTable, dataInit, setDataInit } = props;
    const [form] = Form.useForm();

    const submitTag = async (valuesForm: any) => {
        const { name } = valuesForm;
        const res = dataInit?.id
            ? await callUpdateTag({ id: dataInit.id, name })
            : await callCreateTag({ name });

        if (res.data) {
            message.success(dataInit?.id ? "Cập nhật tag thành công" : "Thêm mới tag thành công");
            // Modal "thêm phim" dùng catalog cache -> xoá cache để lần mở sau lấy list mới
            invalidateFilmCatalog();
            handleReset();
            reloadTable();
        } else {
            notification.error({
                message: 'Có lỗi xảy ra',
                description: (res as any).error ?? (res as any).message
            });
        }
    }

    const handleReset = async () => {
        form.resetFields();
        setDataInit(null);
        setOpenModal(false);
    }

    return (
        <>
            <ModalForm
                title={<>{dataInit?.id ? "Cập nhật Tag" : "Tạo mới Tag"}</>}
                open={openModal}
                modalProps={{
                    onCancel: () => { handleReset() },
                    afterClose: () => handleReset(),
                    destroyOnClose: true,
                    width: isMobile ? "100%" : 500,
                    keyboard: false,
                    maskClosable: false,
                    okText: <>{dataInit?.id ? "Cập nhật" : "Tạo mới"}</>,
                    cancelText: "Hủy"
                }}
                scrollToFirstError={true}
                preserve={false}
                form={form}
                onFinish={submitTag}
                initialValues={dataInit?.id ? dataInit : {}}
            >
                <Row gutter={16}>
                    <Col span={24}>
                        <ProFormText
                            label="Tên Tag"
                            name="name"
                            rules={[
                                { required: true, message: 'Vui lòng không bỏ trống' },
                            ]}
                            placeholder="Nhập tên tag (vd: Vietsub)"
                        />
                    </Col>
                </Row>
            </ModalForm>
        </>
    )
}

export default ModalTag;
