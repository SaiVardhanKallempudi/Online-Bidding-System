package com.application. example.online_bidding_system.controller;

import com. application.example.online_bidding_system.dto.request. CreateStallRequest;
import com.application.example.online_bidding_system. dto.response.StallResponse;
import com.application.example.online_bidding_system.entity.StallStatus;
import com.application.example.online_bidding_system.entity. User;
import com.application.example. online_bidding_system.exception.UnauthorizedException;
import com.application. example.online_bidding_system.service.StallService;
import org.springframework.beans. factory.annotation. Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework. security.access.prepost.PreAuthorize;
import org. springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web. multipart.MultipartFile;

import java. io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stalls")
@CrossOrigin(origins = "http://localhost:4200")
public class StallController {

    @Autowired
    private StallService stallService;

    /**
     * Get all stalls
     */
    @GetMapping
    public ResponseEntity<List<StallResponse>> getAllStalls() {
        return ResponseEntity.ok(stallService.getAllStalls());
    }

    /**
     * Get stall by ID
     */
    @GetMapping("/{stallId}")
    public ResponseEntity<StallResponse> getStallById(@PathVariable Long stallId) {
        return stallService.getStallById(stallId);
    }

    /**
     * Get stall by stall number
     */
    @GetMapping("/number/{stallNo}")
    public ResponseEntity<StallResponse> getStallByNumber(@PathVariable int stallNo) {
        return stallService. getStallByNumber(stallNo);
    }

    /**
     * Get stalls by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<StallResponse>> getStallsByStatus(@PathVariable StallStatus status) {
        return ResponseEntity.ok(stallService.getStallsByStatus(status));
    }

    /**
     * Get active stalls
     */
    @GetMapping("/active")
    public ResponseEntity<List<StallResponse>> getActiveStalls() {
        return ResponseEntity. ok(stallService. getActiveStalls());
    }

    /**
     * Get available stalls
     */
    @GetMapping("/available")
    public ResponseEntity<List<StallResponse>> getAvailableStalls() {
        return ResponseEntity. ok(stallService. getAvailableStalls());
    }

    /**
     * Get closed stalls
     */
    @GetMapping("/closed")
    public ResponseEntity<List<StallResponse>> getClosedStalls() {
        return ResponseEntity.ok(stallService.getClosedStalls());
    }

    /**
     * Get stall counts
     */
    @GetMapping("/counts")
    public ResponseEntity<Map<String, Long>> getStallCounts() {
        return ResponseEntity. ok(stallService. getStallCounts());
    }

    /**
     * Create stall (Admin only)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StallResponse> createStall(
            @AuthenticationPrincipal User admin,
            @RequestBody CreateStallRequest request) {

        if (admin == null) {
            throw new UnauthorizedException("You must be logged in as admin");
        }

        return stallService.addStall(request, admin);
    }

    /**
     * Update stall (Admin only)
     */
    @PutMapping("/{stallId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StallResponse> updateStall(
            @PathVariable Long stallId,
            @RequestBody CreateStallRequest request) {

        return stallService.updateStall(stallId, request);
    }

    /**
     * Update stall with image (Admin only)
     */
    @PutMapping(value = "/{stallId}/with-image", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StallResponse> updateStallWithImage(
            @PathVariable Long stallId,
            @RequestPart("stall") CreateStallRequest request,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) throws IOException {

        return stallService. updateStall(stallId, request, imageFile);
    }

    /**
     * Delete stall (Admin only)
     */
    @DeleteMapping("/{stallId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteStall(@PathVariable Long stallId) {
        stallService.deleteByStallId(stallId);

        return ResponseEntity. ok(Map.of(
                "success", true,
                "message", "Stall deleted successfully"
        ));
    }

    /**
     * Start bidding (Admin only)
     */
    @PostMapping("/{stallId}/start-bidding")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> startBidding(@PathVariable Long stallId) {
        return stallService.startBidding(stallId);
    }

    /**
     * Stop bidding (Admin only)
     */
    @PostMapping("/{stallId}/stop-bidding")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> stopBidding(@PathVariable Long stallId) {
        return stallService. stopBidding(stallId);
    }
}