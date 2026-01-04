package com.application.example.online_bidding_system.entity;

import jakarta.persistence.*;
import lombok. Getter;
import lombok.Setter;
import java.sql. Timestamp;

@Entity
@Table(name = "application_settings")
@Getter
@Setter
public class ApplicationSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String festName;
    private String academicYear;

    private Timestamp applicationStartTime;
    private Timestamp applicationEndTime;

    private boolean isApplicationOpen = false;

    private Timestamp biddingStartTime;
    private Timestamp biddingEndTime;

    private boolean isBiddingOpen = false;

    @Column(updatable = false)
    private Timestamp createdAt;

    private Timestamp updatedAt;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = new Timestamp(System.currentTimeMillis());
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Timestamp(System. currentTimeMillis());
    }
}