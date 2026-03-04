package com.application.example.online_bidding_system.service;

import com.application.example.online_bidding_system.dto.request.CreateNotificationRequest;
import com.application.example.online_bidding_system.dto.response.NotificationResponse;
import com.application.example.online_bidding_system.entity.Notification;
import com.application.example.online_bidding_system.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    public NotificationResponse createNotification(CreateNotificationRequest req) {
        Notification notification = new Notification();
        notification.setUserId(req.userId);
        notification.setType(req.type);
        notification.setTitle(req.title);
        notification.setMessage(req.message);
        notification.setRelatedEntityId(req.relatedEntityId);
        notification.setRelatedEntityType(req.relatedEntityType);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        Notification saved = notificationRepository.save(notification);
        return toResponse(saved);
    }

    public List<NotificationResponse> getAllNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<NotificationResponse> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    public void markAsRead(Long notificationId, Long userId) {
        Notification n = notificationRepository.findById(notificationId)
                .filter(notif -> notif.getUserId().equals(userId)).orElseThrow();
        n.setRead(true);
        notificationRepository.save(n);
    }

    public void markAllAsRead(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        for (Notification n : notifications) {
            n.setRead(true);
        }
        notificationRepository.saveAll(notifications);
    }

    public void deleteNotification(Long notificationId, Long userId) {
        Notification n = notificationRepository.findById(notificationId)
                .filter(notif -> notif.getUserId().equals(userId)).orElseThrow();
        notificationRepository.delete(n);
    }

    public void deleteAllNotifications(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        notificationRepository.deleteAll(notifications);
    }

    private NotificationResponse toResponse(Notification n) {
        NotificationResponse r = new NotificationResponse();
        r.id = n.getId();
        r.userId = n.getUserId();
        r.type = n.getType();
        r.title = n.getTitle();
        r.message = n.getMessage();
        r.relatedEntityId = n.getRelatedEntityId();
        r.relatedEntityType = n.getRelatedEntityType();
        r.isRead = n.isRead();
        r.createdAt = n.getCreatedAt();
        return r;
    }
}
