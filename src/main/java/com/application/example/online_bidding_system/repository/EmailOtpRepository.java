package com.application.example.online_bidding_system.repository;

import com.application. example.online_bidding_system.entity.EmailOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype. Repository;
import java.util. Optional;

@Repository
public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {
    Optional<EmailOtp> findByEmail(String email);
    Optional<EmailOtp> findByOtp(String otp);
    Optional<EmailOtp> findByEmailAndOtp(String email, String otp);
    Optional<EmailOtp> findTopByEmailOrderByCreatedAtDesc(String email);
    void deleteByEmail(String email);
}