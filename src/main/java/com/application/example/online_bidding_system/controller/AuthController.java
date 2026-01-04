package com.application. example.online_bidding_system.controller;

import com. application.example.online_bidding_system.dto.request. LoginRequest;
import com.application.example.online_bidding_system.dto. request.SignUpRequest;
import com. application.example.online_bidding_system.dto.response. AuthResponse;
import com.application.example.online_bidding_system.dto. response.UserResponse;
import com.application.example.online_bidding_system. service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory. annotation.Autowired;
import org. springframework.http.ResponseEntity;
import org.springframework.web. bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        return authService.signUp(request);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@RequestParam String email, @RequestParam String otp) {
        return authService. verifyOtp(email, otp);
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<AuthResponse> resendOtp(@RequestParam String email) {
        return authService.resendOtp(email);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@RequestHeader("Authorization") String token) {
        return authService.getCurrentUser(token. replace("Bearer ", ""));
    }
}