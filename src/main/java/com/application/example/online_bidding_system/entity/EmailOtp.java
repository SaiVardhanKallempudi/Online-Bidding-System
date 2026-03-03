package com.application. example.online_bidding_system.entity;

import jakarta.persistence.*;
import lombok. Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Entity
@Table(name = "email_otp")
@Getter
@Setter
public class EmailOtp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String otp;

    @Column(nullable = false)
    private Timestamp createdAt;

    private Timestamp expiresAt;

    private boolean verified = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpPurpose purpose = OtpPurpose.EMAIL_VERIFICATION;
}