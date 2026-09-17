package com.college.library.service;

import com.college.library.entity.Notification;
import com.college.library.entity.User;
import com.college.library.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void notify(User user, String message) {
        Notification n = new Notification();
        n.setUser(user);
        n.setMessage(message);
        n.setCreatedAt(LocalDateTime.now());
        n.setRead(false);
        notificationRepository.save(n);
    }

    public List<Notification> forUser(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public long unreadCount(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    public void markAllRead(User user) {
        List<Notification> all = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        all.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(all);
    }
}
