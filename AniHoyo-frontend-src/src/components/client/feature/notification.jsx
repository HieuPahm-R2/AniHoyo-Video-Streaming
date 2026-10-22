import { useEffect, useState } from "react";
import { Badge, Dropdown, List } from "antd";
import SockJS from "sockjs-client";
import { over } from "stompjs";
import { getNotification, markAsReadNotifi } from "@/config/api.handle";

let stompClient = null;

export default function NotificationBell({ userId }) {
    const [notifications, setNotifications] = useState([]);

    // Load từ API
    const loadNotifications = async () => {
        const res = await getNotification(userId);
        setNotifications(res.data);
    };

    useEffect(() => {
        loadNotifications();

        // Follow VITE_BACKEND_URL so the same build works behind the nginx
        // proxy (empty value => same origin, nginx upgrades /ws).
        const socket = new SockJS(`${import.meta.env.VITE_BACKEND_URL ?? "http://localhost:8083"}/ws`);
        stompClient = over(socket);

        stompClient.connect({}, () => {
            stompClient.subscribe(`/topic/notifications/${userId}`, (message) => {
                const newNoti = JSON.parse(message.body);
                setNotifications(prev => [newNoti, ...prev]);
            });
        });

        return () => {
            if (stompClient && stompClient.connected) {
                stompClient.disconnect();
            }
        };
    }, [userId]);

    const markAsRead = async (id) => {
        await markAsReadNotifi(id);
        loadNotifications();
    };

    return (
        <Dropdown
            overlay={
                <List style={{ background: "#e6f7ff", padding: "5px" }}
                    dataSource={notifications}
                    renderItem={(n) => (
                        <List.Item
                            style={{ background: n.isRead ? "#fff" : "#e6f7", padding: "10px", borderRadius: "5px", margin: "5px" }}
                            onClick={() => markAsRead(n.id)}
                        >
                            <span>{n.message}</span>
                        </List.Item>
                    )}
                />
            }
            trigger={['click']}
        >
            <Badge count={notifications.filter(n => !n.isRead).length}>
                <span style={{ cursor: "pointer", fontSize: 20, marginLeft: "10px" }}>🔔</span>
            </Badge>
        </Dropdown>
    );
}