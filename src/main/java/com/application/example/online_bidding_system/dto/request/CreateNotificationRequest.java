package com.application.example.online_bidding_system.dto.request;

import com.application.example.online_bidding_system.entity.NotificationType;
import com.application.example.online_bidding_system.entity.RelatedEntityType;

public class CreateNotificationRequest {
    public Long userId;
    public NotificationType type;
    public String title;
    public String message;
    public Long relatedEntityId;
    public RelatedEntityType relatedEntityType;
}