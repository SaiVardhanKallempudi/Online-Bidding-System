package com. application.example.online_bidding_system.service;

import com.application.example.online_bidding_system.dto.request.BidRequest;
import com.application.example. online_bidding_system.dto.response.BidHistoryDto;
import com. application.example.online_bidding_system.dto.response. BidResponse;
import com.application. example.online_bidding_system.dto.response.BidderInfo;
import com. application.example.online_bidding_system.entity.*;
import com.application.example.online_bidding_system. exception.BadRequestException;
import com.application.example.online_bidding_system.exception. ResourceNotFoundException;
import com.application.example.online_bidding_system.repository.*;
import org.springframework.beans.factory. annotation.Autowired;
import org. springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org. springframework.stereotype.Service;

import java. math.BigDecimal;
import java. time.LocalDateTime;
import java.util.*;
import java.util. concurrent.ConcurrentHashMap;
import java.util. stream.Collectors;

@Service
public class BidService {

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private StallRepository stallRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BiddingResultRepository biddingResultRepository;

    // Track viewers for each stall
    private final ConcurrentHashMap<Long, Integer> stallViewers = new ConcurrentHashMap<>();

    /**
     * Place a bid
     */
    public ResponseEntity<BidResponse> placeBid(BidRequest request) {
        // Validate request
        if (request. getStallId() == null) {
            throw new BadRequestException("Stall ID is required");
        }
        if (request.getBidderId() == null) {
            throw new BadRequestException("Bidder ID is required");
        }
        if (request. getBiddedPrice() == null) {
            throw new BadRequestException("Bid price is required");
        }

        // Find stall
        Stall stall = stallRepository.findById(request.getStallId())
                .orElseThrow(() -> new ResourceNotFoundException("Stall", "id", request.getStallId()));

        // Find bidder
        User bidder = userRepository. findById(request. getBidderId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getBidderId()));

        // Check if user is a bidder
        if (bidder.getRole() != Role.BIDDER && bidder.getRole() != Role.ADMIN) {
            throw new BadRequestException("Only approved bidders can place bids");
        }

        // Check stall status
        if (stall.getStatus() == StallStatus. CLOSED) {
            throw new BadRequestException("Bidding is closed for this stall");
        }

        if (stall.getStatus() != StallStatus. ACTIVE) {
            throw new BadRequestException("Stall is not active for bidding");
        }

        // Check bidding time window
        LocalDateTime now = LocalDateTime.now();
        if (stall.getBiddingStart() != null && now.isBefore(stall.getBiddingStart())) {
            throw new BadRequestException("Bidding has not started yet.  Starts at: " + stall.getBiddingStart());
        }
        if (stall.getBiddingEnd() != null && now.isAfter(stall.getBiddingEnd())) {
            throw new BadRequestException("Bidding has ended for this stall");
        }

        // Check if bid is higher than current highest
        BigDecimal newBid = request.getBiddedPrice();
        BigDecimal currentHighest = bidRepository.findTopByStallOrderByBiddedPriceDesc(stall)
                .map(Bid::getBiddedPrice)
                .orElse(BigDecimal.ZERO);

        if (newBid.compareTo(currentHighest) <= 0) {
            throw new BadRequestException("Bid must be higher than current highest:  ₹" + currentHighest);
        }

        // Check minimum bid amount (base price)
        BigDecimal minBid = stall.getBasePrice() != null ? stall.getBasePrice() : BigDecimal. ZERO;
        if (newBid.compareTo(minBid) < 0) {
            throw new BadRequestException("Bid must be at least the base price: ₹" + minBid);
        }

        // Create and save bid
        Bid bid = new Bid();
        bid.setStall(stall);
        bid.setBidder(bidder);
        bid.setBiddedPrice(newBid);
        bid.setBidTime(LocalDateTime.now());
        bidRepository.save(bid);

        // Update stall's current highest bid
        stall.setCurrentHighestBid(newBid);
        stallRepository.save(stall);

        // Auto-close if bid >= original price
        if (stall.getOriginalPrice() != null && newBid.compareTo(stall.getOriginalPrice()) >= 0) {
            stall.setStatus(StallStatus. CLOSED);

            if (stall.getResult() == null && biddingResultRepository. findByStall(stall).isEmpty()) {
                BiddingResult result = new BiddingResult();
                result.setStall(stall);
                result. setWinner(bidder);
                result.setWinningPrice(newBid);
                result.setResultTime(LocalDateTime. now());
                biddingResultRepository. save(result);

                stall.setResult(result);
            }

            stallRepository.save(stall);
        }

        // Prepare response
        BidResponse response = new BidResponse();
        response.setBidId(bid. getBidId());
        response.setStallId(stall.getStallId());
        response.setStallName(stall.getStallName());
        response.setStudentId(bidder.getStudentId());
        response.setStudentName(bidder.getStudentName());
        response.setBiddedPrice(bid.getBiddedPrice());
        response.setBidTime(bid.getBidTime());
        response.setMessage("Bid placed successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Get all bidders for a stall (descending order by bid)
     */
    public List<BidderInfo> getBiddersForStall(Long stallId) {
        // Verify stall exists
        if (! stallRepository.existsById(stallId)) {
            throw new ResourceNotFoundException("Stall", "id", stallId);
        }

        return bidRepository.findByStall_StallIdOrderByBiddedPriceDesc(stallId)
                .stream()
                .map(bid -> new BidderInfo(
                        bid.getBidder().getStudentName(),
                        bid.getBiddedPrice()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Get all bids for a stall
     */
    public List<BidResponse> getAllBids(Long stallId) {
        Stall stall = stallRepository.findById(stallId)
                .orElseThrow(() -> new ResourceNotFoundException("Stall", "id", stallId));

        return bidRepository.findByStall(stall).stream()
                .map(bid -> mapBidToResponse(bid, stall))
                .collect(Collectors.toList());
    }

    /**
     * Manually declare winner
     */
    public ResponseEntity<BidResponse> declareWinner(Long stallId) {
        Stall stall = stallRepository.findById(stallId)
                .orElseThrow(() -> new ResourceNotFoundException("Stall", "id", stallId));

        if (stall.getStatus() != StallStatus.CLOSED) {
            throw new BadRequestException("Cannot declare winner.  Bidding is not closed yet.");
        }

        Optional<Bid> topBidOpt = bidRepository. findTopByStallOrderByBiddedPriceDesc(stall);

        if (topBidOpt.isEmpty()) {
            throw new BadRequestException("No bids placed for this stall");
        }

        Bid topBid = topBidOpt.get();

        if (stall.getOriginalPrice() != null &&
                topBid.getBiddedPrice().compareTo(stall.getOriginalPrice()) < 0) {
            throw new BadRequestException("No valid winner:  highest bid (₹" + topBid.getBiddedPrice() +
                    ") is below original price (₹" + stall. getOriginalPrice() + ")");
        }

        BidResponse response = new BidResponse();
        response.setStudentName(topBid. getBidder().getStudentName());
        response.setStudentId(topBid.getBidder().getStudentId());
        response.setBiddedPrice(topBid.getBiddedPrice());
        response.setStallId(stallId);
        response.setStallName(stall.getStallName());
        response.setMessage("Winner:  " + topBid.getBidder().getStudentName());

        return ResponseEntity.ok(response);
    }

    /**
     * Bid history for a stall
     */
    public List<BidHistoryDto> getBidHistory(Long stallId) {
        // Verify stall exists
        if (!stallRepository.existsById(stallId)) {
            throw new ResourceNotFoundException("Stall", "id", stallId);
        }

        List<Bid> bids = bidRepository. findByStall_StallIdOrderByBiddedPriceDesc(stallId);

        return bids. stream()
                .map(bid -> new BidHistoryDto(
                        bid.getBidId(),
                        bid. getStall().getStallId(),
                        bid.getBidder().getStudentName(),
                        bid.getBiddedPrice(),
                        bid.getBidTime()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Scheduler:  update stall status & auto-declare winners
     */
    @Scheduled(fixedRate = 30000)
    public void updateStallsAndDeclareWinners() {
        List<Stall> allStalls = stallRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Stall stall : allStalls) {
            LocalDateTime start = stall.getBiddingStart();
            LocalDateTime end = stall.getBiddingEnd();

            if (start == null || end == null) {
                continue;
            }

            if (now.isBefore(start)) {
                if (stall.getStatus() != StallStatus. AVAILABLE) {
                    stall.setStatus(StallStatus. AVAILABLE);
                    stallRepository.save(stall);
                }
            } else if (now.isBefore(end)) {
                if (stall.getStatus() != StallStatus. ACTIVE) {
                    stall.setStatus(StallStatus. ACTIVE);
                    stallRepository.save(stall);
                }
            } else {
                if (stall.getStatus() != StallStatus. CLOSED) {
                    stall.setStatus(StallStatus.CLOSED);
                    stallRepository. save(stall);
                }

                // Auto-declare winner
                if (stall.getResult() == null) {
                    bidRepository.findTopByStallOrderByBiddedPriceDesc(stall).ifPresent(bid -> {
                        BiddingResult result = new BiddingResult();
                        result.setStall(stall);
                        result.setWinner(bid.getBidder());
                        result.setWinningPrice(bid.getBiddedPrice());
                        result.setResultTime(LocalDateTime.now());
                        biddingResultRepository.save(result);
                        stall. setResult(result);
                        stallRepository.save(stall);
                    });
                }
            }
        }
    }

    /**
     * Get all bids by a user
     */
    public List<BidResponse> getAllBidsByUser(Long studentId) {
        // Verify user exists
        if (!userRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("User", "id", studentId);
        }

        return bidRepository.findByBidder_StudentId(studentId).stream()
                .map(bid -> mapBidToResponse(bid, bid.getStall()))
                .collect(Collectors.toList());
    }

    /**
     * Get the highest bid for a stall
     */
    public BidResponse getHighestBid(Long stallId) {
        Stall stall = stallRepository.findById(stallId)
                .orElseThrow(() -> new ResourceNotFoundException("Stall", "id", stallId));

        return bidRepository.findTopByStallOrderByBiddedPriceDesc(stall)
                .map(bid -> mapBidToResponse(bid, stall))
                .orElse(new BidResponse("No bids yet", BigDecimal.ZERO));
    }

    /**
     * Map Bid entity to BidResponse
     */
    private BidResponse mapBidToResponse(Bid bid, Stall stall) {
        BidResponse response = new BidResponse();
        response.setBidId(bid. getBidId());
        response.setStallId(stall.getStallId());
        response.setStallName(stall.getStallName());
        response.setStudentId(bid.getBidder().getStudentId());
        response.setStudentName(bid.getBidder().getStudentName());
        response.setBiddedPrice(bid.getBiddedPrice());
        response.setBidTime(bid.getBidTime());
        return response;
    }

    // ==================== Viewer Count Methods ====================

    /**
     * Increment viewer count for a stall
     */
    public void incrementViewer(Long stallId) {
        stallViewers.merge(stallId, 1, Integer::sum);
    }

    /**
     * Decrement viewer count for a stall
     */
    public void decrementViewer(Long stallId) {
        stallViewers.computeIfPresent(stallId, (id, count) -> (count > 1) ? count - 1 : null);
    }

    /**
     * Get viewer count for a stall
     */
    public int getViewerCount(Long stallId) {
        return stallViewers.getOrDefault(stallId, 0);
    }
}