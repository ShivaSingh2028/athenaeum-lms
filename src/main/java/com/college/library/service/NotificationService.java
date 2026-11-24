package com.college.library.service;

import com.college.library.entity.Notification;
import com.college.library.entity.User;
import com.college.library.repository.NotificationRepository;
import com.college.library.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<Notification> getNotificationsForUser(User user) {
        return notificationRepository.findByTenantIdAndUserOrderByCreatedAtDesc(TenantContext.getTenantId(), user);
    }

    public List<Notification> forUser(User user) {
        return getNotificationsForUser(user);
    }

    public long getUnreadCount(User user) {
        return notificationRepository.countByTenantIdAndUserAndIsReadFalse(TenantContext.getTenantId(), user);
    }

    public long unreadCount(User user) {
        return getUnreadCount(user);
    }

    @Transactional
    public void notify(User user, String message) {
        Notification n = new Notification(user, "Library Notification", message, "CIRCULATION");
        notificationRepository.save(n);
    }

    @Transactional
    public void markAsRead(Long notificationId, User user) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getTenantId() != null && !n.getTenantId().equals(TenantContext.getTenantId())) {
                throw new org.springframework.security.access.AccessDeniedException("Access denied: Notification belongs to another library tenant.");
            }
            if (user != null && !user.getRole().isStaffOrAdmin() && !n.getUser().getId().equals(user.getId())) {
                throw new org.springframework.security.access.AccessDeniedException("Access denied: You can only modify your own notifications.");
            }
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        markAsRead(notificationId, null);
    }

    @Transactional
    public void markAllAsRead(User user) {
        List<Notification> notifications = notificationRepository.findByTenantIdAndUserOrderByCreatedAtDesc(TenantContext.getTenantId(), user);
        for (Notification n : notifications) {
            if (!n.isRead()) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        }
    }
}
