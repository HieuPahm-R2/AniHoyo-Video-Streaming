import { useEffect, useState } from "react";
import { Input, Button } from "antd";

import SockJS from "sockjs-client";
import { over } from "stompjs";
import CommentItem from "./comment.post";
import { fecthCommentsAPI, postCommentAPI, reactCommentAPI, replyCommentAPI } from "@/config/api.handle";

const { TextArea } = Input;

let stompClient = null;

function CommentList({ seasonId, userId, username }) {
    const [comments, setComments] = useState([]);
    const [newContent, setNewContent] = useState("");

    // Load comment từ API
    const loadComments = async () => {
        const res = await fecthCommentsAPI(seasonId);
        setComments(res.data);
    };

    // Kết nối websocket
    useEffect(() => {
        loadComments();

        // Follow VITE_BACKEND_URL so the same build works behind the nginx
        // proxy (empty value => same origin, nginx upgrades /ws).
        const socket = new SockJS(`${import.meta.env.VITE_BACKEND_URL ?? "http://localhost:8083"}/ws`);
        stompClient = over(socket);

        stompClient.connect({}, () => {
            // Nhận comment mới
            stompClient.subscribe(`/topic/comments/${seasonId}`, (message) => {
                const newComment = JSON.parse(message.body);
                if (newComment.parentId) {
                    setComments(prev => {
                        // Logic tìm và cập nhật comment cha
                        const findAndAddReply = (nodes, parentId, reply) => {
                            return nodes.map(node => {
                                if (node.id === parentId) {
                                    return { ...node, replies: [...(node.replies || []), reply] };
                                }
                                if (node.replies) {
                                    return { ...node, replies: findAndAddReply(node.replies, parentId, reply) };
                                }
                                return node;
                            });
                        };
                        return findAndAddReply(prev, newComment.parentId, newComment);
                    });
                } else {
                    setComments(prev => [newComment, ...prev]);
                    console.log(comments)
                }
            });

            // Nhận update like
            stompClient.subscribe(`/topic/comments/like`, (message) => {
                const { commentId, likeCount } = JSON.parse(message.body);
                const updateLikes = (comments) => {
                    return comments.map(c => {
                        if (c.id === commentId) {
                            return { ...c, likeCount };
                        }
                        if (c.replies && c.replies.length > 0) {
                            return { ...c, replies: updateLikes(c.replies) };
                        }
                        return c;
                    });
                };
                setComments(prev => updateLikes(prev));
            });
        });

        return () => {
            if (stompClient) {
                stompClient.disconnect();
            }
        };
    }, [seasonId]);

    // Post comment mới
    const handlePost = () => {
        if (!newContent.trim() || !stompClient) return;
        const payload = {
            content: newContent,
            user: { id: userId },
            season: { id: seasonId },
            parentId: null
        };
        stompClient.send("/app/comment.post", {}, JSON.stringify(payload));
        setNewContent("");
    };


    // Gửi reply qua WebSocket
    const handleReply = (parentId, content) => {
        if (!content.trim() || !stompClient) return;
        const payload = {
            content: content,
            user: { id: userId },
            season: { id: seasonId },
            parentId: parentId
        };
        stompClient.send("/app/comment.post", {}, JSON.stringify(payload));
    };

    // Like comment
    // Gửi sự kiện like qua WebSocket
    const handleLike = (commentId) => {
        if (!stompClient) return;
        const payload = {
            commentId: commentId,
            userId: userId
        };
        stompClient.send("/app/comment.like", {}, JSON.stringify(payload));
    };

    return (
        <div>
            <TextArea
                rows={3}
                value={newContent}
                onChange={(e) => setNewContent(e.target.value)}
                placeholder="Hãy chia sẻ cảm nhận của bạn đi..."
            />
            <Button type="primary" onClick={handlePost} style={{ marginTop: 8 }}>
                Bình luận
            </Button>
            <h3 style={{ color: "white" }}>Các Bình Luận Trước:</h3>

            <div style={{ marginTop: 16 }}>
                {comments.map(c => (
                    <CommentItem
                        key={c.id}
                        comment={c}
                        onReply={handleReply}
                        onLike={handleLike}
                    />
                ))}
            </div>
        </div>
    );
}

export default CommentList;