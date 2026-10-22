package com.HieuPahm.AniHoyo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.HieuPahm.AniHoyo.model.entities.Notification;
import com.HieuPahm.AniHoyo.repository.NotificationRepository;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationRepository repo;

    public NotificationController(NotificationRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/user/{userId}")
    public List<Notification> getUserNotifications(@PathVariable Long userId) {
        return repo.findByReceiverIdOrderByCreatedAtDesc(userId);
    }

    @PutMapping("/{id}/read")
    public Notification markAsRead(@PathVariable Long id) {
        Notification n = repo.findById(id).orElseThrow();
        n.setIsRead(true);
        return repo.save(n);
    }

    @PutMapping("/user/{userId}/read-all")
    public void markAllAsRead(@PathVariable Long userId) {
        List<Notification> notis = repo.findByReceiverIdOrderByCreatedAtDesc(userId);
        notis.forEach(n -> n.setIsRead(true));
        repo.saveAll(notis);
    }
}
