import { Badge, Button, Col, Descriptions, Drawer, Image, message, notification, Popconfirm, Row, Table, Typography } from 'antd'
import moment from 'moment';
import React, { useEffect, useState } from 'react'
import { FORMAT_DATE_DISPLAY } from '@/config/constant.date';
import { CloudUploadOutlined, DeleteTwoTone, EditTwoTone, EyeOutlined, PlusOutlined, ReloadOutlined } from "@ant-design/icons";
import { fetchSeasonsOfFilmAPI } from '@/config/api.handle';
import ModalEpisodeUpload from './modal.upload.episode';
import ModalEpisodeList from './modal.view.episode';
import ModalCreateSeason from './modal.create.season';
import ModalUpdateSeason from './modal.update.season';

const ModalAdjustFilm = (props) => {
    const { openViewDetail, setOpenViewDetail, dataDetail, setDataDetail } = props;
    const [modalAddSeason, setModalAddSeason] = useState(false)
    const [modalAddEpisode, setModalAddEpisode] = useState(false)
    const [modalListEpisode, setModalListEpisode] = useState(false)
    const [modalUpdateSeason, setModalUpdateSeason] = useState(false)

    const [dataSeason, setDataSeason] = useState(null)
    const [selectedSeason, setSelectedSeason] = useState(null)

    const [current, setCurrent] = useState(1);
    const [pageSize, setPageSize] = useState(5);
    const [total, setTotal] = useState(0);
    const [sortQuery, setSortQuery] = useState("");
    const [filter, setFilter] = useState("");

    const { Title } = Typography;


    useEffect(() => {
        if (dataDetail && dataDetail.id) {
            refetchData();
        }
    }, [current, pageSize, sortQuery, filter, dataDetail]);


    const refetchData = async () => {
        if (!dataDetail || !dataDetail.id) return;
        try {
            let queryString = `current=${current}&pageSize=${pageSize}`;
            if (filter) {
                queryString += filter;
            }
            if (sortQuery) {
                queryString += sortQuery;
            }

            const res = await fetchSeasonsOfFilmAPI(dataDetail.id, queryString);

            if (res && res.data && res.data.result) {
                setDataSeason(res.data.result);
                setTotal(res.data.meta?.total || 0);

            } else {
                console.log('No data received from API');
                setDataSeason([]);
                setTotal(0);
            }
        } catch (error) {
            console.error('Error fetching seasons:', error);
            setDataSeason([]);
            setTotal(0);
        }
    }
    const handleDeleteSeason = async (id) => {
        const res = await callDeleteSeasonAPI(id);
        if (res && res.data) {
            message.success('Xóa phim thành công');
            refetchData();
        } else {
            notification.error({
                message: "Xóa phimkhông thành công",
                description: res.data.message
            })
        }
    }

    const onClose = () => {
        setOpenViewDetail(false);
        setDataDetail(null);
        setSelectedSeason(null);
    }

    const columns = [
        {
            title: 'Id Product',
            key: 'id',
            hidden: true
        },
        {
            title: "Season Name",
            dataIndex: "seasonName",
            width: "32%",
            render: (text, record, index) => {
                return (
                    <Image.PreviewGroup>
                        <Image className="shape-avatar" width={50}
                            src={`${import.meta.env.VITE_BACKEND_URL}/storage/visual/${record.thumb}`} alt="" />
                        <div className="avatar-info" style={{ display: "inline-block", marginLeft: "5px", cursor: "pointer" }}>
                            <Title level={5}>{record.seasonName || 'No Name'}</Title>
                        </div>
                    </Image.PreviewGroup>
                )
            }
        },
        {
            title: "Dạng Anime",
            dataIndex: "type",
            key: "type",
            render: (text, record, index) => {
                return (
                    <div className="semibold">{record.type || '---'}</div>
                )
            }
        },
        {
            title: "Release Year",
            dataIndex: "releaseYear",
            key: "releaseYear"
        },
        {
            title: "Upload Date",
            dataIndex: "uploadDate",
            key: "uploadDate",
            sorter: true,
            render: (text, record, index) => {
                return (
                    <>{moment(record.uploadDate).format(FORMAT_DATE_DISPLAY)}</>
                )
            }
        },
        {
            title: "Thêm tập phim",
            ket: "action",
            render: (_, record) => {
                return (
                    <>
                        <Popconfirm
                            placement="leftTop"
                            title={"Xác nhận xóa?"}
                            description={"Bạn chắc chắn xóa mùa Anime này ?"}
                            onConfirm={() => handleDeleteSeason(record.id)}
                            okText="OK"
                            cancelText="Hủy bỏ"
                        >
                            <span style={{ cursor: "pointer", margin: "0 10px" }}>
                                <DeleteTwoTone twoToneColor="#ff4d4f" />
                            </span>
                        </Popconfirm>
                        <EditTwoTone
                            onClick={() => {
                                setModalUpdateSeason(true)
                                setSelectedSeason(record)
                            }}
                        />
                        <CloudUploadOutlined twoToneColor="green" style={{ cursor: "pointer", margin: "0 10px", color: "green" }}
                            onClick={() => {
                                setSelectedSeason(record);
                                setModalAddEpisode(true);
                            }} />
                        <EyeOutlined twoToneColor="yellow" style={{ cursor: "pointer", margin: "0 10px" }}
                            onClick={() => {
                                setOpenViewDetail(false)
                                setModalListEpisode(true)
                                setSelectedSeason(record)
                            }}
                        />
                    </>
                )
            }
        },
    ].filter(item => !item.hidden);


    const renderHeader = () => {
        return (
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: "center" }}>
                <h3 style={{ fontSize: "20px" }}>List Seasons/Movies</h3>
                <span style={{ display: 'flex', gap: 15 }}>
                    <Button
                        icon={<PlusOutlined />}
                        type="primary" onClick={() => {
                            setModalAddSeason(true);
                        }}
                    >Thêm Season/Movie/OVA</Button>
                    <Button type='ghost' >
                        <ReloadOutlined />
                    </Button>
                </span>
            </div>
        )
    }
    return (
        <div>
            <Drawer
                title="Chỉnh sửa thông tin phim"
                width={"70vw"}
                onClose={onClose}
                open={openViewDetail}
            >
                <Descriptions
                    title="Thông tin của Anime"
                    bordered
                    column={2}
                >
                    <Descriptions.Item label="Tên Phim">{dataDetail?.name}</Descriptions.Item>
                    <Descriptions.Item label="Xưởng Phim">{dataDetail?.studio}</Descriptions.Item>
                    <Descriptions.Item label="Thể loại" span={2}>

                        {dataDetail?.categories.map(item => " | " + item.name)}
                    </Descriptions.Item>

                    <Descriptions.Item label="Created At">
                        {moment(dataDetail?.uploadDate).format(FORMAT_DATE_DISPLAY)}
                    </Descriptions.Item>
                    <Descriptions.Item label="Updated At">
                        {moment(dataDetail?.updatedAt).format(FORMAT_DATE_DISPLAY)}
                    </Descriptions.Item>
                </Descriptions>
                <Row gutter={24}>
                    <Col span={24} className="gutter-row">
                        <Table
                            title={renderHeader}
                            columns={columns}
                            rowKey="id"
                            dataSource={dataSeason}
                            pagination={{
                                current: current,
                                pageSize: pageSize,
                                total: total,
                                showTotal: (total, range) => `${range[0]}-${range[1]} of ${total} items`,
                                showSizeChanger: true
                            }} />
                    </Col>
                </Row>

            </Drawer>
            <ModalEpisodeList
                modalListEpisode={modalListEpisode}
                setModalListEpisode={setModalListEpisode}
                dataSeason={dataSeason}
                selectedSeason={selectedSeason}
                onSeasonSelect={setSelectedSeason} />
            <ModalEpisodeUpload modalAddEpisode={modalAddEpisode}
                setModalAddEpisode={setModalAddEpisode}
                dataSeason={dataSeason}
                selectedSeason={selectedSeason} />
            <ModalCreateSeason modalAddSeason={modalAddSeason}
                dataDetail={dataDetail}
                setModalAddSeason={setModalAddSeason} refetchData={refetchData} />
            <ModalUpdateSeason
                modalUpdateSeason={modalUpdateSeason}
                setModalUpdateSeason={setModalUpdateSeason}
                refetchData={refetchData} selectedSeason={selectedSeason} setSelectedSeason={setSelectedSeason} />

        </div>
    )
}

export default ModalAdjustFilm