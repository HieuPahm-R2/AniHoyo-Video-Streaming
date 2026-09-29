import { Alert, Button, Drawer, message, List, Avatar, Space, Tag, Tooltip } from 'antd'
import React, { useEffect, useState } from 'react'

import { fetchAllEpisodeBySeason, processEpisodeAPI } from '@/config/api.handle';
import { MediaPlayer } from '@vidstack/react';

import '@vidstack/react/player/styles/default/theme.css';
import '@vidstack/react/player/styles/default/layouts/audio.css';
import '@vidstack/react/player/styles/default/layouts/video.css';

import { MediaProvider } from "@vidstack/react"
import { DefaultVideoLayout, defaultLayoutIcons } from '@vidstack/react/player/layouts/default';

const ModalEpisodeList = (props) => {
    const { modalListEpisode, setModalListEpisode, dataSeason, selectedSeason, onSeasonSelect } = props

    const [videoId, setVideoId] = useState("");
    const [currentEpisode, setCurrentEpisode] = useState(0);
    const [episodeList, setEpisodeList] = useState([]);
    const [isLoading, setIsLoading] = useState(false);

    // Save modal state to localStorage
    const saveModalState = (isOpen, seasonId, episodeIndex) => {
        if (isOpen) {
            localStorage.setItem('modalEpisodeList', JSON.stringify({
                isOpen: true,
                seasonId: seasonId?.id,
                episodeIndex: episodeIndex,
                timestamp: Date.now()
            }));
        } else {
            localStorage.removeItem('modalEpisodeList');
        }
    };

    // Restore modal state from localStorage
    const restoreModalState = () => {
        const savedState = localStorage.getItem('modalEpisodeList');
        if (savedState) {
            try {
                const state = JSON.parse(savedState);
                // Check if saved state is not too old (within 1 hour)
                const isRecent = Date.now() - state.timestamp < 3600000;

                if (state.isOpen && state.seasonId && isRecent) {
                    // Find the season and restore state
                    if (dataSeason && dataSeason.length > 0) {
                        const season = dataSeason.find(s => s.id === state.seasonId);
                        if (season) {
                            console.log('Restoring modal state for season:', season.id);
                            setModalListEpisode(true);
                            // Force update selectedSeason if different
                            if (season.id !== selectedSeason?.id) {
                                onSeasonSelect?.(season);
                            }
                            // Set episode index
                            if (state.episodeIndex !== undefined) {
                                setCurrentEpisode(state.episodeIndex);
                            }
                        }
                    }
                }
            } catch (error) {
                console.error('Error restoring modal state:', error);
                localStorage.removeItem('modalEpisodeList');
            }
        }
    };

    // Restore modal state on mount and when dataSeason changes
    useEffect(() => {
        if (dataSeason && dataSeason.length > 0) {
            restoreModalState();
        }
    }, [dataSeason]);

    // Save modal state when it changes
    useEffect(() => {
        if (modalListEpisode) {
            saveModalState(modalListEpisode, selectedSeason, currentEpisode);
        }
    }, [modalListEpisode, selectedSeason, currentEpisode]);

    // Fetch episodes when selectedSeason changes
    useEffect(() => {
        if (selectedSeason) {
            fetchEpisodes();
        }
    }, [selectedSeason]);

    const fetchEpisodes = async () => {
        if (selectedSeason?.id) {

            try {
                const res = await fetchAllEpisodeBySeason(selectedSeason.id)
                if (res && res.data) {
                    const accept = res.data.result.map((item) => ({
                        videoId: item.id,
                        title: item.title,
                        // PENDING/PROCESSING/READY/FAILED — do backend chuyển mã HLS ở nền.
                        hlsStatus: item.hlsStatus ?? null,
                        hlsError: item.hlsError ?? null,
                    }))
                    setEpisodeList(accept)
                    console.log('Episode list set:', accept)
                    if (accept.length > 0) {
                        // Restore episode index from localStorage if available
                        const savedState = localStorage.getItem('modalEpisodeList');
                        let targetEpisodeIndex = 0;

                        if (savedState) {
                            try {
                                const state = JSON.parse(savedState);
                                if (state.episodeIndex !== undefined && state.episodeIndex < accept.length) {
                                    targetEpisodeIndex = state.episodeIndex;
                                }
                            } catch (error) {
                                console.error('Error parsing saved state:', error);
                            }
                        }

                        setCurrentEpisode(targetEpisodeIndex);
                        setVideoId(accept[targetEpisodeIndex].videoId);
                        console.log(`Set initial episode: ${targetEpisodeIndex}, videoId: ${accept[targetEpisodeIndex].videoId}`);
                    }
                }
            } catch (error) {
                console.error('Error fetching episodes:', error);
                message.error('Lỗi khi tải danh sách tập phim');
            }
        } else {
            console.log('No selectedSeason or selectedSeason.id is missing');
        }
    }

    // Handle episode change
    const handleEpisodeChange = (episodeIndex) => {
        const episode = episodeList[episodeIndex];
        if (episode) {
            console.log(`Changing to episode: ${episode.title} (ID: ${episode.videoId})`);
            setCurrentEpisode(episodeIndex);
            setVideoId(episode.videoId);
            message.success(`Đang chuyển sang ${episode.title}`);
        }
    };

    const onCloseChild = () => {
        // Clear localStorage when closing
        localStorage.removeItem('modalEpisodeList');
        setModalListEpisode(false);
    }

    const currentEp = episodeList[currentEpisode];
    const isProcessing = currentEp?.hlsStatus === 'PENDING' || currentEp?.hlsStatus === 'PROCESSING';
    const isFailed = currentEp?.hlsStatus === 'FAILED';

    // Gửi lại yêu cầu chuyển mã cho tập đang chọn: POST /api/v1/process-episode/{id}
    const handleReprocess = async () => {
        if (!currentEp) return;
        setIsLoading(true);
        try {
            await processEpisodeAPI(currentEp.videoId);
            message.success('Đã gửi yêu cầu chuyển mã lại, đợi vài phút rồi mở lại danh sách tập.');
        } finally {
            setIsLoading(false);
            fetchEpisodes();
        }
    }

    const statusTag = (episode) => {
        switch (episode?.hlsStatus) {
            case 'READY':
                return <Tag color="green">Sẵn sàng</Tag>;
            case 'PROCESSING':
            case 'PENDING':
                return <Tag color="blue">Đang xử lý</Tag>;
            case 'FAILED':
                return (
                    <Tooltip title={episode.hlsError || 'Không rõ nguyên nhân'}>
                        <Tag color="red">Lỗi xử lý</Tag>
                    </Tooltip>
                );
            default:
                return null;
        }
    }

    return (
        <Drawer
            title={
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <span>Danh sách tập phim:</span>
                </div>
            }
            width={"80vw"}
            placement={'left'}
            closable={true}
            onClose={onCloseChild}
            open={modalListEpisode}
            bodyStyle={{ padding: '16px' }}
            destroyOnClose={true}
        >
            <div style={{ display: 'flex', gap: '20px', height: '100%', alignItems: 'flex-start' }}>
                {/* Video Player Section */}
                <div style={{ flex: '2', height: "400px", }}>

                    {isProcessing && (
                        <Alert
                            type="info"
                            showIcon
                            message="Video đang được chuyển mã HLS"
                            description="Tập này chưa phát được. Đợi vài phút rồi mở lại danh sách tập."
                        />
                    )}

                    {isFailed && (
                        <Alert
                            type="error"
                            showIcon
                            message="Chuyển mã video thất bại"
                            description={currentEp?.hlsError || 'Không rõ nguyên nhân.'}
                            action={<Button size="small" danger onClick={handleReprocess}>Xử lý lại</Button>}
                        />
                    )}

                    {!isProcessing && !isFailed && videoId && (
                        <MediaPlayer
                            viewType='video'
                            streamType='on-demand'
                            src={`${import.meta.env.VITE_BACKEND_URL ?? 'http://localhost:8083'}/api/v1/${videoId}/master.m3u8`}
                            onLoadStart={() => setIsLoading(true)}
                            onCanPlay={() => setIsLoading(false)}
                            onError={(e) => {
                                message.error('Lỗi phát video');
                                setIsLoading(false);
                            }}

                            key={videoId} // Force re-render when episode changes
                        >
                            <MediaProvider />

                            <DefaultVideoLayout
                                thumbnails='https://files.vidstack.io/sprite-fight/thumbnails.vtt'
                                icons={defaultLayoutIcons}
                            />

                        </MediaPlayer>
                    )}

                </div>

                {/* Episode List Section */}
                <div style={{ flex: '1', minHeight: '500px' }}>
                    <h3 style={{
                        marginBottom: '16px',
                        color: '#1890ff',
                        padding: '12px',
                        backgroundColor: '#f0f8ff',
                        borderRadius: '8px',
                        border: '1px solid #d6e4ff'
                    }}>
                        Danh sách tập ({episodeList.length})
                    </h3>
                    <List
                        itemLayout="horizontal"
                        dataSource={episodeList}
                        style={{
                            backgroundColor: '#fff',
                            borderRadius: '8px',
                            padding: '8px',
                            border: '1px solid #f0f0f0',
                            boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
                        }}
                        renderItem={(episodeList, index) => (
                            <List.Item
                                style={{
                                    cursor: 'pointer',
                                    backgroundColor: currentEpisode === index ? '#f0f8ff' : 'transparent',
                                    borderRadius: '8px',
                                    padding: '12px',
                                    marginBottom: '8px',
                                    border: currentEpisode === index ? '2px solid #1890ff' : '1px solid #f0f0f0',
                                    transition: 'all 0.3s ease',
                                    boxShadow: currentEpisode === index ? '0 2px 8px rgba(24,144,255,0.2)' : 'none'
                                }}
                                onClick={() => handleEpisodeChange(index)}
                            >
                                <List.Item.Meta
                                    avatar={
                                        <Avatar
                                            size={60}
                                            style={{
                                                borderRadius: '8px',
                                                backgroundColor: currentEpisode === index ? '#1890ff' : '#f0f0f0',
                                                color: currentEpisode === index ? '#fff' : '#666',
                                                fontSize: '16px',
                                                fontWeight: 'bold'
                                            }}
                                        >
                                            {index + 1}
                                        </Avatar>
                                    }
                                    title={
                                        <div style={{
                                            display: 'flex',
                                            justifyContent: 'space-between',
                                            alignItems: 'center'
                                        }}>
                                            <span style={{
                                                fontWeight: currentEpisode === index ? 'bold' : 'normal',
                                                color: currentEpisode === index ? '#1890ff' : '#000',
                                                fontSize: '14px'
                                            }}>
                                                {episodeList.title}
                                            </span>
                                            <Space size={4}>
                                                {statusTag(episodeList)}
                                                {currentEpisode === index && (
                                                    <span style={{
                                                        color: '#52c41a',
                                                        fontSize: '12px',
                                                        backgroundColor: '#f6ffed',
                                                        padding: '4px 8px',
                                                        borderRadius: '12px',
                                                        border: '1px solid #b7eb8f'
                                                    }}>
                                                        Đang phát
                                                    </span>
                                                )}
                                            </Space>
                                        </div>
                                    }
                                    description={
                                        <div style={{
                                            fontSize: '12px',
                                            color: '#666',
                                            marginTop: '4px',
                                            padding: '4px 8px',
                                            backgroundColor: '#fafafa',
                                            borderRadius: '4px',
                                            border: '1px solid #f0f0f0'
                                        }}>
                                            <div>Thời lượng: ~24 phút</div>
                                        </div>
                                    }
                                />
                            </List.Item>
                        )}
                    />
                </div>
            </div>
        </Drawer>
    )
}

export default ModalEpisodeList
