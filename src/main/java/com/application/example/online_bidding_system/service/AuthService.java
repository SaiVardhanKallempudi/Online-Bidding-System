package com.application.example.online_bidding_system. service;

import com.application.example. online_bidding_system.dto.request.LoginRequest;
import com.application.example. online_bidding_system.dto.request.SignUpRequest;
import com.application.example.online_bidding_system. dto.response.AuthResponse;
import com.application.example. online_bidding_system.dto.response.UserResponse;
import com. application.example.online_bidding_system.entity.*;
import com.application.example.online_bidding_system. exception.BadRequestException;
import com.application.example.online_bidding_system.exception. ResourceNotFoundException;
import com.application.example.online_bidding_system.exception. UnauthorizedException;
import com.application.example.online_bidding_system. repository.EmailOtpRepository;
import com.application.example. online_bidding_system.repository.UserRepository;
import com.application.example. online_bidding_system.security.JwtUtils;
import org. springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security. crypto.password.PasswordEncoder;
import org.springframework.stereotype. Service;
import org.springframework. transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util. Optional;
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

    @Transactional
    public ResponseEntity<AuthResponse> signUp(SignUpRequest request) {
        // Validate email not already registered
        if (userRepository.existsByStudentEmail(request. getStudentEmail())) {
            throw new BadRequestException("Email already registered:  " + request.getStudentEmail());
        }

        // Validate college ID not already registered
        if (request.getCollageId() != null && userRepository.existsByCollageId(request.getCollageId())) {
            throw new BadRequestException("College ID already registered: " + request. getCollageId());
        }

        // Create new user
        User user = new User();
        user.setStudentName(request.getStudentName());
        user.setStudentEmail(request. getStudentEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCollageId(request. getCollageId());
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

        // Generate and send OTP
        String otp = generateOtp();
        saveOtp(request.getStudentEmail(), otp);
        emailService.sendOtpEmail(request.getStudentEmail(), otp);

        return ResponseEntity.ok(new AuthResponse(
                true,
                "Registration successful!  Please verify your email with OTP.",
                null,
                mapToUserResponse(user)
        ));
    }

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
        if (! passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        // Check email verification (except for admin)
        if (user.getRole() != Role.ADMIN && ! user.isEmailVerified()) {
            String otp = generateOtp();
            saveOtp(user.getStudentEmail(), otp);
            emailService.sendOtpEmail(user.getStudentEmail(), otp);
            throw new UnauthorizedException("Email not verified. A new OTP has been sent to your email.");
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

    @Transactional
    public ResponseEntity<AuthResponse> verifyOtp(String email, String otp) {
        // Find OTP record
        EmailOtp otpRecord = emailOtpRepository.findByEmailAndOtp(email, otp)
                .orElseThrow(() -> new BadRequestException("Invalid OTP"));

        // Check if OTP expired (5 minutes)
        long timeDiff = System.currentTimeMillis() - otpRecord.getCreatedAt().getTime();
        if (timeDiff > 5 * 60 * 1000) {
            emailOtpRepository. delete(otpRecord);
            throw new BadRequestException("OTP expired. Please request a new one.");
        }

        // Find and update user
        User user = userRepository. findByStudentEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        user.setEmailVerified(true);
        userRepository.save(user);

        // Delete used OTP
        emailOtpRepository. delete(otpRecord);

        // Generate token
        String token = jwtUtils.generateToken(user);

        return ResponseEntity. ok(new AuthResponse(
                true,
                "Email verified successfully! ",
                token,
                mapToUserResponse(user)
        ));
    }

    public ResponseEntity<AuthResponse> resendOtp(String email) {
        // Check if user exists
        User user = userRepository.findByStudentEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        // Generate and send new OTP
        String otp = generateOtp();
        saveOtp(email, otp);
        emailService.sendOtpEmail(email, otp);

        return ResponseEntity.ok(new AuthResponse(true, "OTP sent successfully to " + email, null, null));
    }

    public ResponseEntity<UserResponse> getCurrentUser(String token) {
        if (!jwtUtils. validateToken(token)) {
            throw new UnauthorizedException("Invalid or expired token");
        }

        String email = jwtUtils. getEmailFromToken(token);
        User user = userRepository.findByStudentEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        return ResponseEntity.ok(mapToUserResponse(user));
    }

    private String generateOtp() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    private void saveOtp(String email, String otp) {
        // Delete existing OTP for this email
        emailOtpRepository.findByEmail(email).ifPresent(emailOtpRepository::delete);

        // Save new OTP
        EmailOtp otpEntity = new EmailOtp();
        otpEntity.setEmail(email);
        otpEntity.setOtp(otp);
        otpEntity.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        otpEntity.setVerified(false);
        emailOtpRepository.save(otpEntity);
    }

    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setStudentId(user.getStudentId());
        response.setStudentName(user. getStudentName());
        response.setStudentEmail(user. getStudentEmail());
        response.setCollageId(user.getCollageId());
        response.setDepartment(user.getDepartment());
        response.setYear(user. getYear());
        response.setGender(user.getGender());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());
        response.setProfilePicture(user.getProfilePicture());
        response.setEmailVerified(user. isEmailVerified());
        return response;
    }
}