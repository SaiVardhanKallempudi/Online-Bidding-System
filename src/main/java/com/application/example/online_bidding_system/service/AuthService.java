package com.application.example.online_bidding_system.service;

import com.application.example.online_bidding_system. dto.request.LoginRequest;
import com. application.example.online_bidding_system.dto.request. ResetPasswordRequest;
import com.application. example.online_bidding_system.dto.request.SignUpRequest;
import com.application.example. online_bidding_system.dto.response.AuthResponse;
import com.application. example.online_bidding_system.dto.response.UserResponse;
import com. application.example.online_bidding_system.entity.*;
import com.application.example.online_bidding_system. exception.BadRequestException;
import com. application.example.online_bidding_system.exception.ResourceNotFoundException;
import com. application.example.online_bidding_system.exception.UnauthorizedException;
import com. application.example.online_bidding_system.repository.EmailOtpRepository;
import com.application. example.online_bidding_system.repository.UserRepository;
import com.application.example. online_bidding_system.security.JwtUtils;
import org.springframework.beans.factory. annotation.Autowired;
import org. springframework.http.ResponseEntity;
import org.springframework. security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework. transaction.annotation. Transactional;

import java.sql.Timestamp;
import java.util. Random;

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

    private static final long OTP_EXPIRY_MS = 5 * 60 * 1000; // 5 minutes

    // ==================== SIGNUP ====================
    @Transactional
    public ResponseEntity<AuthResponse> signUp(SignUpRequest request) {
        // Validate email not already registered
        if (userRepository.existsByStudentEmail(request. getStudentEmail())) {
            throw new BadRequestException("Email already registered:  " + request.getStudentEmail());
        }

        // Validate college ID not already registered
        if (request.getCollageId() != null && userRepository.existsByCollageId(request. getCollageId())) {
            throw new BadRequestException("College ID already registered:  " + request.getCollageId());
        }

        // Create new user
        User user = new User();
        user.setStudentName(request.getStudentName());
        user.setStudentEmail(request.getStudentEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCollageId(request. getCollageId());
        user.setDepartment(request.getDepartment());
        user.setYear(request.getYear());
        user.setGender(request.getGender());
        user.setPhone(request. getPhone());
        user.setAddress(request.getAddress());
        user.setRole(Role.USER);
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setEmailVerified(false);
        user.setActive(true);

        userRepository.save(user);

        // Generate and send OTP
        String otp = generateOtp();
        saveOtp(request. getStudentEmail(), otp, OtpPurpose.EMAIL_VERIFICATION);
        emailService.sendOtpEmail(request.getStudentEmail(), otp);

        return ResponseEntity.ok(new AuthResponse(
                true,
                "Registration successful!  Please verify your email with OTP.",
                null,
                mapToUserResponse(user)
        ));
    }

    // ==================== LOGIN ====================
    public ResponseEntity<AuthResponse> login(LoginRequest request) {
        // Find user by email
        User user = userRepository.findByStudentEmail(request.getStudentEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        // Check if account is active
        if (!user.isActive()) {
            throw new UnauthorizedException("Account is deactivated.  Please contact admin.");
        }

        // Check if user registered with Google
        if (user.getAuthProvider() == AuthProvider. GOOGLE && user.getPassword() == null) {
            throw new BadRequestException("This account uses Google Sign-In. Please use Google to login.");
        }

        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        // Check email verification (except for admin)
        if (user.getRole() != Role.ADMIN && ! user.isEmailVerified()) {
            String otp = generateOtp();
            saveOtp(user.getStudentEmail(), otp, OtpPurpose.EMAIL_VERIFICATION);
            emailService.sendOtpEmail(user.getStudentEmail(), otp);
            throw new UnauthorizedException("Email not verified.  A new OTP has been sent to your email.");
        }

        // Update last login
        user.setLastLogin(new Timestamp(System.currentTimeMillis()));
        userRepository.save(user);

        // Generate token
        String token = jwtUtils.generateToken(user);

        return ResponseEntity. ok(new AuthResponse(
                true,
                "Login successful",
                token,
                mapToUserResponse(user)
        ));
    }

    // ==================== VERIFY OTP (Email Verification) ====================
    @Transactional
    public ResponseEntity<AuthResponse> verifyOtp(String email, String otp) {
        // Find OTP record
        EmailOtp otpRecord = emailOtpRepository.findByEmailAndOtp(email, otp)
                .orElseThrow(() -> new BadRequestException("Invalid OTP"));

        // Check if OTP expired (5 minutes)
        if (isOtpExpired(otpRecord)) {
            emailOtpRepository. delete(otpRecord);
            throw new BadRequestException("OTP expired. Please request a new one.");
        }

        // Find and update user
        User user = userRepository.findByStudentEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        user.setEmailVerified(true);
        userRepository.save(user);

        // Delete used OTP
        emailOtpRepository.delete(otpRecord);

        // Generate token
        String token = jwtUtils.generateToken(user);

        return ResponseEntity.ok(new AuthResponse(
                true,
                "Email verified successfully! ",
                token,
                mapToUserResponse(user)
        ));
    }

    // ==================== RESEND OTP (Email Verification) ====================
    public ResponseEntity<AuthResponse> resendOtp(String email) {
        // Check if user exists
        User user = userRepository.findByStudentEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        // Generate and send new OTP
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

        String email = jwtUtils. getEmailFromToken(token);
        User user = userRepository. findByStudentEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        return ResponseEntity. ok(mapToUserResponse(user));
    }

    // ==================== FORGOT PASSWORD ====================
    /**
     * Step 1: Request password reset - sends OTP to email
     */
    @Transactional
    public ResponseEntity<AuthResponse> forgotPassword(String email) {
        // Check if user exists
        User user = userRepository.findByStudentEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        // Check if user registered with Google (no password to reset)
        if (user.getAuthProvider() == AuthProvider.GOOGLE && user.getPassword() == null) {
            throw new BadRequestException("This account uses Google Sign-In. Password reset is not available.");
        }

        // Check if account is active
        if (!user.isActive()) {
            throw new BadRequestException("Account is deactivated. Please contact admin.");
        }

        // Generate and save OTP for password reset
        String otp = generateOtp();
        saveOtp(email, otp, OtpPurpose. PASSWORD_RESET);

        // Send password reset email
        emailService.sendPasswordResetOtpEmail(email, user. getStudentName(), otp);

        return ResponseEntity. ok(new AuthResponse(
                true,
                "Password reset OTP sent to your email.  Please check your inbox.",
                null,
                null
        ));
    }

    // ==================== VERIFY RESET OTP ====================
    /**
     * Step 2: Verify the reset OTP
     */
    @Transactional
    public ResponseEntity<AuthResponse> verifyResetOtp(String email, String otp) {
        // Find the OTP record for password reset
        EmailOtp otpRecord = emailOtpRepository.findByEmailAndOtpAndPurpose(email, otp, OtpPurpose.PASSWORD_RESET)
                .orElseThrow(() -> new BadRequestException("Invalid OTP"));

        // Check if OTP is expired
        if (isOtpExpired(otpRecord)) {
            emailOtpRepository.delete(otpRecord);
            throw new BadRequestException("OTP has expired. Please request a new one.");
        }

        // Mark OTP as verified (but don't delete yet - needed for password reset)
        otpRecord.setVerified(true);
        emailOtpRepository.save(otpRecord);

        return ResponseEntity.ok(new AuthResponse(
                true,
                "OTP verified successfully.  You can now reset your password.",
                null,
                null
        ));
    }

    // ==================== RESET PASSWORD ====================
    /**
     * Step 3: Reset password with verified OTP
     */
    @Transactional
    public ResponseEntity<AuthResponse> resetPassword(ResetPasswordRequest request) {
        // Validate passwords match
        if (! request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        // Find and validate OTP
        EmailOtp otpRecord = emailOtpRepository.findByEmailAndOtpAndPurpose(
                        request.getStudentEmail(),
                        request. getOtp(),
                        OtpPurpose.PASSWORD_RESET)
                .orElseThrow(() -> new BadRequestException("Invalid or expired OTP.  Please request a new one."));

        // Check if OTP was verified
        if (!otpRecord.isVerified()) {
            throw new BadRequestException("OTP not verified. Please verify OTP first.");
        }

        // Check if OTP is expired
        if (isOtpExpired(otpRecord)) {
            emailOtpRepository. delete(otpRecord);
            throw new BadRequestException("OTP has expired. Please request a new one.");
        }

        // Find user
        User user = userRepository.findByStudentEmail(request. getStudentEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getStudentEmail()));

        // Update password
        user. setPassword(passwordEncoder.encode(request. getNewPassword()));
        userRepository.save(user);

        // Delete the used OTP
        emailOtpRepository.delete(otpRecord);

        // Send confirmation email
        emailService.sendPasswordResetSuccessEmail(user.getStudentEmail(), user.getStudentName());

        return ResponseEntity.ok(new AuthResponse(
                true,
                "Password reset successful! You can now login with your new password.",
                null,
                null
        ));
    }

    // ==================== RESEND RESET OTP ====================
    /**
     * Resend password reset OTP
     */
    @Transactional
    public ResponseEntity<AuthResponse> resendResetOtp(String email) {
        // Check if user exists
        User user = userRepository.findByStudentEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        // Check if user registered with Google
        if (user. getAuthProvider() == AuthProvider.GOOGLE && user.getPassword() == null) {
            throw new BadRequestException("This account uses Google Sign-In. Password reset is not available.");
        }

        // Generate and save new OTP
        String otp = generateOtp();
        saveOtp(email, otp, OtpPurpose.PASSWORD_RESET);

        // Send email
        emailService. sendPasswordResetOtpEmail(email, user.getStudentName(), otp);

        return ResponseEntity.ok(new AuthResponse(
                true,
                "Password reset OTP resent successfully! ",
                null,
                null
        ));
    }

    // ==================== HELPER METHODS ====================

    private String generateOtp() {
        return String. format("%06d", new Random().nextInt(999999));
    }

    private void saveOtp(String email, String otp, OtpPurpose purpose) {
        // Delete existing OTP for this email and purpose
        emailOtpRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, purpose)
                .ifPresent(emailOtpRepository:: delete);

        // Save new OTP
        EmailOtp otpEntity = new EmailOtp();
        otpEntity.setEmail(email);
        otpEntity. setOtp(otp);
        otpEntity.setPurpose(purpose);
        otpEntity. setCreatedAt(new Timestamp(System.currentTimeMillis()));
        otpEntity.setExpiresAt(new Timestamp(System. currentTimeMillis() + OTP_EXPIRY_MS));
        otpEntity.setVerified(false);
        emailOtpRepository.save(otpEntity);
    }

    // Backward compatible method (defaults to EMAIL_VERIFICATION)
    private void saveOtp(String email, String otp) {
        saveOtp(email, otp, OtpPurpose. EMAIL_VERIFICATION);
    }

    private boolean isOtpExpired(EmailOtp otpRecord) {
        if (otpRecord. getExpiresAt() != null) {
            return new Timestamp(System.currentTimeMillis()).after(otpRecord. getExpiresAt());
        }
        // Fallback:  check using createdAt
        long timeDiff = System.currentTimeMillis() - otpRecord.getCreatedAt().getTime();
        return timeDiff > OTP_EXPIRY_MS;
    }

    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setStudentId(user.getStudentId());
        response.setStudentName(user.getStudentName());
        response.setStudentEmail(user.getStudentEmail());
        response.setCollageId(user.getCollageId());
        response.setDepartment(user. getDepartment());
        response.setYear(user.getYear());
        response.setGender(user.getGender());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());
        response.setProfilePicture(user.getProfilePicture());
        response.setEmailVerified(user.isEmailVerified());
        return response;
    }
}