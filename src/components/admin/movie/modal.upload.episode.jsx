import { InboxOutlined } from '@ant-design/icons'
import { Input, message, Modal, notification } from 'antd'
import Dragger from 'antd/es/upload/Dragger'
import { useState } from 'react'
import { createEpisodeAPI, uploadVideoAPI } from '@/config/api.handle'
import ModalEpisodeList from './modal.view.episode'

const ModalEpisodeUpload = (props) => {
    const { modalAddEpisode, setModalAddEpisode, selectedSeason } = props
    const [urlVideo, setUrlVideo] = useState(null);
    const [isLoading, setIsLoading] = useState(false);
    const [titleEpisode, setTitleEpisode] = useState("");
    const [contentType, setContentType] = useState("");
    const [dataEpisode, setDataEpisode] = useState([]);


    const handleSubmit = async () => {
        if (!urlVideo) {
            message.error("Vui lòng upload tập phim!");
            return;
        }
        if (!selectedSeason?.id) {
            message.error("Vui lòng chọn mùa phim trước khi thêm tập!");
            return;
        }
        setIsLoading(true);
        try {
            // Backend chỉ lưu episode rồi trả về ngay; FFmpeg chạy ở nền và trạng
            // thái nằm trong `hlsStatus` (PENDING -> PROCESSING -> READY/FAILED).
            const res = await createEpisodeAPI(titleEpisode, urlVideo, contentType, selectedSeason?.id);
            if (res && res.data) {
                notification.success({
                    description: `Đã thêm "${titleEpisode}". Video đang được chuyển mã HLS ở nền, xem được sau ít phút.`,
                    message: "Upload Done!",
                })
                setModalAddEpisode(false)
                setDataEpisode(res)
                window.location.reload();
            } else {
                notification.error({
                    description: res?.message ?? "Không rõ nguyên nhân",
                    message: "Đã có lỗi xảy ra",
                })
                setModalAddEpisode(false)
            }
        } catch (error) {
            // axios.customize resolve lỗi HTTP thành body lỗi, nên chỉ còn lỗi mạng
            // hoặc lỗi phía client rơi vào đây.
            notification.error({
                description: error?.message ?? "Không gửi được yêu cầu lên server",
                message: "Đã có lỗi xảy ra",
            })
        } finally {
            setIsLoading(false)
        }
    }
    const setTitleInput = (value) => {
        setTitleEpisode(value);
    }
    const propsUpload = {
        name: 'file',
        multiple: false,
        maxCount: 1,
        accept: "video/mp4,video/x-matroska,video/webm,video/avi,video/quicktime",
        async customRequest({ file, onSuccess, onError }) {
            const res = await uploadVideoAPI(file, "videos");
            if (res && res.data) {
                setUrlVideo(res.data.fileName);
                if (onSuccess) onSuccess('ok')
            } else {
                if (onError) {
                    setUrlVideo("");
                    const error = new Error(res.message);
                    onError({ event: error });
                }
            }
        },
        onChange(info) {
            const { status } = info.file;
            if (status !== 'uploading') {
                message.loading("Chờ xíu...");
            }
            if (status === 'done') {
                if (info.fileList.length > 0 && info.fileList) {
                    setContentType(info.file.type)
                    message.success(`file uploaded successfully.`);
                }
            } else if (status === 'error') {
                message.error(`file upload failed.`);
            }
        },
        onDrop(e) {
            console.log('Dropped files', e.dataTransfer.files);
        },
    };
    return (
        <Modal
            title="Thêm tập phim"
            centered
            open={modalAddEpisode}
            loading={isLoading}
            onOk={() => handleSubmit()}
            onCancel={() => setModalAddEpisode(false)}
        >
            <Input
                placeholder="tiêu đề Anime | Số tập"
                variant="underlined" style={{ width: 300 }}
                autoFocus={true}
                name={'title'}
                onChange={(e) => setTitleInput(e.target.value)}
            />
            <div style={{ marginTop: "20px", marginBottom: "10px" }}></div>
            <Dragger {...propsUpload}>
                <p className="ant-upload-drag-icon">
                    <InboxOutlined />
                </p>
                <p className="ant-upload-text">Nhấp hoặc kéo tệp vào khu vực này để tải lên</p>
                <p className="ant-upload-hint">
                    Chỉ hỗ trợ tải lên một lần. Nghiêm cấm tải lên dữ liệu, tài liệu không phù hợp hoặc các thông tin khác
                    tập tin bị cấm.
                </p>
            </Dragger>
            <ModalEpisodeList dataEpisode={dataEpisode} />
        </Modal>

    )
}

export default ModalEpisodeUpload