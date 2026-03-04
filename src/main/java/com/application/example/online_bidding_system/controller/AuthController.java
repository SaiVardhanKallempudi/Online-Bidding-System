package com.application.example.online_bidding_system.controller;

import com.application.example.online_bidding_system.dto.request.ForgotPasswordRequest;
import com.application.example.online_bidding_system.dto.request.LoginRequest;
import com.application.example.online_bidding_system.dto.request.ResetPasswordRequest;
import com.application.example.online_bidding_system.dto.request.SignUpRequest;
import com.application.example.online_bidding_system.dto.response.AuthResponse;
import com.application.example.online_bidding_system.entity.EmailOtp;
import com.application.example.online_bidding_system.entity.Role;
import com.application.example.online_bidding_system.entity.User;
import com.application.example.online_bidding_system.entity.AuthProvider;
import com.application.example.online_bidding_system.exception.BadRequestException;
import com.application.example.online_bidding_system.exception.ResourceNotFoundException;
import com.application.example.online_bidding_system.repository.EmailOtpRepository;
import com.application.example.online_bidding_system.repository.UserRepository;
import com.application.example.online_bidding_system.security.JwtUtils;
import com.application.example.online_bidding_system.service.AuthService;
import com.application.example.online_bidding_system.service.Emailservice;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailOtpRepository emailOtpRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Emailservice emailService;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@Valid @RequestBody SignUpRequest request) {
        log.info("Signup attempt for email: {}", request.getStudentEmail());

        try {
            Optional<User> existingUser = userRepository.findByStudentEmail(request.getStudentEmail());

            if (existingUser.isPresent()) {
                User user = existingUser.get();
                if (!user.isEmailVerified()) {
                    user.setStudentName(request.getStudentName());
                    user.setCollageId(request.getCollageId());
                    user.setPhone(request.getPhone());
                    user.setDepartment(request.getDepartment());
                    user.setYear(request.getYear());
                    user.setGender(request.getGender());
                    user.setAddress(request.getAddress());

                    if (request.getPassword() != null && !request.getPassword().isEmpty()) {
                        user.setPassword(passwordEncoder.encode(request.getPassword()));
                    }

                    userRepository.save(user);

                    String otp = generateAndSaveOtp(request.getStudentEmail());
                    emailService.sendOtpEmail(request.getStudentEmail(), otp);

                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "Account found but not verified. New OTP sent to your email.");
                    response.put("email", request.getStudentEmail());
                    response.put("isExistingUnverified", true);

                    return ResponseEntity.ok(response);
                }

                throw new BadRequestException("Email already registered and verified. Please login instead.");
            }

            User newUser = new User();
            newUser.setStudentName(request.getStudentName());
            newUser.setStudentEmail(request.getStudentEmail());
            newUser.setPassword(passwordEncoder.encode(request.getPassword()));
            newUser.setCollageId(request.getCollageId());
            newUser.setPhone(request.getPhone());
            newUser.setDepartment(request.getDepartment());
            newUser.setYear(request.getYear());
            newUser.setGender(request.getGender());
            newUser.setAddress(request.getAddress());
            newUser.setRole(Role.USER);
            newUser.setEmailVerified(false);
            newUser.setActive(true);
            newUser.setAuthProvider(AuthProvider.LOCAL);

            userRepository.save(newUser);

            String otp = generateAndSaveOtp(request.getStudentEmail());
            emailService.sendOtpEmail(request.getStudentEmail(), otp);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Registration successful! OTP sent to your email.");
            response.put("email", request.getStudentEmail());

            return ResponseEntity.ok(response);

        } catch (BadRequestException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("Unexpected error during signup: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "An unexpected error occurred. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for email: {}", request.getStudentEmail());

        try {
            User user = userRepository.findByStudentEmail(request.getStudentEmail())
                    .orElseThrow(() -> new BadRequestException("Invalid email or password"));

            if (!user.isEmailVerified()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Please verify your email first. Check your inbox for OTP.");
                errorResponse.put("email", request.getStudentEmail());
                errorResponse.put("requiresVerification", true);
                errorResponse.put("code", "EMAIL_NOT_VERIFIED");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }

            if (user.getAuthProvider() != AuthProvider.LOCAL) {
                throw new BadRequestException("This account uses Google login. Please use 'Login with Google'.");
            }

            if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new BadRequestException("Invalid email or password");
            }

            if (!user.isActive()) {
                throw new BadRequestException("Account is deactivated. Please contact support.");
            }

            String token = jwtUtils.generateToken(user);

            user.setLastLogin(new Timestamp(System.currentTimeMillis()));
            userRepository.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Login successful");
            response.put("token", token);
            response.put("user", mapToUserResponse(user));

            return ResponseEntity.ok(response);

        } catch (BadRequestException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("Unexpected login error: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "An unexpected error occurred. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@RequestParam String email, @RequestParam String otp) {
        return authService.verifyOtp(email, otp);
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<AuthResponse> resendOtp(@RequestParam String email) {
        return authService.resendOtp(email);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            return authService.getCurrentUser(jwtToken);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Invalid or expired token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            User user = userRepository.findByStudentEmail(request.getStudentEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getStudentEmail()));

            if (user.getAuthProvider() != AuthProvider.LOCAL) {
                throw new BadRequestException("This account uses Google login. Password reset is not available.");
            }

            if (!user.isEmailVerified()) {
                throw new BadRequestException("Email not verified. Please verify your email first.");
            }

            return authService.forgotPassword(request.getStudentEmail());

        } catch (ResourceNotFoundException | BadRequestException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/verify-reset-otp")
    public ResponseEntity<AuthResponse> verifyResetOtp(@RequestParam String email, @RequestParam String otp) {
        return authService.verifyResetOtp(email, otp);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

    @PostMapping("/resend-reset-otp")
    public ResponseEntity<AuthResponse> resendResetOtp(@RequestParam String email) {
        return authService.resendResetOtp(email);
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    private String generateAndSaveOtp(String email) {
        emailOtpRepository.findByEmail(email).ifPresent(emailOtpRepository::delete);

        // FIX: Use SecureRandom instead of Random
        String otp = String.format("%06d", SECURE_RANDOM.nextInt(999999));

        EmailOtp otpEntity = new EmailOtp();
        otpEntity.setEmail(email);
        otpEntity.setOtp(otp);
        otpEntity.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        otpEntity.setVerified(false);
        emailOtpRepository.save(otpEntity);

        return otp;
    }

    private Map<String, Object> mapToUserResponse(User user) {
        Map<String, Object> userResponse = new HashMap<>();
        userResponse.put("studentId", user.getStudentId());
        userResponse.put("studentName", user.getStudentName());
        userResponse.put("studentEmail", user.getStudentEmail());
        userResponse.put("collageId", user.getCollageId());
        userResponse.put("phone", user.getPhone());
        userResponse.put("department", user.getDepartment());
        userResponse.put("year", user.getYear());
        userResponse.put("gender", user.getGender());
        userResponse.put("address", user.getAddress());
        userResponse.put("role", user.getRole().name());
        userResponse.put("profilePicture", user.getProfilePicture());
        userResponse.put("emailVerified", user.isEmailVerified());
        userResponse.put("authProvider", user.getAuthProvider().name());
        return userResponse;
    }
}