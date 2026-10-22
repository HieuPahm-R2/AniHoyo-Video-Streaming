import React, { useEffect, useState } from 'react'
import { AddSeasonAPI, callUploadImage } from '@/config/api.handle';
import { Col, Divider, Form, Input, InputNumber, Modal, notification, Row, Select, Upload } from 'antd';
import { LoadingOutlined, PlusOutlined } from '@ant-design/icons';
import { SEASON_STATUS_OPTIONS, SEASON_TYPE_OPTIONS } from '@/config/constant.status';
import { v4 as uuidv4 } from 'uuid';
const ModalCreateSeason = (props) => {
    const { modalAddSeason, setModalAddSeason, refetchData, dataDetail } = props
    const [isSubmit, setIsSubmit] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [dataThumbnail, setDataThumbnail] = useState([]);
    const [form] = Form.useForm();

    const onFinish = async (values) => {
        if (dataThumbnail.length === 0) {
            notification.error({
                message: 'Lỗi validate',
                description: 'Vui lòng upload ảnh thumbnail'
            })
            return;
        }
        const { name, description, type, releaseYear, status, trailer, ordinal } = values;
        const thumb = dataThumbnail[0].name;
        setIsSubmit(true);
        const res = await AddSeasonAPI(thumb, name, description, type, releaseYear, status, trailer, ordinal, dataDetail?.id);
        if (res && res.data) {
            notification.success({
                message: 'Thành công',
                description: 'Tạo season thành công'
            })
            setModalAddSeason(false);
            form.resetFields();
            setDataThumbnail([]);
            await refetchData();
        } else {
            notification.error({
                message: 'Lỗi',
                description: 'Tạo season thất bại'
            })
        }
        setIsSubmit(false);
    }
    // handle upload file
    const handleUploadThumbnail = async ({ file, onSuccess, onError }) => {
        console.log("handleUploadThumbnail called");
        const res_thumb = await callUploadImage(file, "visual");
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
        if (info.file.status === 'uploading') {
            setIsLoading(true);
            return;
        }
        if (info.file.status === 'done') {
            // Get this url from response in real world.
            getBase64(info.file.originFileObj, (url) => {
                setIsLoading(false);
                setImageUrl(url);
            });
        }
    };

    return (
        <Modal
            title="Thêm Season/Movie/OVA"
            open={modalAddSeason}
            onOk={() => { form.submit() }}
            okText={"Tạo mới"}
            onCancel={() => {
                form.resetFields();
                setModalAddSeason(false);
            }}
            cancelText={"Hủy"}
            confirmLoading={isSubmit}
            width={"50vw"}
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
                            label="Tên Season"
                            name="name"
                            rules={[{ required: true, message: 'Vui lòng nhập tên hiển thị!' }]}
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
                            label="Ảnh Thumbnail"
                            name="thumbnail"
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
    )
}

export default ModalCreateSeason