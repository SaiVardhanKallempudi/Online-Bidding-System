package com. application.example.online_bidding_system.repository;

import com.application.example.online_bidding_system.entity. EmailOtp;
import com.application.example. online_bidding_system.entity.OtpPurpose;
import org. springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {

    // Existing methods
    Optional<EmailOtp> findByEmail(String email);
    Optional<EmailOtp> findByOtp(String otp);
    Optional<EmailOtp> findByEmailAndOtp(String email, String otp);
    Optional<EmailOtp> findTopByEmailOrderByCreatedAtDesc(String email);
    void deleteByEmail(String email);

    // NEW:  Methods for password reset with purpose
    Optional<EmailOtp> findByEmailAndOtpAndPurpose(String email, String otp, OtpPurpose purpose);
    Optional<EmailOtp> findTopByEmailAndPurposeOrderByCreatedAtDesc(String email, OtpPurpose purpose);
    void deleteByEmailAndPurpose(String email, OtpPurpose purpose);
}