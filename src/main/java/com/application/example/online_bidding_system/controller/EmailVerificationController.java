package com.application.example.online_bidding_system.controller;

import com.application. example.online_bidding_system.entity.EmailOtp;
import com.application.example.online_bidding_system.entity.User;
import com.application.example. online_bidding_system.repository.EmailOtpRepository;
import com.application.example. online_bidding_system.service. Emailservice;
import org.springframework.beans. factory.annotation. Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework. web.bind.annotation.*;
import com.application.example. online_bidding_system.dto.request.OtpVerifyRequest;
import com.application.example.online_bidding_system.repository.UserRepository;

import java.sql.Timestamp;
import java.util. HashMap;
import java. util.Map;
import java.util. Optional;
import java.util. Random;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/otp")
public class EmailVerificationController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmailOtpRepository emailOtpRepository;

    @Autowired
    private Emailservice emailservice;

    @GetMapping("/send")
    public ResponseEntity<Map<String, String>> sendOtp(@RequestParam String email) {
        // Delete any existing OTP for this email
        emailOtpRepository.findByEmail(email).ifPresent(emailOtpRepository::delete);

        String otp = String.format("%06d", new Random().nextInt(999999));

        EmailOtp otpEntity = new EmailOtp();
        otpEntity. setEmail(email);
        otpEntity.setOtp(otp);
        otpEntity. setCreatedAt(new Timestamp(System.currentTimeMillis()));
        otpEntity.setVerified(false);
        emailOtpRepository.save(otpEntity);

        emailservice.sendOtpEmail(email, otp);

        Map<String, String> response = new HashMap<>();
        response.put("success", "true");
        response.put("message", "OTP sent to " + email);
        return ResponseEntity. ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyOtp(
            @RequestBody OtpVerifyRequest request) {

        String email = request.getStudentEmail();
        String otp = request.getOtp();

        Map<String, String> response = new HashMap<>();

        Optional<EmailOtp> opt =
                emailOtpRepository.findTopByEmailOrderByCreatedAtDesc(email);

        if (opt.isEmpty()) {
            response.put("success", "false");
            response.put("message", "No OTP found for this email.");
            return ResponseEntity.badRequest().body(response);
        }

        EmailOtp savedOtp = opt.get();
        long diff = System.currentTimeMillis() - savedOtp.getCreatedAt().getTime();

        if (diff > 5 * 60 * 1000) {
            response.put("success", "false");
            response.put("message", "OTP expired.");
            return ResponseEntity.badRequest().body(response);
        }

        if (!savedOtp.getOtp().equals(otp)) {
            response.put("success", "false");
            response.put("message", "Invalid OTP.");
            return ResponseEntity.badRequest().body(response);
        }

        // ✅ MARK OTP VERIFIED
        savedOtp.setVerified(true);
        emailOtpRepository.save(savedOtp);

        // ✅ MARK USER VERIFIED (THIS WAS MISSING)
        User user = userRepository.findByStudentEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setEmailVerified(true);
        userRepository.save(user);

        response.put("success", "true");
        response.put("message", "OTP verified successfully.");
        return ResponseEntity.ok(response);
    }


    @PostMapping("/resend")
    public ResponseEntity<Map<String, String>> resendOtp(@RequestParam String email) {

        String otp = String.format("%06d", new Random().nextInt(999999));

        EmailOtp otpEntity = new EmailOtp();
        otpEntity.setEmail(email);
        otpEntity.setOtp(otp);
        otpEntity.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        otpEntity.setVerified(false);

        emailOtpRepository.save(otpEntity);
        emailservice.sendOtpEmail(email, otp);

        Map<String, String> response = new HashMap<>();
        response.put("success", "true");
        response.put("message", "OTP resent to " + email);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/latest-verified")
    public ResponseEntity<Map<String, Object>> getLatestVerifiedOtp(@RequestParam String email) {
        Optional<EmailOtp> otp = emailservice.getLatestVerifiedOtp(email);
        Map<String, Object> response = new HashMap<>();
        response.put("verified", otp.isPresent());
        otp.ifPresent(o -> response. put("otp", o.getOtp()));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-status")
    public ResponseEntity<Map<String, Object>> checkOtpStatus(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();

        Optional<EmailOtp> otpOpt = emailOtpRepository. findTopByEmailOrderByCreatedAtDesc(email);

        if (otpOpt.isEmpty()) {
            response.put("exists", false);
            response.put("verified", false);
            return ResponseEntity.ok(response);
        }

        EmailOtp otp = otpOpt.get();
        long diff = System.currentTimeMillis() - otp.getCreatedAt().getTime();
        boolean expired = diff > 5 * 60 * 1000;

        response.put("exists", true);
        response.put("verified", otp.isVerified());
        response.put("expired", expired);

        return ResponseEntity.ok(response);
    }
}