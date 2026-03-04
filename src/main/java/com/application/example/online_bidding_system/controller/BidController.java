package com.application.example.online_bidding_system.controller;

import com.application.example.online_bidding_system. dto.request.BidRequest;
import com.application.example.online_bidding_system. dto.response.BidHistoryDto;
import com.application.example.online_bidding_system.dto. response.BidResponse;
import com. application.example.online_bidding_system.dto.response. BidderInfo;
import com.application.example.online_bidding_system. entity.User;
import com. application.example.online_bidding_system.exception.UnauthorizedException;
import com.application. example.online_bidding_system.service.BidService;
import jakarta.validation.Valid;
import org.springframework.beans.factory. annotation.Autowired;
import org. springframework.http.ResponseEntity;
import org.springframework.security. access.prepost. PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java. util.List;
import java.util. Map;

@RestController
@RequestMapping("/api/bids")
@CrossOrigin(origins = "http://localhost:4200")
public class BidController {

    @Autowired
    private BidService bidService;

    /**
     * Place a bid (Only BIDDER role can place bids)
     */
    @PostMapping("/place")
    @PreAuthorize("hasAuthority('BIDDER') or hasAuthority('ADMIN')")
    public ResponseEntity<BidResponse> placeBid(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody BidRequest request) {

        if (user == null) {
            throw new UnauthorizedException("You must be logged in to place a bid");
        }

        // Set bidder ID from authenticated user
        request. setBidderId(user.getStudentId());

        return bidService.placeBid(request);
    }

    /**
     * Get all bids for a stall
     */
    @GetMapping("/stall/{stallId}")
    public ResponseEntity<List<BidResponse>> getBidsByStall(@PathVariable Long stallId) {
        List<BidResponse> bids = bidService. getAllBids(stallId);
        return ResponseEntity.ok(bids);
    }

    /**
     * Get all bidders for a stall
     */
    @GetMapping("/stall/{stallId}/bidders")
    public ResponseEntity<List<BidderInfo>> getBiddersForStall(@PathVariable Long stallId) {
        List<BidderInfo> bidders = bidService.getBiddersForStall(stallId);
        return ResponseEntity.ok(bidders);
    }

    /**
     * Get bid history for a stall
     */
    @GetMapping("/stall/{stallId}/history")
    public ResponseEntity<List<BidHistoryDto>> getBidHistory(@PathVariable Long stallId) {
        List<BidHistoryDto> history = bidService.getBidHistory(stallId);
        return ResponseEntity.ok(history);
    }

    /**
     * Get the highest bid for a stall
     */
    @GetMapping("/stall/{stallId}/highest")
    public ResponseEntity<BidResponse> getHighestBid(@PathVariable Long stallId) {
        BidResponse highestBid = bidService.getHighestBid(stallId);
        return ResponseEntity.ok(highestBid);
    }

    /**
     * Declare winner for a stall (Admin only)
     */
    @PostMapping("/stall/{stallId}/declare-winner")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BidResponse> declareWinner(@PathVariable Long stallId) {
        return bidService.declareWinner(stallId);
    }

    /**
     * Get my bids (current user's bids)
     */
    @GetMapping("/my-bids")
    public ResponseEntity<List<BidResponse>> getMyBids(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new UnauthorizedException("You must be logged in to view your bids");
        }

        List<BidResponse> bids = bidService.getAllBidsByUser(user. getStudentId());
        return ResponseEntity.ok(bids);
    }

    /**
     * Get viewer count for a stall
     */
    @GetMapping("/stall/{stallId}/viewers")
    public ResponseEntity<Map<String, Integer>> getViewerCount(@PathVariable Long stallId) {
        int count = bidService.getViewerCount(stallId);
        Map<String, Integer> response = new HashMap<>();
        response.put("viewerCount", count);
        return ResponseEntity.ok(response);
    }
}