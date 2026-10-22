package com.HieuPahm.AniHoyo.services.implement;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.HieuPahm.AniHoyo.model.entities.Notification;
import com.HieuPahm.AniHoyo.repository.NotificationRepository;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(NotificationRepository notificationRepository, SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public void sendNotification(Long receiverId, Long senderId, String type, Long refId, String message) {
        Notification noti = new Notification();
        noti.setReceiverId(receiverId);
        noti.setSenderId(senderId);
        noti.setType(type);
        noti.setReferId(refId);
        noti.setMessage(message);
        notificationRepository.save(noti);

        messagingTemplate.convertAndSend("/topic/notifications/" + receiverId, noti);
    }
}
