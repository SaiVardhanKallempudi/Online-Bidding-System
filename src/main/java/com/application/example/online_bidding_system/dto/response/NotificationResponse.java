package com.application.example.online_bidding_system.dto.response;

import com.application.example.online_bidding_system.entity.NotificationType;
import com.application.example.online_bidding_system.entity.RelatedEntityType;

import java.time.LocalDateTime;

public class NotificationResponse {
    public Long id;
    public Long userId;
    public NotificationType type;
    public String title;
    public String message;
    public Long relatedEntityId;
    public RelatedEntityType relatedEntityType;
    public boolean isRead;
    public LocalDateTime createdAt;
}