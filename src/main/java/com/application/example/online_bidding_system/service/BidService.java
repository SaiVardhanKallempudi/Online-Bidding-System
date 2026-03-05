package com.application.example.online_bidding_system.service;

import com.application.example.online_bidding_system.dto.request.BidRequest;
import com.application.example.online_bidding_system.dto.response.BidHistoryDto;
import com.application.example.online_bidding_system.dto.response.BidResponse;
import com.application.example.online_bidding_system.dto.response.BidderInfo;
import com.application.example.online_bidding_system.entity.*;
import com.application.example.online_bidding_system.exception.BadRequestException;
import com.application.example.online_bidding_system.exception.ResourceNotFoundException;
import com.application.example.online_bidding_system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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

    @Autowired
    private NotificationTriggerService notificationTrigger; // ✅ ADDED

    private final ConcurrentHashMap<Long, Integer> stallViewers = new ConcurrentHashMap<>();

    public ResponseEntity<BidResponse> placeBid(BidRequest request) {
        if (request.getStallId() == null)     throw new BadRequestException("Stall ID is required");
        if (request.getBidderId() == null)    throw new BadRequestException("Bidder ID is required");
        if (request.getBiddedPrice() == null) throw new BadRequestException("Bid price is required");

        Stall stall = stallRepository.findById(request.getStallId())
                .orElseThrow(() -> new ResourceNotFoundException("Stall", "id", request.getStallId()));

        User bidder = userRepository.findById(request.getBidderId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getBidderId()));

        if (bidder.getRole() != Role.BIDDER && bidder.getRole() != Role.ADMIN)
            throw new BadRequestException("Only approved bidders can place bids");

        if (stall.getStatus() == StallStatus.CLOSED)
            throw new BadRequestException("Bidding is closed for this stall");

        if (stall.getStatus() != StallStatus.ACTIVE)
            throw new BadRequestException("Stall is not active for bidding");

        LocalDateTime now = LocalDateTime.now();
        if (stall.getBiddingStart() != null && now.isBefore(stall.getBiddingStart()))
            throw new BadRequestException("Bidding has not started yet. Starts at: " + stall.getBiddingStart());
        if (stall.getBiddingEnd() != null && now.isAfter(stall.getBiddingEnd()))
            throw new BadRequestException("Bidding has ended for this stall");

        BigDecimal newBid = request.getBiddedPrice();

        // ✅ Capture previous highest bidder BEFORE saving new bid
        Optional<Bid> prevTopBid = bidRepository.findTopByStallOrderByBiddedPriceDesc(stall);
        BigDecimal currentHighest = prevTopBid.map(Bid::getBiddedPrice).orElse(BigDecimal.ZERO);
        Long previousBidderId = prevTopBid.map(b -> b.getBidder().getStudentId()).orElse(null);

        if (newBid.compareTo(currentHighest) <= 0)
            throw new BadRequestException("Bid must be higher than current highest: ₹" + currentHighest);

        BigDecimal minBid = stall.getBasePrice() != null ? stall.getBasePrice() : BigDecimal.ZERO;
        if (newBid.compareTo(minBid) < 0)
            throw new BadRequestException("Bid must be at least the base price: ₹" + minBid);

        // Save bid
        Bid bid = new Bid();
        bid.setStall(stall);
        bid.setBidder(bidder);
        bid.setBiddedPrice(newBid);
        bid.setBidTime(LocalDateTime.now());
        bidRepository.save(bid);

        // Update stall current highest
        stall.setCurrentHighestBid(newBid);
        stallRepository.save(stall);

        // ✅ Notify previous highest bidder they were outbid (skip if same person)
        if (previousBidderId != null && !previousBidderId.equals(request.getBidderId())) {
            notificationTrigger.notifyOutbid(previousBidderId, stall.getStallName(), stall.getStallId());
        }

        // ✅ Auto-close if bid >= original price
        if (stall.getOriginalPrice() != null && newBid.compareTo(stall.getOriginalPrice()) >= 0) {
            stall.setStatus(StallStatus.CLOSED);

            if (stall.getResult() == null && biddingResultRepository.findByStall(stall).isEmpty()) {
                BiddingResult result = new BiddingResult();
                result.setStall(stall);
                result.setWinner(bidder);
                result.setWinningPrice(newBid);
                result.setResultTime(LocalDateTime.now());
                biddingResultRepository.save(result);
                stall.setResult(result);

                // ✅ Notify winner immediately on auto-close
                notificationTrigger.notifyAuctionWon(
                        bidder.getStudentId(),
                        stall.getStallName(),
                        stall.getStallId(),
                        newBid.toString()
                );
            }
            stallRepository.save(stall);
        }

        BidResponse response = new BidResponse();
        response.setBidId(bid.getBidId());
        response.setStallId(stall.getStallId());
        response.setStallName(stall.getStallName());
        response.setStudentId(bidder.getStudentId());
        response.setStudentName(bidder.getStudentName());
        response.setBiddedPrice(bid.getBiddedPrice());
        response.setBidTime(bid.getBidTime());
        response.setMessage("Bid placed successfully");

        return ResponseEntity.ok(response);
    }

    public List<BidderInfo> getBiddersForStall(Long stallId) {
        if (!stallRepository.existsById(stallId))
            throw new ResourceNotFoundException("Stall", "id", stallId);
        return bidRepository.findByStall_StallIdOrderByBiddedPriceDesc(stallId).stream()
                .map(bid -> new BidderInfo(bid.getBidder().getStudentName(), bid.getBiddedPrice()))
                .collect(Collectors.toList());
    }

    public List<BidResponse> getAllBids(Long stallId) {
        Stall stall = stallRepository.findById(stallId)
                .orElseThrow(() -> new ResourceNotFoundException("Stall", "id", stallId));
        return bidRepository.findByStall(stall).stream()
                .map(bid -> mapBidToResponse(bid, stall))
                .collect(Collectors.toList());
    }

    public ResponseEntity<BidResponse> declareWinner(Long stallId) {
        Stall stall = stallRepository.findById(stallId)
                .orElseThrow(() -> new ResourceNotFoundException("Stall", "id", stallId));

        if (stall.getStatus() != StallStatus.CLOSED)
            throw new BadRequestException("Cannot declare winner. Bidding is not closed yet.");

        Optional<Bid> topBidOpt = bidRepository.findTopByStallOrderByBiddedPriceDesc(stall);
        if (topBidOpt.isEmpty())
            throw new BadRequestException("No bids placed for this stall");

        Bid topBid = topBidOpt.get();

        if (stall.getOriginalPrice() != null &&
                topBid.getBiddedPrice().compareTo(stall.getOriginalPrice()) < 0)
            throw new BadRequestException("No valid winner: highest bid (₹" + topBid.getBiddedPrice() +
                    ") is below original price (₹" + stall.getOriginalPrice() + ")");

        BidResponse response = new BidResponse();
        response.setStudentName(topBid.getBidder().getStudentName());
        response.setStudentId(topBid.getBidder().getStudentId());
        response.setBiddedPrice(topBid.getBiddedPrice());
        response.setStallId(stallId);
        response.setStallName(stall.getStallName());
        response.setMessage("Winner: " + topBid.getBidder().getStudentName());
        return ResponseEntity.ok(response);
    }

    public List<BidHistoryDto> getBidHistory(Long stallId) {
        if (!stallRepository.existsById(stallId))
            throw new ResourceNotFoundException("Stall", "id", stallId);
        return bidRepository.findByStall_StallIdOrderByBiddedPriceDesc(stallId).stream()
                .map(bid -> new BidHistoryDto(
                        bid.getBidId(),
                        bid.getStall().getStallId(),
                        bid.getBidder().getStudentName(),
                        bid.getBiddedPrice(),
                        bid.getBidTime()
                ))
                .collect(Collectors.toList());
    }

    @Scheduled(fixedRate = 30000)
    public void updateStallsAndDeclareWinners() {
        List<Stall> allStalls = stallRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Stall stall : allStalls) {
            LocalDateTime start = stall.getBiddingStart();
            LocalDateTime end   = stall.getBiddingEnd();
            if (start == null || end == null) continue;

            if (now.isBefore(start)) {
                if (stall.getStatus() != StallStatus.AVAILABLE) {
                    stall.setStatus(StallStatus.AVAILABLE);
                    stallRepository.save(stall);
                }
            } else if (now.isBefore(end)) {
                if (stall.getStatus() != StallStatus.ACTIVE) {
                    stall.setStatus(StallStatus.ACTIVE);
                    stallRepository.save(stall);
                }
            } else {
                if (stall.getStatus() != StallStatus.CLOSED) {
                    stall.setStatus(StallStatus.CLOSED);
                    stallRepository.save(stall);
                }
                if (stall.getResult() == null) {
                    bidRepository.findTopByStallOrderByBiddedPriceDesc(stall).ifPresent(bid -> {
                        BiddingResult result = new BiddingResult();
                        result.setStall(stall);
                        result.setWinner(bid.getBidder());
                        result.setWinningPrice(bid.getBiddedPrice());
                        result.setResultTime(LocalDateTime.now());
                        biddingResultRepository.save(result);
                        stall.setResult(result);
                        stallRepository.save(stall);

                        // ✅ Notify winner via scheduler path too
                        notificationTrigger.notifyAuctionWon(
                                bid.getBidder().getStudentId(),
                                stall.getStallName(),
                                stall.getStallId(),
                                bid.getBiddedPrice().toString()
                        );
                    });
                }
            }
        }
    }

    public List<BidResponse> getAllBidsByUser(Long studentId) {
        if (!userRepository.existsById(studentId))
            throw new ResourceNotFoundException("User", "id", studentId);
        return bidRepository.findByBidder_StudentId(studentId).stream()
                .map(bid -> mapBidToResponse(bid, bid.getStall()))
                .collect(Collectors.toList());
    }

    public BidResponse getHighestBid(Long stallId) {
        Stall stall = stallRepository.findById(stallId)
                .orElseThrow(() -> new ResourceNotFoundException("Stall", "id", stallId));
        return bidRepository.findTopByStallOrderByBiddedPriceDesc(stall)
                .map(bid -> mapBidToResponse(bid, stall))
                .orElse(new BidResponse("No bids yet", BigDecimal.ZERO));
    }

    private BidResponse mapBidToResponse(Bid bid, Stall stall) {
        BidResponse response = new BidResponse();
        response.setBidId(bid.getBidId());
        response.setStallId(stall.getStallId());
        response.setStallName(stall.getStallName());
        response.setStudentId(bid.getBidder().getStudentId());
        response.setStudentName(bid.getBidder().getStudentName());
        response.setBiddedPrice(bid.getBiddedPrice());
        response.setBidTime(bid.getBidTime());
        return response;
    }

    public void incrementViewer(Long stallId) { stallViewers.merge(stallId, 1, Integer::sum); }
    public void decrementViewer(Long stallId) { stallViewers.computeIfPresent(stallId, (id, count) -> count > 1 ? count - 1 : null); }
    public int getViewerCount(Long stallId)   { return stallViewers.getOrDefault(stallId, 0); }
}