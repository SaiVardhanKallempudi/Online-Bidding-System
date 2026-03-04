package com.application.example.online_bidding_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId; // Optional: reference to User entity by id

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private String title;
    private String message;

    private Long relatedEntityId;

    @Enumerated(EnumType.STRING)
    private RelatedEntityType relatedEntityType;

    private boolean isRead;

    private LocalDateTime createdAt;

    // Constructors, getters, setters
}