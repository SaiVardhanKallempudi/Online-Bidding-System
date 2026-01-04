package com.application.example.online_bidding_system.entity;

import jakarta.persistence.*;
import lombok. Getter;
import lombok.Setter;
import java.sql.Timestamp;

@Entity
@Table(name = "comments")
@Getter
@Setter
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType. IDENTITY)
    private Long commentId;

    @ManyToOne
    @JoinColumn(name = "stall_id", nullable = false)
    private Stall stall;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 500, nullable = false)
    private String commentText;

    @Column(updatable = false)
    private Timestamp createdAt;

    private boolean isDeleted = false;

    @PrePersist
    protected void onCreate() {
        createdAt = new Timestamp(System. currentTimeMillis());
    }
}