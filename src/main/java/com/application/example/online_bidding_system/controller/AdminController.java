package com.application.example.online_bidding_system.controller;

import com.application.example.online_bidding_system.dto.request.ApplicationSettingsRequest;
import com.application. example.online_bidding_system.dto.response.UserResponse;
import com.application. example.online_bidding_system.entity.*;
import com.application.example.online_bidding_system. repository.*;
import com. application.example.online_bidding_system.service. Emailservice;
import org.springframework.beans.factory. annotation.Autowired;
import org. springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org. springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util. HashMap;
import java. util.List;
import java.util. Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:4200")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BidderApplicationRepository bidderApplicationRepository;

    @Autowired
    private StallRepository stallRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private ApplicationSettingsRepository applicationSettingsRepository;

    @Autowired
    private Emailservice emailService;

    /**
     * Get dashboard statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalUsers", userRepository.countByRole(Role.USER) + userRepository.countByRole(Role. BIDDER));
        stats.put("totalBidders", userRepository.countByRole(Role.BIDDER));
        stats.put("totalStalls", stallRepository. count());
        stats.put("activeStalls", stallRepository.countByStatus(StallStatus.ACTIVE));
        stats.put("pendingApplications", bidderApplicationRepository.countByStatus(Status.PENDING));
        stats.put("approvedApplications", bidderApplicationRepository.countByStatus(Status. APPROVED));
        stats.put("rejectedApplications", bidderApplicationRepository.countByStatus(Status. REJECTED));
        stats.put("totalBids", bidRepository.count());

        return ResponseEntity. ok(stats);
    }

    /**
     * Get all pending bidder applications
     */
    @GetMapping("/applications/pending")
    public ResponseEntity<List<Map<String, Object>>> getPendingApplications() {
        List<BidderApplication> applications = bidderApplicationRepository.findByStatus(Status.PENDING);

        List<Map<String, Object>> response = applications.stream().map(app -> {
            Map<String, Object> map = new HashMap<>();
            map.put("applicationId", app.getApplicationId());
            map.put("studentId", app.getUser().getStudentId());
            map.put("studentName", app.getUser().getStudentName());
            map.put("studentEmail", app.getUser().getStudentEmail());
            map.put("collageId", app.getUser().getCollageId());
            map.put("department", app.getUser().getDepartment());
            map.put("year", app. getUser().getYear());
            map.put("phoneNumber", app.getPhoneNumber());
            map.put("reason", app.getReason());
            map.put("preferredStallCategory", app.getPreferredStallCategory());
            map.put("appliedAt", app.getAppliedAt());
            map.put("status", app.getStatus().name());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Get all applications (all statuses)
     */
    @GetMapping("/applications")
    public ResponseEntity<List<Map<String, Object>>> getAllApplications() {
        List<BidderApplication> applications = bidderApplicationRepository.findAll();

        List<Map<String, Object>> response = applications. stream().map(app -> {
            Map<String, Object> map = new HashMap<>();
            map.put("applicationId", app.getApplicationId());
            map.put("studentId", app.getUser().getStudentId());
            map.put("studentName", app. getUser().getStudentName());
            map.put("studentEmail", app.getUser().getStudentEmail());
            map.put("collageId", app.getUser().getCollageId());
            map.put("department", app.getUser().getDepartment());
            map.put("phoneNumber", app.getPhoneNumber());
            map.put("reason", app.getReason());
            map.put("preferredStallCategory", app.getPreferredStallCategory());
            map.put("appliedAt", app.getAppliedAt());
            map.put("status", app.getStatus().name());
            map.put("reviewedAt", app. getReviewedAt());
            map.put("rejectionReason", app. getRejectionReason());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Get application by ID
     */
    @GetMapping("/applications/{applicationId}")
    public ResponseEntity<? > getApplicationById(@PathVariable Long applicationId) {
        BidderApplication application = bidderApplicationRepository. findById(applicationId)
                .orElse(null);

        if (application == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Application not found"
            ));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("applicationId", application. getApplicationId());
        response.put("studentId", application. getUser().getStudentId());
        response.put("studentName", application.getUser().getStudentName());
        response.put("studentEmail", application.getUser().getStudentEmail());
        response.put("collageId", application.getUser().getCollageId());
        response.put("department", application.getUser().getDepartment());
        response.put("year", application.getUser().getYear());
        response.put("phoneNumber", application. getPhoneNumber());
        response.put("reason", application. getReason());
        response.put("preferredStallCategory", application.getPreferredStallCategory());
        response.put("appliedAt", application.getAppliedAt());
        response.put("status", application.getStatus().name());
        response.put("reviewedAt", application. getReviewedAt());
        response.put("rejectionReason", application.getRejectionReason());

        return ResponseEntity.ok(response);
    }

    /**
     * Approve a bidder application
     */
    @PostMapping("/applications/{applicationId}/approve")
    public ResponseEntity<Map<String, Object>> approveApplication(
            @PathVariable Long applicationId,
            @AuthenticationPrincipal User admin) {

        BidderApplication application = bidderApplicationRepository.findById(applicationId)
                .orElse(null);

        if (application == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Application not found"
            ));
        }

        if (application.getStatus() != Status.PENDING) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Application already processed"
            ));
        }

        // Update application status
        application.setStatus(Status.APPROVED);
        application. setReviewedAt(new Timestamp(System. currentTimeMillis()));
        application.setReviewedBy(admin);
        bidderApplicationRepository.save(application);

        // Update user role to BIDDER
        User user = application.getUser();
        user.setRole(Role.BIDDER);
        userRepository.save(user);

        // Send approval email
        try {
            emailService.sendApplicationApprovedEmail(
                    user.getStudentEmail(),
                    user.getStudentName()
            );
        } catch (Exception e) {
            System.err.println("Failed to send approval email: " + e. getMessage());
        }

        return ResponseEntity.ok(Map. of(
                "success", true,
                "message", "Application approved successfully.  User is now a BIDDER."
        ));
    }

    /**
     * Reject a bidder application
     */
    @PostMapping("/applications/{applicationId}/reject")
    public ResponseEntity<Map<String, Object>> rejectApplication(
            @PathVariable Long applicationId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User admin) {

        BidderApplication application = bidderApplicationRepository.findById(applicationId)
                .orElse(null);

        if (application == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Application not found"
            ));
        }

        if (application.getStatus() != Status.PENDING) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Application already processed"
            ));
        }

        String reason = body.getOrDefault("reason", "No reason provided");

        // Update application status
        application.setStatus(Status. REJECTED);
        application.setReviewedAt(new Timestamp(System. currentTimeMillis()));
        application.setReviewedBy(admin);
        application.setRejectionReason(reason);
        bidderApplicationRepository.save(application);

        // Send rejection email
        User user = application.getUser();
        try {
            emailService.sendApplicationRejectedEmail(
                    user.getStudentEmail(),
                    user.getStudentName(),
                    reason
            );
        } catch (Exception e) {
            System.err. println("Failed to send rejection email: " + e.getMessage());
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Application rejected."
        ));
    }

    /**
     * Get all users (excluding admins)
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<User> users = userRepository. findAll();

        List<UserResponse> responses = users.stream()
                .filter(u -> u.getRole() != Role.ADMIN)
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Get all bidders
     */
    @GetMapping("/bidders")
    public ResponseEntity<List<UserResponse>> getAllBidders() {
        List<User> bidders = userRepository. findByRole(Role. BIDDER);

        List<UserResponse> responses = bidders. stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Get user by ID
     */
    @GetMapping("/users/{studentId}")
    public ResponseEntity<?> getUserById(@PathVariable Long studentId) {
        User user = userRepository.findById(studentId).orElse(null);

        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "User not found"
            ));
        }

        return ResponseEntity.ok(mapToUserResponse(user));
    }

    /**
     * Deactivate a user
     */
    @PostMapping("/users/{studentId}/deactivate")
    public ResponseEntity<Map<String, Object>> deactivateUser(@PathVariable Long studentId) {
        User user = userRepository.findById(studentId).orElse(null);

        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "User not found"
            ));
        }

        if (user.getRole() == Role.ADMIN) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Cannot deactivate admin"
            ));
        }

        user.setActive(false);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User deactivated successfully"
        ));
    }

    /**
     * Activate a user
     */
    @PostMapping("/users/{studentId}/activate")
    public ResponseEntity<Map<String, Object>> activateUser(@PathVariable Long studentId) {
        User user = userRepository. findById(studentId).orElse(null);

        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "User not found"
            ));
        }

        user.setActive(true);
        userRepository.save(user);

        return ResponseEntity.ok(Map. of(
                "success", true,
                "message", "User activated successfully"
        ));
    }

    /**
     * Update application settings (application window, bidding window)
     */
    @PostMapping("/settings")
    public ResponseEntity<Map<String, Object>> updateSettings(
            @RequestBody Map<String, Object> settings,
            @AuthenticationPrincipal User admin) {

        ApplicationSettings appSettings = applicationSettingsRepository.findTopByOrderByIdDesc()
                .orElse(new ApplicationSettings());

        if (settings.containsKey("festName")) {
            appSettings.setFestName((String) settings.get("festName"));
        }
        if (settings. containsKey("academicYear")) {
            appSettings.setAcademicYear((String) settings.get("academicYear"));
        }
        if (settings. containsKey("applicationStartTime")) {
            appSettings.setApplicationStartTime(Timestamp.valueOf((String) settings.get("applicationStartTime")));
        }
        if (settings.containsKey("applicationEndTime")) {
            appSettings. setApplicationEndTime(Timestamp.valueOf((String) settings.get("applicationEndTime")));
        }
        if (settings. containsKey("isApplicationOpen")) {
            appSettings.setApplicationOpen((Boolean) settings.get("isApplicationOpen"));
        }
        if (settings.containsKey("biddingStartTime")) {
            appSettings.setBiddingStartTime(Timestamp.valueOf((String) settings.get("biddingStartTime")));
        }
        if (settings. containsKey("biddingEndTime")) {
            appSettings.setBiddingEndTime(Timestamp. valueOf((String) settings.get("biddingEndTime")));
        }
        if (settings.containsKey("isBiddingOpen")) {
            appSettings. setBiddingOpen((Boolean) settings.get("isBiddingOpen"));
        }

        appSettings.setCreatedBy(admin);
        applicationSettingsRepository.save(appSettings);

        return ResponseEntity. ok(Map.of(
                "success", true,
                "message", "Settings updated successfully"
        ));
    }

    /**
     * Get current application settings
     */
    @GetMapping("/settings")
    public ResponseEntity<ApplicationSettings> getSettings() {
        ApplicationSettings settings = applicationSettingsRepository.findTopByOrderByIdDesc()
                .orElse(new ApplicationSettings());
        return ResponseEntity.ok(settings);
    }

    /**
     * Toggle application window open/close
     */
    @PostMapping("/settings/toggle-application")
    public ResponseEntity<Map<String, Object>> toggleApplicationWindow(
            @AuthenticationPrincipal User admin) {

        ApplicationSettings appSettings = applicationSettingsRepository.findTopByOrderByIdDesc()
                .orElse(new ApplicationSettings());

        boolean newStatus = ! appSettings.isApplicationOpen();
        appSettings.setApplicationOpen(newStatus);
        appSettings.setCreatedBy(admin);
        applicationSettingsRepository.save(appSettings);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "isApplicationOpen", newStatus,
                "message", newStatus ? "Application window opened" : "Application window closed"
        ));
    }

    /**
     * Toggle bidding window open/close
     */
    @PostMapping("/settings/toggle-bidding")
    public ResponseEntity<Map<String, Object>> toggleBiddingWindow(
            @AuthenticationPrincipal User admin) {

        ApplicationSettings appSettings = applicationSettingsRepository. findTopByOrderByIdDesc()
                .orElse(new ApplicationSettings());

        boolean newStatus = !appSettings.isBiddingOpen();
        appSettings. setBiddingOpen(newStatus);
        appSettings.setCreatedBy(admin);
        applicationSettingsRepository.save(appSettings);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "isBiddingOpen", newStatus,
                "message", newStatus ? "Bidding window opened" : "Bidding window closed"
        ));
    }

    /**
     * Send email to all students about bidding started
     */
    @PostMapping("/notify/bidding-started")
    public ResponseEntity<Map<String, Object>> notifyBiddingStarted(@RequestBody Map<String, String> body) {
        String stallName = body.getOrDefault("stallName", "Stalls");

        List<User> allUsers = userRepository.findAll();
        int sentCount = 0;

        for (User user : allUsers) {
            if (user.getRole() != Role.ADMIN && user.getStudentEmail() != null) {
                try {
                    emailService.sendBiddingStartedEmail(
                            user.getStudentEmail(),
                            user. getStudentName(),
                            stallName
                    );
                    sentCount++;
                } catch (Exception e) {
                    System.err.println("Failed to send email to:  " + user.getStudentEmail());
                }
            }
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Notification sent to " + sentCount + " users"
        ));
    }

    /**
     * Send email to all approved bidders
     */
    @PostMapping("/notify/bidders")
    public ResponseEntity<Map<String, Object>> notifyBidders(@RequestBody Map<String, String> body) {
        String subject = body.getOrDefault("subject", "Important Update");
        String message = body.getOrDefault("message", "");

        List<User> bidders = userRepository. findByRole(Role.BIDDER);
        int sentCount = 0;

        for (User bidder : bidders) {
            if (bidder.getStudentEmail() != null) {
                try {
                    emailService.sendSimpleMail(
                            new com.application.example.online_bidding_system.dto.email.EmailDetails(
                                    bidder.getStudentEmail(),
                                    subject,
                                    "Dear " + bidder. getStudentName() + ",\n\n" + message + "\n\nRegards,\nCollege Bidding System"
                            )
                    );
                    sentCount++;
                } catch (Exception e) {
                    System.err. println("Failed to send email to: " + bidder.getStudentEmail());
                }
            }
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Notification sent to " + sentCount + " bidders"
        ));
    }

    /**
     * Get counts for dashboard
     */
    @GetMapping("/counts")
    public ResponseEntity<Map<String, Long>> getCounts() {
        Map<String, Long> counts = new HashMap<>();

        counts.put("totalUsers", userRepository.count() - userRepository.countByRole(Role. ADMIN));
        counts.put("totalBidders", userRepository.countByRole(Role.BIDDER));
        counts.put("totalStalls", stallRepository.count());
        counts.put("activeStalls", stallRepository.countByStatus(StallStatus. ACTIVE));
        counts.put("closedStalls", stallRepository.countByStatus(StallStatus. CLOSED));
        counts.put("pendingApplications", bidderApplicationRepository.countByStatus(Status. PENDING));
        counts.put("approvedApplications", bidderApplicationRepository.countByStatus(Status. APPROVED));
        counts.put("rejectedApplications", bidderApplicationRepository.countByStatus(Status. REJECTED));
        counts.put("totalBids", bidRepository.count());

        return ResponseEntity. ok(counts);
    }

    /**
     * Map User entity to UserResponse DTO
     */
    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setStudentId(user. getStudentId());
        response.setStudentName(user. getStudentName());
        response.setStudentEmail(user.getStudentEmail());
        response.setCollageId(user.getCollageId());
        response.setDepartment(user.getDepartment());
        response.setYear(user.getYear());
        response.setGender(user.getGender());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());
        response.setProfilePicture(user.getProfilePicture());
        response.setEmailVerified(user. isEmailVerified());
        return response;
    }
    // In AdminController.java

    @PostMapping("/settings/update")
    public ResponseEntity<Map<String, Object>> updateSettings(
            @RequestBody ApplicationSettingsRequest request,
            @AuthenticationPrincipal User admin) {

        ApplicationSettings appSettings = applicationSettingsRepository.findTopByOrderByIdDesc()
                .orElse(new ApplicationSettings());

        if (request.getFestName() != null) {
            appSettings.setFestName(request.getFestName());
        }
        if (request.getAcademicYear() != null) {
            appSettings.setAcademicYear(request.getAcademicYear());
        }
        if (request.getApplicationStartTime() != null) {
            appSettings.setApplicationStartTime(
                    Timestamp.valueOf(request.getApplicationStartTime())
            );
        }
        if (request.getApplicationEndTime() != null) {
            appSettings.setApplicationEndTime(
                    Timestamp.valueOf(request.getApplicationEndTime())
            );
        }
        if (request.getIsApplicationOpen() != null) {
            appSettings.setApplicationOpen(request.getIsApplicationOpen());
        }
        if (request.getBiddingStartTime() != null) {
            appSettings.setBiddingStartTime(
                    Timestamp.valueOf(request.getBiddingStartTime())
            );
        }
        if (request.getBiddingEndTime() != null) {
            appSettings.setBiddingEndTime(
                    Timestamp.valueOf(request.getBiddingEndTime())
            );
        }
        if (request.getIsBiddingOpen() != null) {
            appSettings. setBiddingOpen(request.getIsBiddingOpen());
        }

        appSettings.setCreatedBy(admin);
        applicationSettingsRepository.save(appSettings);

        return ResponseEntity. ok(Map.of(
                "success", true,
                "message", "Settings updated successfully"
        ));
    }
}