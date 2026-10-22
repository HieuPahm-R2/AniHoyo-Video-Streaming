import { Col, Divider, Form, Input, InputNumber, Modal, notification, Row, Select, Upload } from "antd";
import { useEffect, useState } from "react";
import { LoadingOutlined, PlusOutlined } from "@ant-design/icons";
import { callUploadImage, UpdateSeasonAPI } from '@/config/api.handle';
import { SEASON_STATUS_OPTIONS, SEASON_TYPE_OPTIONS } from '@/config/constant.status';
import { v4 as uuidv4 } from 'uuid';

const ModalUpdateSeason = (props) => {
    const { modalUpdateSeason, setModalUpdateSeason, refetchData, selectedSeason, setSelectedSeason } = props;
    const [form] = Form.useForm();
    const [isLoading, setIsLoading] = useState(false);
    const [previewOpen, setPreviewOpen] = useState(false);
    const [previewImage, setPreviewImage] = useState('');
    const [previewTitle, setPreviewTitle] = useState('');
    const [imageUrl, setImageUrl] = useState("");

    const [dataThumbnail, setDataThumbnail] = useState([]);

    const [initForm, setInitForm] = useState(null);

    const [isSubmit, setIsSubmit] = useState(false);


    useEffect(() => {
        if (selectedSeason?.id) {
            const arrThumbnail = [{
                uid: uuidv4(),
                name: selectedSeason.thumb,
                status: 'done',
                url: `${import.meta.env.VITE_BACKEND_URL}/storage/visual/${selectedSeason.thumb}`,
            }]

            const initialVal = {
                id: selectedSeason.id,
                seasonName: selectedSeason.seasonName,
                description: selectedSeason.description,
                type: selectedSeason.type,
                releaseYear: selectedSeason.releaseYear,
                status: selectedSeason.status,
                trailer: selectedSeason.trailer,
                ordinal: selectedSeason.ordinal,
                thumb: {
                    fileList: arrThumbnail
                },
            }
            setInitForm(initialVal);
            setDataThumbnail(arrThumbnail);
            form.setFieldsValue(initialVal);
        }
        return () => { form.resetFields() };

    }, [selectedSeason]);

    const onFinish = async (values) => {
        if (dataThumbnail.length === 0) {
            notification.error({
                message: 'Lỗi validate',
                description: 'Vui lòng upload ảnh thumbnail'
            })
            return;
        }
        setIsSubmit(true);
        const thumb = dataThumbnail[0].name;
        const { id, seasonName, description, type, releaseYear, status, trailer, ordinal } = values;
        console.log(values);
        const res = await UpdateSeasonAPI(
            thumb, seasonName, description, type, releaseYear, status, trailer, ordinal, id);
        if (res && res.data) {
            setModalUpdateSeason(false);
            setInitForm(null);
            setDataThumbnail([]);
            form.resetFields();
            await refetchData();
            notification.success({
                message: 'Cập nhật thành công',
                description: 'Cập nhật thành công'
            })

        } else {
            notification.error({
                message: 'Cập nhật thất bại',
                description: res.message
            })
        }
        setIsSubmit(false);
    }
    const handleUploadThumbnail = async ({ file, onSuccess, onError }) => {
        console.log("handleUploadThumbnail called");
        const res_thumb = await callUploadImage(file, "thumbnail");
        console.log(res_thumb.data)
        if (res_thumb && res_thumb.data) {
            setDataThumbnail([{
                name: res_thumb.data.fileName,
                uid: uuidv4()
            }]);
            onSuccess("Tải ảnh thành công!");
            message.success("done")
        } else {
            onError("Lỗi tải ảnh!");
        }
    };

    const getBase64 = (img, callback) => {
        const reader = new FileReader();
        reader.addEventListener('load', () => callback(reader.result));
        reader.readAsDataURL(img);
    };

    const handlePreview = async (file) => {
        getBase64(file.originFileObj, (url) => {
            setPreviewImage(url);
            setPreviewOpen(true);
            setPreviewTitle(file.name || file.url.substring(file.url.lastIndexOf('/') + 1));
        });
    };
    const handleRemoveFile = (file, type) => {
        if (type === 'thumbnail') {
            setDataThumbnail([])
        }
    }
    const beforeUpload = (file) => {
        const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/webp';
        if (!isJpgOrPng) {
            message.error('You can only upload JPG/PNG or webp file!');
        }
        const isLt3M = file.size / 1024 / 1024 < 3;
        if (!isLt3M) {
            message.error('Image must smaller than 3MB!');
        }
        return isJpgOrPng && isLt3M;
    };
    const handleChange = async (info, type) => {
        if (info.file.status === 'uploading' && type) {
            setIsLoading(true);
            return;
        }
        if (info.file.status === 'done' && type) {
            // Get this url from response in real world.
            getBase64(info.file.originFileObj, (url) => {
                setIsLoading(false);
                setImageUrl(url);
            });
        }
    };
    return (
        <>
            <Modal
                title="Chỉnh sửa thông tin phim"
                open={modalUpdateSeason}
                onOk={() => { form.submit() }}
                okText={"Cập Nhật"}
                onCancel={() => {
                    form.resetFields();
                    setInitForm(null)
                    setModalUpdateSeason(false);
                    setSelectedSeason(null);
                }}
                cancelText={"Hủy"}
                confirmLoading={isSubmit}
                width={"50vw"}
                //do not close when click fetchBook
                maskClosable={false}
            >
                <Divider />

                <Form
                    form={form}
                    name="basic"
                    onFinish={onFinish}
                    autoComplete="off"
                >
                    <Row gutter={15}>
                        <Col hidden>
                            <Form.Item
                                hidden
                                labelCol={{ span: 24 }}
                                label="ID season"
                                name="id"
                            >
                                <Input />
                            </Form.Item>
                        </Col>
                        <Col span={12}>
                            <Form.Item
                                labelCol={{ span: 24 }}
                                label="Tên season"
                                name="seasonName"
                                rules={[{ required: true, message: 'Vui lòng nhập tên !' }]}
                            >
                                <Input />
                            </Form.Item>
                        </Col>
                        <Col span={12}>
                            <Form.Item
                                labelCol={{ span: 24 }}
                                label="Mô tả / Giới Thiệu"
                                name="description"
                                rules={[{ required: true, message: 'Không giới thiệu phim à??!' }]}
                            >
                                <Input.TextArea rows={3} />
                            </Form.Item>
                        </Col>

                        <Col span={6}>
                            <Form.Item
                                labelCol={{ span: 24 }}
                                label="Highlight"
                                name="type"
                                rules={[{ required: true, message: 'Vui lòng chọn dạng phim!' }]}
                            >
                                <Select
                                    defaultValue={null}
                                    showSearch
                                    allowClear
                                    options={SEASON_TYPE_OPTIONS}
                                />
                            </Form.Item>
                        </Col>
                        <Col span={6}>
                            <Form.Item
                                labelCol={{ span: 24 }}
                                label="Status"
                                name="status"
                                rules={[{ required: true, message: 'Vui lòng chọn dạng phim!' }]}
                            >
                                <Select
                                    defaultValue={null}
                                    showSearch
                                    allowClear
                                    options={SEASON_STATUS_OPTIONS}
                                />
                            </Form.Item>
                        </Col>
                        <Col span={6}>
                            <Form.Item
                                labelCol={{ span: 24 }}
                                label="Key Visual"
                                name="thumb"
                            >
                                <Upload
                                    name="thumb"
                                    listType="picture-card"
                                    className="avatar-uploader"
                                    maxCount={1}
                                    multiple={false}
                                    customRequest={handleUploadThumbnail}
                                    beforeUpload={beforeUpload}
                                    onRemove={file => handleRemoveFile(file, 'thumbnail')}
                                    defaultFileList={initForm?.thumb?.fileList ?? []}
                                    onPreview={handlePreview}
                                    showUploadList={{
                                        showPreviewIcon: true,
                                        showRemoveIcon: true,
                                    }}
                                >
                                    <div>
                                        {isLoading ? <LoadingOutlined /> : <PlusOutlined />}
                                        <div style={{ marginTop: 8 }}>Upload</div>
                                    </div>
                                </Upload>
                            </Form.Item>

                        </Col>

                        <Col span={6}>
                            <Form.Item
                                labelCol={{ span: 24 }}
                                label="Năm Phát Hành"
                                name="releaseYear"
                                rules={[{ required: true, message: 'Vui lòng nhập thông tin!' }]}
                            >
                                <InputNumber min={1} style={{ width: '100%' }} />
                            </Form.Item>
                        </Col>
                        <Col span={12}>
                            <Form.Item
                                labelCol={{ span: 24 }}
                                label="Trailer/Teaser Link"
                                name="trailer"
                                rules={[{ required: true, message: 'Vui lòng nhập thông tin!' }]}
                            >
                                <Input />
                            </Form.Item>
                        </Col>
                        <Col span={6}>
                            <Form.Item
                                labelCol={{ span: 24 }}
                                label="Tên ngắn gọn (STT mùa or movie)"
                                name="ordinal"
                                rules={[{ required: true, message: 'Vui lòng nhập thông tin!' }]}
                            >
                                <Input />
                            </Form.Item>
                        </Col>

                    </Row>
                </Form>
            </Modal>
            <Modal open={previewOpen} title={previewTitle} footer={null} onCancel={() => setPreviewOpen(false)}>
                <img alt="example" style={{ width: '100%' }} src={previewImage} />
            </Modal>

        </>
    )
}

export default ModalUpdateSeason