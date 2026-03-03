package com.application.example.online_bidding_system.controller;

import com.application.example.online_bidding_system.dto.request.ForgotPasswordRequest;
import com.application.example.online_bidding_system.dto.request.LoginRequest;
import com.application.example.online_bidding_system.dto.request.ResetPasswordRequest;
import com.application.example.online_bidding_system.dto.request.SignUpRequest;
import com.application.example.online_bidding_system.dto.response.AuthResponse;
import com.application.example.online_bidding_system.dto.response.UserResponse;
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

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

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

    /**
     * SIGNUP - Handles all edge cases:
     * - New user
     * - Existing unverified user (resends OTP, updates details)
     * - Existing verified user (blocks signup)
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@Valid @RequestBody SignUpRequest request) {
        log.info("📝 Signup attempt for email: {}", request.getStudentEmail());

        try {
            Optional<User> existingUser = userRepository.findByStudentEmail(request.getStudentEmail());

            if (existingUser.isPresent()) {
                User user = existingUser.get();
                if (!user.isEmailVerified()) {
                    log.warn("⚠️ Unverified user attempting signup: {}", request.getStudentEmail());

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
                    log.info(" Updated unverified user details: {}", request.getStudentEmail());

                    // Generate and send new OTP
                    String otp = generateAndSaveOtp(request.getStudentEmail());
                    emailService.sendOtpEmail(request.getStudentEmail(), otp);

                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "Account found but not verified. New OTP sent to your email.");
                    response.put("email", request.getStudentEmail());
                    response.put("isExistingUnverified", true);

                    return ResponseEntity.ok(response);
                }

                // CASE 2: User exists and email IS verified
                log.warn("❌ Attempt to signup with already registered email: {}", request.getStudentEmail());
                throw new BadRequestException("Email already registered and verified. Please login instead.");
            }

            // CASE 3: New user signup
            log.info("Creating new user: {}", request.getStudentEmail());

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
            log.info("User saved to database: {}", request.getStudentEmail());

            // Generate and send OTP
            String otp = generateAndSaveOtp(request.getStudentEmail());
            emailService.sendOtpEmail(request.getStudentEmail(), otp);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Registration successful! OTP sent to your email.");
            response.put("email", request.getStudentEmail());

            return ResponseEntity.ok(response);

        } catch (BadRequestException e) {
            log.error("❌ Signup failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("❌ Unexpected error during signup: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "An unexpected error occurred. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * LOGIN - Handles all edge cases:
     * - Valid credentials
     * - Invalid credentials
     * - Unverified email
     * - Inactive account
     * - OAuth user (no password)
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        log.info("🔐 Login attempt for email: {}", request.getStudentEmail());

        try {
            // Find user
            User user = userRepository.findByStudentEmail(request.getStudentEmail())
                    .orElseThrow(() -> {
                        log.warn("❌ User not found: {}", request.getStudentEmail());
                        return new BadRequestException("Invalid email or password");
                    });

            //  CASE 1: Email not verified
            if (!user.isEmailVerified()) {
                log.warn("⚠️ Login attempt with unverified email: {}", request.getStudentEmail());

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Please verify your email first. Check your inbox for OTP.");
                errorResponse.put("email", request.getStudentEmail());
                errorResponse.put("requiresVerification", true);
                errorResponse.put("code", "EMAIL_NOT_VERIFIED");

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }

            // CASE 2: OAuth user trying to login with password
            if (user.getAuthProvider() != AuthProvider.LOCAL) {
                log.warn("⚠️ OAuth user attempting password login: {}", request.getStudentEmail());
                throw new BadRequestException("This account uses Google login. Please use 'Login with Google'.");
            }

            // CASE 3: Check password
            if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                log.warn("❌ Invalid password for user: {}", request.getStudentEmail());
                throw new BadRequestException("Invalid email or password");
            }

            // CASE 4: Account inactive
            if (!user.isActive()) {
                log.warn("⚠️ Inactive account login attempt: {}", request.getStudentEmail());
                throw new BadRequestException("Account is deactivated. Please contact support.");
            }

            // Successful login
            log.info("✅ Login successful for: {}", request.getStudentEmail());

            // Generate JWT token
            String token = jwtUtils.generateToken(user);

            // Update last login
            user.setLastLogin(new Timestamp(System.currentTimeMillis()));
            userRepository.save(user);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Login successful");
            response.put("token", token);
            response.put("user", mapToUserResponse(user));

            return ResponseEntity.ok(response);

        } catch (BadRequestException e) {
            log.error("❌ Login failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("❌ Unexpected login error: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "An unexpected error occurred. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * VERIFY OTP - No changes needed, delegating to service
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@RequestParam String email, @RequestParam String otp) {
        log.info("🔍 OTP verification attempt for: {}", email);
        return authService.verifyOtp(email, otp);
    }

    /**
     * RESEND OTP - No changes needed, delegating to service
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<AuthResponse> resendOtp(@RequestParam String email) {
        log.info("🔄 Resending OTP for: {}", email);
        return authService.resendOtp(email);
    }

    /**
     * GET CURRENT USER
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            return authService.getCurrentUser(jwtToken);
        } catch (Exception e) {
            log.error("❌ Error getting current user: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Invalid or expired token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    /*
     * FORGOT PASSWORD - Step 1: Request password reset
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("🔑 Forgot password request for: {}", request.getStudentEmail());

        try {
            // Check if user exists
            User user = userRepository.findByStudentEmail(request.getStudentEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getStudentEmail()));

            //  Check if user is OAuth user
            if (user.getAuthProvider() != AuthProvider.LOCAL) {
                throw new BadRequestException("This account uses Google login. Password reset is not available.");
            }

            //  Check if email is verified
            if (!user.isEmailVerified()) {
                throw new BadRequestException("Email not verified. Please verify your email first.");
            }

            return authService.forgotPassword(request.getStudentEmail());

        } catch (ResourceNotFoundException | BadRequestException e) {
            log.error("❌ Forgot password failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * VERIFY RESET OTP - Step 2
     */
    @PostMapping("/verify-reset-otp")
    public ResponseEntity<AuthResponse> verifyResetOtp(
            @RequestParam String email,
            @RequestParam String otp) {
        log.info("🔍 Verifying reset OTP for: {}", email);
        return authService.verifyResetOtp(email, otp);
    }

    /**
     *  RESET PASSWORD - Step 3
     */
    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("🔒 Password reset for: {}", request.getStudentEmail());
        return authService.resetPassword(request);
    }

    /**
     * RESEND RESET OTP
     */
    @PostMapping("/resend-reset-otp")
    public ResponseEntity<AuthResponse> resendResetOtp(@RequestParam String email) {
        log.info("🔄 Resending reset OTP for: {}", email);
        return authService.resendResetOtp(email);
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    /**
     * Generate and save OTP
     */
    private String generateAndSaveOtp(String email) {
        // Delete old OTP
        emailOtpRepository.findByEmail(email).ifPresent(emailOtpRepository::delete);

        // Generate new OTP
        String otp = String.format("%06d", new Random().nextInt(999999));

        EmailOtp otpEntity = new EmailOtp();
        otpEntity.setEmail(email);
        otpEntity.setOtp(otp);
        otpEntity.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        otpEntity.setVerified(false);
        emailOtpRepository.save(otpEntity);

        log.info("✅ OTP generated and saved for: {}", email);
        return otp;
    }

    /**
     * Map User to UserResponse
     */
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