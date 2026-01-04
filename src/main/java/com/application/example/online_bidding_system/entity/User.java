package com.application.example.online_bidding_system.entity;

import jakarta.persistence.*;
import lombok. Getter;
import lombok.Setter;
import java.sql.Timestamp;
import java.util. List;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType. IDENTITY)
    private Long studentId;

    @Column(unique = true)
    private String collageId;

    @Column(nullable = false)
    private String studentName;

    @Column(unique = true, nullable = false)
    private String studentEmail;

    private String password;

    private String profilePicture;

    private String phone;
    private String address;
    private String department;
    private String gender;
    private Integer year;

    @Enumerated(EnumType. STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider authProvider = AuthProvider.LOCAL;

    private String googleId;

    private boolean emailVerified = false;
    private boolean isActive = true;

    @Column(updatable = false)
    private Timestamp createdAt;

    private Timestamp updatedAt;
    private Timestamp lastLogin;

    @OneToOne(mappedBy = "user", cascade = CascadeType. ALL, fetch = FetchType.LAZY)
    private BidderApplication bidderApplication;

    @OneToMany(mappedBy = "bidder", fetch = FetchType. LAZY)
    private List<Bid> bids;

    @OneToMany(mappedBy = "createdBy", fetch = FetchType. LAZY)
    private List<Stall> createdStalls;

    @OneToMany(mappedBy = "winner", fetch = FetchType.LAZY)
    private List<BiddingResult> resultsWon;

    @OneToMany(mappedBy = "user", fetch = FetchType. LAZY)
    private List<Comment> comments;

    @PrePersist
    protected void onCreate() {
        createdAt = new Timestamp(System. currentTimeMillis());
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Timestamp(System.currentTimeMillis());
    }
}