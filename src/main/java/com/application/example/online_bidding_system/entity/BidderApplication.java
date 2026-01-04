package com.application. example.online_bidding_system.entity;

import jakarta.persistence.*;
import lombok. Getter;
import lombok.Setter;
import java.sql.Timestamp;

@Entity
@Table(name = "bidder_applications")
@Getter
@Setter
public class BidderApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long applicationId;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    private Long phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status. PENDING;

    @Column(length = 500)
    private String reason;

    private String preferredStallCategory;

    @Column(updatable = false)
    private Timestamp appliedAt;

    private Timestamp reviewedAt;

    @ManyToOne
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(length = 500)
    private String rejectionReason;

    @PrePersist
    protected void onCreate() {
        appliedAt = new Timestamp(System.currentTimeMillis());
    }
}