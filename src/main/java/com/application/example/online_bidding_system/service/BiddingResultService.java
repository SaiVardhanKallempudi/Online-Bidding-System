package com.application.example.online_bidding_system.service;

import com.application.example.online_bidding_system.dto.email.EmailDetails;
import com.application.example.online_bidding_system.dto.response.BiddingResultResponse;
import com.application.example.online_bidding_system.entity.*;
import com.application.example.online_bidding_system.exception.BadRequestException;
import com.application.example.online_bidding_system.exception.ResourceNotFoundException;
import com.application.example.online_bidding_system.repository.BidRepository;
import com.application.example.online_bidding_system.repository.BiddingResultRepository;
import com.application.example.online_bidding_system.repository.StallRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BiddingResultService {

    @Autowired
    private BiddingResultRepository biddingResultRepository;

    @Autowired
    private StallRepository stallRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private Emailservice emailService;

    @Autowired
    private NotificationTriggerService notificationTrigger; // ✅ ADDED

    public ResponseEntity<?> getResultByStallId(Long stallId) {
        Stall stall = stallRepository.findById(stallId)
                .orElseThrow(() -> new ResourceNotFoundException("Stall", "id", stallId));

        LocalDateTime now = LocalDateTime.now();

        if (stall.getBiddingEnd() != null && stall.getBiddingEnd().isAfter(now)) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "PENDING");
            response.put("message", "Bidding is still in progress");
            response.put("biddingEndsAt", stall.getBiddingEnd());
            return ResponseEntity.ok(response);
        }

        Optional<BiddingResult> resultOpt = biddingResultRepository.findByStall(stall);

        if (resultOpt.isEmpty()) {
            Optional<Bid> highestBid = bidRepository.findTopByStallOrderByBiddedPriceDesc(stall);
            if (highestBid.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "NO_BIDS");
                response.put("message", "No bids were placed for this stall");
                return ResponseEntity.ok(response);
            }
            if (stall.getStatus() == StallStatus.CLOSED) {
                BiddingResult result = declareResult(highestBid.get(), stall);
                return ResponseEntity.ok(convertToDto(result));
            }
            Map<String, Object> response = new HashMap<>();
            response.put("status", "NOT_DECLARED");
            response.put("message", "Result will be declared soon");
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.ok(convertToDto(resultOpt.get()));
    }

    public List<BiddingResultResponse> getAllResults() {
        return biddingResultRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<BiddingResultResponse> getResultsByWinner(Long studentId) {
        return biddingResultRepository.findByWinner_StudentId(studentId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ResponseEntity<?> declareResultManually(Long stallId) {
        Stall stall = stallRepository.findById(stallId)
                .orElseThrow(() -> new ResourceNotFoundException("Stall", "id", stallId));

        if (biddingResultRepository.findByStall(stall).isPresent())
            throw new BadRequestException("Result already declared for this stall");

        Optional<Bid> highestBid = bidRepository.findTopByStallOrderByBiddedPriceDesc(stall);
        if (highestBid.isEmpty())
            throw new BadRequestException("No bids found for this stall. Cannot declare winner.");

        BigDecimal winningBid = highestBid.get().getBiddedPrice();
        if (winningBid.compareTo(stall.getBasePrice()) < 0)
            throw new BadRequestException("Highest bid does not meet the base price requirement");

        stall.setStatus(StallStatus.CLOSED);
        stallRepository.save(stall);

        BiddingResult result = declareResult(highestBid.get(), stall);

        System.out.println("✅ Result manually declared for Stall ID: " + stallId);
        return ResponseEntity.ok(convertToDto(result));
    }

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void autoCloseExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();

        List<Stall> expiredStalls = stallRepository.findAll().stream()
                .filter(s -> s.getStatus() == StallStatus.ACTIVE)
                .filter(s -> s.getBiddingEnd() != null && s.getBiddingEnd().isBefore(now))
                .collect(Collectors.toList());

        System.out.println("🔍 Checking expired auctions... Found: " + expiredStalls.size());

        for (Stall stall : expiredStalls) {
            try {
                stall.setStatus(StallStatus.CLOSED);
                stallRepository.save(stall);
                System.out.println("🔒 Stall #" + stall.getStallId() + " closed");
            } catch (Exception e) {
                System.err.println("❌ Error closing stall #" + stall.getStallId() + ": " + e.getMessage());
            }
        }
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void checkAndDeclareWinners() {
        List<Stall> closedStalls = stallRepository.findByStatus(StallStatus.CLOSED);
        System.out.println("🔍 Checking winners... " + closedStalls.size() + " closed stalls");

        for (Stall stall : closedStalls) {
            try {
                if (biddingResultRepository.findByStall(stall).isPresent()) continue;

                Optional<Bid> highestBid = bidRepository.findTopByStallOrderByBiddedPriceDesc(stall);
                if (highestBid.isEmpty()) continue;

                if (highestBid.get().getBiddedPrice().compareTo(stall.getBasePrice()) < 0) continue;

                declareResult(highestBid.get(), stall);

            } catch (Exception e) {
                System.err.println("❌ Error declaring winner for Stall #" + stall.getStallId() + ": " + e.getMessage());
            }
        }
    }

    @Transactional
    protected BiddingResult declareResult(Bid winningBid, Stall stall) {
        BiddingResult result = new BiddingResult();
        result.setStall(stall);
        result.setWinner(winningBid.getBidder());
        result.setWinningPrice(winningBid.getBiddedPrice());
        result.setResultTime(LocalDateTime.now());
        biddingResultRepository.save(result);

        stall.setResult(result);
        stallRepository.save(stall);

        // ✅ Notify winner
        notificationTrigger.notifyAuctionWon(
                winningBid.getBidder().getStudentId(),
                stall.getStallName(),
                stall.getStallId(),
                winningBid.getBiddedPrice().toString()
        );

        // ✅ Notify all other bidders they lost
        bidRepository.findByStall(stall).stream()
                .map(b -> b.getBidder().getStudentId())
                .filter(id -> !id.equals(winningBid.getBidder().getStudentId()))
                .distinct()
                .forEach(loserId ->
                        notificationTrigger.notifyAuctionLost(loserId, stall.getStallName(), stall.getStallId())
                );

        // Send winner email
        sendWinnerEmail(result);

        System.out.println("🏆 Winner: " + winningBid.getBidder().getStudentName()
                + " | Stall: " + stall.getStallName()
                + " | ₹" + winningBid.getBiddedPrice());

        return result;
    }

    private void sendWinnerEmail(BiddingResult result) {
        try {
            emailService.sendWinnerEmail(
                    result.getWinner().getStudentEmail(),
                    result.getWinner().getStudentName(),
                    result.getStall().getStallName(),
                    "₹" + result.getWinningPrice()
            );
            System.out.println("📧 Winner email sent to: " + result.getWinner().getStudentEmail());
        } catch (Exception e) {
            System.err.println("❌ Winner email failed: " + e.getMessage());
            try {
                String body = String.format(
                        "Dear %s,\n\nCongratulations! You have won the bid for:\n\n" +
                                "🏪 Stall: %s\n💰 Winning Price: ₹%s\n📅 Result: %s\n\n" +
                                "Best regards,\nBidMart Team",
                        result.getWinner().getStudentName(),
                        result.getStall().getStallName(),
                        result.getWinningPrice(),
                        result.getResultTime()
                );
                emailService.sendSimpleMail(new EmailDetails(
                        result.getWinner().getStudentEmail(),
                        "🎉 Congratulations! You've Won the Bid",
                        body
                ));
            } catch (Exception ex) {
                System.err.println("❌ Fallback email also failed: " + ex.getMessage());
            }
        }
    }

    private BiddingResultResponse convertToDto(BiddingResult result) {
        BiddingResultResponse dto = new BiddingResultResponse();
        dto.setResultId(result.getResultId());
        dto.setStallId(result.getStall().getStallId());
        dto.setStallName(result.getStall().getStallName());
        dto.setWinningPrice(result.getWinningPrice());
        dto.setWinnerId(result.getWinner().getStudentId());
        dto.setWinnerName(result.getWinner().getStudentName());
        dto.setWinnerEmail(result.getWinner().getStudentEmail());
        dto.setResultTime(result.getResultTime());
        return dto;
    }
}