package com.application.example.online_bidding_system. controller;

import com.application.example. online_bidding_system.dto.response.BidResponse;
import com. application.example.online_bidding_system.dto.response. BiddingResultResponse;
import com.application.example.online_bidding_system. dto.response.UserResponse;
import com.application.example. online_bidding_system.entity.*;
import com.application.example.online_bidding_system. exception.ResourceNotFoundException;
import com.application.example. online_bidding_system.exception.UnauthorizedException;
import com.application. example.online_bidding_system.repository.BidRepository;
import com.application.example.online_bidding_system. repository.BidderApplicationRepository;
import com.application.example. online_bidding_system.repository.BiddingResultRepository;
import com.application.example.online_bidding_system. repository.UserRepository;
import org.springframework.beans.factory. annotation.Autowired;
import org. springframework.http.ResponseEntity;
import org.springframework.security. core.annotation.AuthenticationPrincipal;
import org. springframework.web.bind.annotation.*;

import java.util.HashMap;
import java. util.List;
import java.util. Map;
import java.util.stream. Collectors;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "http://localhost:4200")
public class ProfileController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private BiddingResultRepository biddingResultRepository;

    @Autowired
    private BidderApplicationRepository bidderApplicationRepository;

    /**
     * Get current user's profile
     */
    @GetMapping
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new UnauthorizedException("You must be logged in to view your profile");
        }
        return ResponseEntity.ok(mapToUserResponse(user));
    }

    /**
     * Update profile
     */
    @PutMapping("/update")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal User currentUser,
            @RequestBody Map<String, Object> updates) {

        if (currentUser == null) {
            throw new UnauthorizedException("You must be logged in to update your profile");
        }

        User user = userRepository. findById(currentUser.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser. getStudentId()));

        if (updates.containsKey("studentName")) {
            user.setStudentName((String) updates.get("studentName"));
        }
        if (updates. containsKey("phone")) {
            user.setPhone((String) updates.get("phone"));
        }
        if (updates.containsKey("address")) {
            user.setAddress((String) updates.get("address"));
        }
        if (updates. containsKey("department")) {
            user.setDepartment((String) updates.get("department"));
        }
        if (updates.containsKey("year")) {
            user.setYear((Integer) updates.get("year"));
        }
        if (updates. containsKey("gender")) {
            user.setGender((String) updates.get("gender"));
        }

        userRepository.save(user);

        return ResponseEntity.ok(mapToUserResponse(user));
    }

    /**
     * Get user's bid history
     */
    @GetMapping("/my-bids")
    public ResponseEntity<List<BidResponse>> getMyBids(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new UnauthorizedException("You must be logged in to view your bids");
        }

        List<Bid> bids = bidRepository.findByBidderOrderByBidTimeDesc(user);

        List<BidResponse> bidResponses = bids.stream().map(bid -> {
            BidResponse response = new BidResponse();
            response.setBidId(bid.getBidId());
            response. setStallId(bid.getStall().getStallId());
            response. setStallName(bid.getStall().getStallName());
            response. setBiddedPrice(bid.getBiddedPrice());
            response. setBidTime(bid.getBidTime());
            response.setStudentName(bid.getBidder().getStudentName());
            return response;
        }).collect(Collectors. toList());

        return ResponseEntity.ok(bidResponses);
    }

    /**
     * Get stalls won by user
     */
    @GetMapping("/my-wins")
    public ResponseEntity<List<BiddingResultResponse>> getMyWins(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new UnauthorizedException("You must be logged in to view your wins");
        }

        List<BiddingResult> results = biddingResultRepository.findByWinner(user);

        List<BiddingResultResponse> responses = results.stream().map(result -> {
            BiddingResultResponse response = new BiddingResultResponse();
            response. setResultId(result.getResultId());
            response.setStallId(result.getStall().getStallId());
            response.setStallName(result.getStall().getStallName());
            response.setWinningPrice(result.getWinningPrice());
            response.setWinnerId(result.getWinner().getStudentId());
            response.setWinnerName(result. getWinner().getStudentName());
            response.setWinnerEmail(result.getWinner().getStudentEmail());
            response.setResultTime(result. getResultTime());
            return response;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Get user's bidder application status
     */
    @GetMapping("/application-status")
    public ResponseEntity<Map<String, Object>> getApplicationStatus(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new UnauthorizedException("You must be logged in to view your application status");
        }

        Map<String, Object> response = new HashMap<>();

        var applicationOpt = bidderApplicationRepository.findByUser_StudentId(user. getStudentId());

        if (applicationOpt.isPresent()) {
            BidderApplication application = applicationOpt. get();
            response.put("hasApplied", true);
            response.put("status", application.getStatus().name());
            response.put("appliedAt", application. getAppliedAt());
            response. put("reviewedAt", application. getReviewedAt());
            response.put("rejectionReason", application.getRejectionReason());
            response.put("reason", application.getReason());
            response.put("preferredStallCategory", application.getPreferredStallCategory());
        } else {
            response.put("hasApplied", false);
            response. put("status", null);
        }

        response.put("currentRole", user.getRole().name());
        response.put("isBidder", user.getRole() == Role.BIDDER);

        return ResponseEntity. ok(response);
    }

    /**
     * Get profile statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getProfileStats(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new UnauthorizedException("You must be logged in to view your stats");
        }

        Map<String, Object> stats = new HashMap<>();

        // Total bids placed
        long totalBids = bidRepository.countByBidder(user);
        stats.put("totalBids", totalBids);

        // Stalls won
        long stallsWon = biddingResultRepository. countByWinner(user);
        stats.put("stallsWon", stallsWon);

        // Current role
        stats.put("role", user.getRole().name());

        // Is bidder
        stats.put("isBidder", user. getRole() == Role.BIDDER);

        // Is admin
        stats.put("isAdmin", user.getRole() == Role.ADMIN);

        return ResponseEntity.ok(stats);
    }

    /**
     * Check if user is a bidder
     */
    @GetMapping("/is-bidder")
    public ResponseEntity<Map<String, Boolean>> isBidder(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new UnauthorizedException("You must be logged in");
        }

        Map<String, Boolean> response = new HashMap<>();
        response.put("isBidder", user.getRole() == Role.BIDDER);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user role
     */
    @GetMapping("/role")
    public ResponseEntity<Map<String, String>> getRole(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new UnauthorizedException("You must be logged in");
        }

        Map<String, String> response = new HashMap<>();
        response.put("role", user.getRole().name());
        return ResponseEntity.ok(response);
    }

    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setStudentId(user. getStudentId());
        response.setStudentName(user.getStudentName());
        response.setStudentEmail(user.getStudentEmail());
        response.setCollageId(user.getCollageId());
        response.setDepartment(user. getDepartment());
        response.setYear(user.getYear());
        response.setGender(user.getGender());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());
        response.setProfilePicture(user.getProfilePicture());
        response.setEmailVerified(user. isEmailVerified());
        return response;
    }
}