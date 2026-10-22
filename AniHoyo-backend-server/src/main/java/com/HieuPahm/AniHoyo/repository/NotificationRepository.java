package com.HieuPahm.AniHoyo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.HieuPahm.AniHoyo.model.entities.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByReceiverIdOrderByCreatedAtDesc(Long receiverId);
}