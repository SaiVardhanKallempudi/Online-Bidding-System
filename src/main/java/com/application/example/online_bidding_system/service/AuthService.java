package com.application.example.online_bidding_system.service;

import com.application.example.online_bidding_system.dto.request.LoginRequest;
import com.application.example.online_bidding_system.dto.request.ResetPasswordRequest;
import com.application.example.online_bidding_system.dto.request.SignUpRequest;
import com.application.example.online_bidding_system.dto.response.AuthResponse;
import com.application.example.online_bidding_system.dto.response.UserResponse;
import com.application.example.online_bidding_system.entity.*;
import com.application.example.online_bidding_system.exception.BadRequestException;
import com.application.example.online_bidding_system.exception.ResourceNotFoundException;
import com.application.example.online_bidding_system.exception.UnauthorizedException;
import com.application.example.online_bidding_system.repository.EmailOtpRepository;
import com.application.example.online_bidding_system.repository.UserRepository;
import com.application.example.online_bidding_system.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Random;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailOtpRepository emailOtpRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private Emailservice emailService;

    @Autowired
    private NotificationTriggerService notificationTrigger; // ✅ added

    private static final long OTP_EXPIRY_MS = 5 * 60 * 1000; // 5 minutes

    // ==================== SIGNUP ====================
    @Transactional
    public ResponseEntity<AuthResponse> signUp(SignUpRequest request) {
        if (userRepository.existsByStudentEmail(request.getStudentEmail())) {
            throw new BadRequestException("Email already registered: " + request.getStudentEmail());
        }
        if (request.getCollageId() != null && userRepository.existsByCollageId(request.getCollageId())) {
            throw new BadRequestException("College ID already registered: " + request.getCollageId());
        }

        User user = new User();
        user.setStudentName(request.getStudentName());
        user.setStudentEmail(request.getStudentEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCollageId(request.getCollageId());
        user.setDepartment(request.getDepartment());
        user.setYear(request.getYear());
        user.setGender(request.getGender());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setRole(Role.USER);
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setEmailVerified(false);
        user.setActive(true);
        userRepository.save(user);

        String otp = generateOtp();
        saveOtp(request.getStudentEmail(), otp, OtpPurpose.EMAIL_VERIFICATION);
        emailService.sendOtpEmail(request.getStudentEmail(), otp);

        return ResponseEntity.ok(new AuthResponse(
                true,
                "Registration successful! Please verify your email with the OTP sent to your inbox.",
                null,
                mapToUserResponse(user)
        ));
    }

    // ==================== LOGIN ====================
    public ResponseEntity<AuthResponse> login(LoginRequest request) {
        User user = userRepository.findByStudentEmail(request.getStudentEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!user.isActive()) {
            throw new UnauthorizedException("Account is deactivated. Please contact admin.");
        }

        if (user.getAuthProvider() == AuthProvider.GOOGLE && user.getPassword() == null) {
            throw new BadRequestException("This account uses Google Sign-In. Please use Google to login.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (user.getRole() != Role.ADMIN && !user.isEmailVerified()) {
            String otp = generateOtp();
            saveOtp(user.getStudentEmail(), otp, OtpPurpose.EMAIL_VERIFICATION);
            emailService.sendOtpEmail(user.getStudentEmail(), otp);
            throw new UnauthorizedException("Email not verified. A new OTP has been sent to your email.");
        }

        user.setLastLogin(new Timestamp(System.currentTimeMillis()));
        userRepository.save(user);

        String token = jwtUtils.generateToken(user);

        return ResponseEntity.ok(new AuthResponse(true, "Login successful", token, mapToUserResponse(user)));
    }

    // ==================== VERIFY OTP (Email Verification) ====================
    @Transactional
    public ResponseEntity<AuthResponse> verifyOtp(String email, String otp) {
        EmailOtp otpRecord = emailOtpRepository.findByEmailAndOtp(email, otp)
                .orElseThrow(() -> new BadRequestException("Invalid OTP"));

        if (isOtpExpired(otpRecord)) {
            emailOtpRepository.delete(otpRecord);
            throw new BadRequestException("OTP expired. Please request a new one.");
        }

        User user = userRepository.findByStudentEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        boolean isFirstVerification = !user.isEmailVerified(); // ✅ track before updating

        user.setEmailVerified(true);
        userRepository.save(user);

        emailOtpRepository.delete(otpRecord);

        // ✅ Send welcome notification only on FIRST email verification
        if (isFirstVerification) {
            notificationTrigger.notifyWelcome(user.getStudentId(), user.getStudentName());
        }

        String token = jwtUtils.generateToken(user);

        return ResponseEntity.ok(new AuthResponse(
                true,
                "Email verified successfully!",
                token,
                mapToUserResponse(user)
        ));
    }

    // ==================== RESEND OTP ====================
    public ResponseEntity<AuthResponse> resendOtp(String email) {
        User user = userRepository.findByStudentEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        String otp = generateOtp();
        saveOtp(email, otp, OtpPurpose.EMAIL_VERIFICATION);
        emailService.sendOtpEmail(email, otp);

        return ResponseEntity.ok(new AuthResponse(true, "OTP sent successfully to " + email, null, null));
    }

    // ==================== GET CURRENT USER ====================
    public ResponseEntity<UserResponse> getCurrentUser(String token) {
        if (!jwtUtils.validateToken(token)) {
            throw new UnauthorizedException("Invalid or expired token");
        }

        String email = jwtUtils.getEmailFromToken(token);
        User user = userRepository.findByStudentEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        return ResponseEntity.ok(mapToUserResponse(user));
    }

    // ==================== FORGOT PASSWORD ====================
    @Transactional
    public ResponseEntity<AuthResponse> forgotPassword(String email) {
        User user = userRepository.findByStudentEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (user.getAuthProvider() == AuthProvider.GOOGLE && user.getPassword() == null) {
            throw new BadRequestException("This account uses Google Sign-In. Password reset is not available.");
        }

        if (!user.isActive()) {
            throw new BadRequestException("Account is deactivated. Please contact admin.");
        }

        String otp = generateOtp();
        saveOtp(email, otp, OtpPurpose.PASSWORD_RESET);
        emailService.sendPasswordResetOtpEmail(email, user.getStudentName(), otp);

        return ResponseEntity.ok(new AuthResponse(
                true,
                "Password reset OTP sent to your email. Please check your inbox.",
                null, null
        ));
    }

    // ==================== VERIFY RESET OTP ====================
    @Transactional
    public ResponseEntity<AuthResponse> verifyResetOtp(String email, String otp) {
        EmailOtp otpRecord = emailOtpRepository.findByEmailAndOtpAndPurpose(email, otp, OtpPurpose.PASSWORD_RESET)
                .orElseThrow(() -> new BadRequestException("Invalid OTP"));

        if (isOtpExpired(otpRecord)) {
            emailOtpRepository.delete(otpRecord);
            throw new BadRequestException("OTP has expired. Please request a new one.");
        }

        otpRecord.setVerified(true);
        emailOtpRepository.save(otpRecord);

        return ResponseEntity.ok(new AuthResponse(
                true,
                "OTP verified successfully. You can now reset your password.",
                null, null
        ));
    }

    // ==================== RESET PASSWORD ====================
    @Transactional
    public ResponseEntity<AuthResponse> resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        EmailOtp otpRecord = emailOtpRepository.findByEmailAndOtpAndPurpose(
                        request.getStudentEmail(), request.getOtp(), OtpPurpose.PASSWORD_RESET)
                .orElseThrow(() -> new BadRequestException("Invalid or expired OTP. Please request a new one."));

        if (!otpRecord.isVerified()) {
            throw new BadRequestException("OTP not verified. Please verify OTP first.");
        }

        if (isOtpExpired(otpRecord)) {
            emailOtpRepository.delete(otpRecord);
            throw new BadRequestException("OTP has expired. Please request a new one.");
        }

        User user = userRepository.findByStudentEmail(request.getStudentEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getStudentEmail()));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        emailOtpRepository.delete(otpRecord);

        emailService.sendPasswordResetSuccessEmail(user.getStudentEmail(), user.getStudentName());

        return ResponseEntity.ok(new AuthResponse(
                true,
                "Password reset successful! You can now login with your new password.",
                null, null
        ));
    }

    // ==================== RESEND RESET OTP ====================
    @Transactional
    public ResponseEntity<AuthResponse> resendResetOtp(String email) {
        User user = userRepository.findByStudentEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (user.getAuthProvider() == AuthProvider.GOOGLE && user.getPassword() == null) {
            throw new BadRequestException("This account uses Google Sign-In. Password reset is not available.");
        }

        String otp = generateOtp();
        saveOtp(email, otp, OtpPurpose.PASSWORD_RESET);
        emailService.sendPasswordResetOtpEmail(email, user.getStudentName(), otp);

        return ResponseEntity.ok(new AuthResponse(true, "Password reset OTP resent successfully!", null, null));
    }

    // ==================== HELPER METHODS ====================

    private String generateOtp() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    private void saveOtp(String email, String otp, OtpPurpose purpose) {
        emailOtpRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, purpose)
                .ifPresent(emailOtpRepository::delete);

        EmailOtp otpEntity = new EmailOtp();
        otpEntity.setEmail(email);
        otpEntity.setOtp(otp);
        otpEntity.setPurpose(purpose);
        otpEntity.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        otpEntity.setExpiresAt(new Timestamp(System.currentTimeMillis() + OTP_EXPIRY_MS));
        otpEntity.setVerified(false);
        emailOtpRepository.save(otpEntity);
    }

    private void saveOtp(String email, String otp) {
        saveOtp(email, otp, OtpPurpose.EMAIL_VERIFICATION);
    }

    private boolean isOtpExpired(EmailOtp otpRecord) {
        if (otpRecord.getExpiresAt() != null) {
            return new Timestamp(System.currentTimeMillis()).after(otpRecord.getExpiresAt());
        }
        return System.currentTimeMillis() - otpRecord.getCreatedAt().getTime() > OTP_EXPIRY_MS;
    }

    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setStudentId(user.getStudentId());
        response.setStudentName(user.getStudentName());
        response.setStudentEmail(user.getStudentEmail());
        response.setCollageId(user.getCollageId());
        response.setDepartment(user.getDepartment());
        response.setYear(user.getYear());
        response.setGender(user.getGender());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());
        response.setProfilePicture(user.getProfilePicture());
        response.setEmailVerified(user.isEmailVerified());
        return response;
    }
}