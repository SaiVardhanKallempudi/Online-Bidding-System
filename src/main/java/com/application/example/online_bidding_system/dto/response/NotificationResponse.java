package com.application.example.online_bidding_system.dto.response;

import com.application.example.online_bidding_system.entity.NotificationType;
import com.application.example.online_bidding_system.entity.RelatedEntityType;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class NotificationResponse {
    public Long id;
    public Long userId;
    public NotificationType type;
    public String title;
    public String message;
    public Long relatedEntityId;
    public RelatedEntityType relatedEntityType;
    @JsonProperty("read")
    public boolean isRead;
    public LocalDateTime createdAt;
}