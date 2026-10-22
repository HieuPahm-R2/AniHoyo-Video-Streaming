import React, { useState } from 'react'
import { Col, Divider, message, Form, Input, InputNumber, Modal, notification, Row, Select, Upload } from "antd";
import { LoadingOutlined, PlusOutlined } from "@ant-design/icons";
import { callCreateFilmAPI, callUploadImage } from '@/config/api.handle';
import { useFilmCatalog } from '@/config/catalog';
import { v4 as uuidv4 } from 'uuid';
const ModalCreate = (props) => {
    const { openModalCreate, setOpenModalCreate, refetchData } = props;

    const [form] = Form.useForm();

    const [isLoading, setIsLoading] = useState(false);
    const [isSubmit, setIsSubmit] = useState(false);
    const [isSliderLoading, setIsSliderLoading] = useState(false);

    const [previewOpen, setPreviewOpen] = useState(false);
    const [previewImage, setPreviewImage] = useState("");
    const [previewTitle, setPreviewTitle] = useState("");

    const [dataThumbnail, setDataThumbnail] = useState([]);
    const [dataSlider, setDataSlider] = useState([]);
    // Thể loại + Highlight Tag là dữ liệu DB (FK theo id) nên vẫn lấy từ API,
    // nhưng chỉ fetch 1 lần cho cả app thay vì mỗi lần mở modal.
    const { categories: listCategories, tags: listTags } = useFilmCatalog();

    const onFinish = async (values) => {
        if (dataThumbnail.length === 0) {
            notification.error({
                message: 'Lỗi validate',
                description: 'Vui lòng upload ảnh thumbnail'
            })
            return;
        }
        if (dataSlider.length === 0) {
            notification.error({
                message: 'Lỗi validate',
                description: 'Vui lòng upload ảnh slider'
            })
            return;
        }
        const { name, studio, tags, categories } = values;
        const thumbnail = dataThumbnail[0].name;
        const slider = dataSlider[0].name;
        setIsSubmit(true);
        const res = await callCreateFilmAPI(thumbnail, slider, name, studio, tags, categories);
        if (res && res.data) {
            notification.success({
                message: 'Thành công',
                description: 'Tạo phim thành công'
            })
            setOpenModalCreate(false);
            form.resetFields();
            setDataSlider([]);
            setDataThumbnail([]);
            await refetchData();
        } else {
            notification.error({
                message: 'Lỗi',
                description: 'Tạo phim thất bại'
            })
        }
        setIsSubmit(false);
    }
    // handle upload file
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
    const handleUploadSlider = async ({ file, onSuccess, onError }) => {
        const res = await callUploadImage(file, "slider");
        if (res && res.data) {
            setDataSlider([{
                name: res.data.fileName,
                uid: uuidv4()
            }]);
            onSuccess("Tải ảnh thành công!");
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
        if (type === 'slider') {
            setDataSlider([]);
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
        if (info.file.status === 'uploading') {
            type ? setIsSliderLoading(true) : setIsLoading(true);
            return;
        }
        if (info.file.status === 'done') {
            // Get this url from response in real world.
            getBase64(info.file.originFileObj, (url) => {
                type ? setIsSliderLoading(false) : setIsLoading(false);
                setImageUrl(url);
            });
        }
    };
    return (
        <>
            <Modal
                title="Thêm Phim mới"
                open={openModalCreate}
                onOk={() => { form.submit() }}
                okText={"Tạo mới"}
                onCancel={() => {
                    form.resetFields();
                    setOpenModalCreate(false);
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
                        <Col span={12}>
                            <Form.Item
                                labelCol={{ span: 24 }}
                                label="Tên Phim"
                                name="name"
                                rules={[{ required: true, message: 'Vui lòng nhập tên hiển thị!' }]}
                            >
                                <Input />
                            </Form.Item>
                        </Col>
                        <Col span={12}>
                            <Form.Item
                                labelCol={{ span: 24 }}
                                style={{ borderRadius: "5px" }}
                                label="Studio Production"
                                name="studio"
                                rules={[{ required: true, message: 'Vui lòng nhập tên xưởng làm phim!' }]}
                            >
                                <Input />
                            </Form.Item>
                        </Col>
                        <Col span={6}>
                            <Form.Item
                                labelCol={{ span: 24 }}
                                label="Thể loại"
                                name="categories"
                                rules={[{ required: true, message: 'Vui lòng chọn thể loại!' }]}
                            >
                                <Select
                                    mode="multiple"
                                    defaultValue={null}
                                    showSearch
                                    allowClear
                                    options={listCategories}
                                />
                            </Form.Item>
                        </Col>
                        <Col span={6}>
                            <Form.Item
                                labelCol={{ span: 24 }}
                                label="Highlight Tag"
                                name="tags"
                                rules={[{ required: true, message: 'Vui lòng chọn tag phim!' }]}
                            >
                                <Select
                                    mode="multiple"
                                    defaultValue={null}
                                    showSearch
                                    allowClear
                                    options={listTags}
                                />
                            </Form.Item>
                        </Col>
                        <Col span={6}>
                            <Form.Item
                                labelCol={{ span: 24 }}
                                label="Ảnh Thumbnail"
                                name="thumbnail"
                            >
                                <Upload
                                    name="thumbnail"
                                    listType="picture-card"
                                    className="avatar-uploader"
                                    maxCount={1}
                                    multiple={false}
                                    customRequest={handleUploadThumbnail}
                                    beforeUpload={beforeUpload}
                                    onRemove={file => handleRemoveFile(file, 'thumbnail')}
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
                                label="Ảnh Slider"
                                name="slider"
                            >
                                <Upload
                                    name="slider"
                                    listType="picture-card"
                                    className="avatar-uploader"
                                    maxCount={1}
                                    multiple={false}
                                    beforeUpload={beforeUpload}
                                    onChange={info => handleChange(info, 'slider')}
                                    onRemove={file => handleRemoveFile(file, 'slider')}
                                    onPreview={handlePreview}
                                    customRequest={handleUploadSlider}
                                    showUploadList={{
                                        showPreviewIcon: true,
                                        showRemoveIcon: true,
                                    }}
                                >
                                    <div>
                                        <PlusOutlined />
                                        <div style={{ marginTop: 8 }}>Upload</div>
                                    </div>
                                </Upload>
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

export default ModalCreate