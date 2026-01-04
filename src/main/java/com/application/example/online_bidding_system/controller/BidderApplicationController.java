package com.application. example.online_bidding_system.controller;

import com. application.example.online_bidding_system.dto.request. BidderApplicationRequest;
import com. application.example.online_bidding_system.dto.response. BidderApplicationResponse;
import com. application.example.online_bidding_system.entity.Status;
import com.application.example.online_bidding_system. service.BidderApplicationService;
import org.springframework.beans. factory.annotation. Qualifier;
import org. springframework.http.ResponseEntity;
import org.springframework.web. bind.annotation.*;

import java.util. HashMap;
import java. util.List;
import java.util. Map;

@RestController
@RequestMapping("/api/bidder-applications")
@CrossOrigin(origins = "http://localhost:4200")
public class BidderApplicationController {

    private final BidderApplicationService applicationService;

    public BidderApplicationController(
            @Qualifier("bidderApplicationServiceImpl") BidderApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /**
     * Student applies to become a bidder
     */
    @PostMapping("/apply")
    public ResponseEntity<BidderApplicationResponse> applyAsBidder(
            @RequestBody BidderApplicationRequest request) {
        System.out.println("Received application:  " + request);
        return applicationService. applyAsBidder(request);
    }

    /**
     * Admin approves an application
     */
    @PutMapping("/{applicationId}/approve")
    public ResponseEntity<String> approve(@PathVariable Long applicationId) {
        return applicationService. approveApplication(applicationId);
    }

    /**
     * Admin rejects an application
     */
    @PutMapping("/{applicationId}/reject")
    public ResponseEntity<String> reject(@PathVariable Long applicationId) {
        return applicationService. rejectApplication(applicationId);
    }

    /**
     * Admin rejects with reason
     */
    @PutMapping("/{applicationId}/reject-with-reason")
    public ResponseEntity<String> rejectWithReason(
            @PathVariable Long applicationId,
            @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "No reason provided");
        return applicationService.updateApplicationStatus(applicationId, Status.REJECTED);
    }

    /**
     * Get applications by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<BidderApplicationResponse>> getByStatus(@PathVariable Status status) {
        List<BidderApplicationResponse> applications = applicationService.getApplicationsByStatus(status);
        return ResponseEntity. ok(applications);
    }

    /**
     * Check if student already applied
     */
    @GetMapping("/has-applied/{email}")
    public ResponseEntity<Map<String, Object>> hasAlreadyApplied(@PathVariable String email) {
        Status status = applicationService. hasUserAlreadyApplied(email);
        Map<String, Object> response = new HashMap<>();
        response.put("hasApplied", status != Status.NOT_APPLIED);
        response.put("status", status.name());
        return ResponseEntity.ok(response);
    }

    /**
     * Get application status by email
     */
    @GetMapping("/status-by-email/{email}")
    public ResponseEntity<Map<String, String>> getStatusByEmail(@PathVariable String email) {
        String status = applicationService. getApplicationStatusByEmail(email);
        Map<String, String> response = new HashMap<>();
        response.put("status", status);
        return ResponseEntity. ok(response);
    }

    /**
     * Get all applications (all bidders)
     */
    @GetMapping("/all")
    public ResponseEntity<List<BidderApplicationResponse>> getAllBidders() {
        List<BidderApplicationResponse> applications = applicationService.getAllUsers();
        return ResponseEntity.ok(applications);
    }

    /**
     * Get all applications (alternative endpoint)
     */
    @GetMapping
    public ResponseEntity<List<BidderApplicationResponse>> getAllApplications() {
        List<BidderApplicationResponse> applications = applicationService.getAllApplications();
        return ResponseEntity.ok(applications);
    }

    /**
     * Get pending applications count
     */
    @GetMapping("/pending-count")
    public ResponseEntity<Map<String, Long>> getPendingCount() {
        List<BidderApplicationResponse> pending = applicationService.getApplicationsByStatus(Status. PENDING);
        Map<String, Long> response = new HashMap<>();
        response.put("count", (long) pending.size());
        return ResponseEntity.ok(response);
    }
}