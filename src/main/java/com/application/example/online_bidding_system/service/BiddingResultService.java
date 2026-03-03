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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    /**
     * ✅ Get result for a specific stall
     */
    public ResponseEntity<?> getResultByStallId(Long stallId) {
        Stall stall = stallRepository.findById(stallId)
                .orElseThrow(() -> new ResourceNotFoundException("Stall", "id", stallId));

        LocalDateTime now = LocalDateTime.now();

        // ✅ Check if bidding has ended
        if (stall.getBiddingEnd() != null && stall.getBiddingEnd().isAfter(now)) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "PENDING");
            response.put("message", "Bidding is still in progress");
            response.put("biddingEndsAt", stall.getBiddingEnd());
            return ResponseEntity.ok(response);
        }

        // ✅ Check if result exists
        Optional<BiddingResult> resultOpt = biddingResultRepository.findByStall(stall);

        if (resultOpt.isEmpty()) {
            // ✅ Check if there are any bids
            Optional<Bid> highestBid = bidRepository.findTopByStallOrderByBiddedPriceDesc(stall);

            if (highestBid.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "NO_BIDS");
                response.put("message", "No bids were placed for this stall");
                return ResponseEntity.ok(response);
            }

            // ✅ Auto-declare if auction ended but result not declared
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

    /**
     * ✅ Get all declared results
     */
    public List<BiddingResultResponse> getAllResults() {
        return biddingResultRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * ✅ Get results by winner
     */
    public List<BiddingResultResponse> getResultsByWinner(Long studentId) {
        return biddingResultRepository.findByWinner_StudentId(studentId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * ✅ Manually declare result for a stall (Admin only)
     */
    @Transactional
    public ResponseEntity<?> declareResultManually(Long stallId) {
        Stall stall = stallRepository.findById(stallId)
                .orElseThrow(() -> new ResourceNotFoundException("Stall", "id", stallId));

        // ✅ Check if result already exists
        if (biddingResultRepository.findByStall(stall).isPresent()) {
            throw new BadRequestException("Result already declared for this stall");
        }

        // ✅ Find highest bid
        Optional<Bid> highestBid = bidRepository.findTopByStallOrderByBiddedPriceDesc(stall);

        if (highestBid.isEmpty()) {
            throw new BadRequestException("No bids found for this stall. Cannot declare winner.");
        }

        // ✅ Check if bid meets base price or original price
        BigDecimal winningBid = highestBid.get().getBiddedPrice();
        if (winningBid.compareTo(stall.getBasePrice()) < 0) {
            throw new BadRequestException("Highest bid does not meet the base price requirement");
        }

        // ✅ Update stall status to CLOSED
        stall.setStatus(StallStatus.CLOSED);
        stallRepository.save(stall);

        // ✅ Declare result
        BiddingResult result = declareResult(highestBid.get(), stall);

        System.out.println("✅ Result manually declared for Stall ID: " + stallId);

        return ResponseEntity.ok(convertToDto(result));
    }

    /**
     * ✅ Scheduled task - Check and auto-close auctions that have ended
     */
    @Scheduled(fixedRate = 30000) // Run every 30 seconds
    @Transactional
    public void autoCloseExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();

        // ✅ Find all ACTIVE stalls whose bidding has ended
        List<Stall> expiredStalls = stallRepository.findAll().stream()
                .filter(stall -> stall.getStatus() == StallStatus.ACTIVE)
                .filter(stall -> stall.getBiddingEnd() != null && stall.getBiddingEnd().isBefore(now))
                .collect(Collectors.toList());

        System.out.println("🔍 Checking for expired auctions... Found: " + expiredStalls.size());

        for (Stall stall : expiredStalls) {
            try {
                // ✅ Change status to CLOSED
                stall.setStatus(StallStatus.CLOSED);
                stallRepository.save(stall);

                System.out.println("🔒 Stall #" + stall.getStallId() + " (" + stall.getStallName() + ") marked as CLOSED");
            } catch (Exception e) {
                System.err.println("❌ Error closing stall #" + stall.getStallId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * ✅ Scheduled task - Declare winners for closed stalls
     */
    @Scheduled(fixedRate = 60000) // Run every 60 seconds
    @Transactional
    public void checkAndDeclareWinners() {
        // ✅ Find all CLOSED stalls
        List<Stall> closedStalls = stallRepository.findByStatus(StallStatus.CLOSED);

        System.out.println("🔍 Checking for winners... Found " + closedStalls.size() + " closed stalls");

        for (Stall stall : closedStalls) {
            try {
                // ✅ Skip if result already declared
                if (biddingResultRepository.findByStall(stall).isPresent()) {
                    continue;
                }

                // ✅ Find highest bid
                Optional<Bid> highestBid = bidRepository.findTopByStallOrderByBiddedPriceDesc(stall);

                if (highestBid.isEmpty()) {
                    System.out.println("⚠️ No bids found for Stall #" + stall.getStallId() + " - Skipping");
                    continue;
                }

                // ✅ Check if bid meets minimum requirements
                BigDecimal winningBidAmount = highestBid.get().getBiddedPrice();
                if (winningBidAmount.compareTo(stall.getBasePrice()) < 0) {
                    System.out.println("⚠️ Highest bid for Stall #" + stall.getStallId() +
                            " does not meet base price - Skipping");
                    continue;
                }

                // ✅ Declare winner
                declareResult(highestBid.get(), stall);

            } catch (Exception e) {
                System.err.println("❌ Error declaring winner for Stall #" + stall.getStallId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * ✅ Declare result and send notifications
     */
    @Transactional
    protected BiddingResult declareResult(Bid winningBid, Stall stall) {
        // ✅ Create result
        BiddingResult result = new BiddingResult();
        result.setStall(stall);
        result.setWinner(winningBid.getBidder());
        result.setWinningPrice(winningBid.getBiddedPrice());
        result.setResultTime(LocalDateTime.now());

        biddingResultRepository.save(result);

        // ✅ Update stall with winner reference
        stall.setResult(result);
        stallRepository.save(stall);

        // ✅ Send email notification
        sendWinnerEmail(result);

        System.out.println("🏆 Winner declared for Stall: " + stall.getStallName() +
                " | Winner: " + winningBid.getBidder().getStudentName() +
                " | Amount: ₹" + winningBid.getBiddedPrice());

        return result;
    }

    /**
     * ✅ Send winner notification email
     */
    private void sendWinnerEmail(BiddingResult result) {
        try {
            String toEmail = result.getWinner().getStudentEmail();
            String studentName = result.getWinner().getStudentName();
            String stallName = result.getStall().getStallName();
            String winningAmount = "₹" + result.getWinningPrice().toString();

            // ✅ Try sending formatted email first
            emailService.sendWinnerEmail(toEmail, studentName, stallName, winningAmount);

            System.out.println("📧 Winner email sent to: " + toEmail);

        } catch (Exception e) {
            System.err.println("❌ Failed to send winner email: " + e.getMessage());

            // ✅ Fallback to simple email
            try {
                String to = result.getWinner().getStudentEmail();
                String subject = "🎉 Congratulations! You've Won the Bid";
                String body = String.format(
                        "Dear %s,\n\n" +
                                "Congratulations! You have won the bid for:\n\n" +
                                "🏪 Stall: %s\n" +
                                "💰 Winning Price: ₹%s\n" +
                                "📅 Result Declared: %s\n\n" +
                                "Thank you for participating in BidMart!\n\n" +
                                "Best regards,\n" +
                                "BidMart Team",
                        result.getWinner().getStudentName(),
                        result.getStall().getStallName(),
                        result.getWinningPrice(),
                        result.getResultTime()
                );

                EmailDetails email = new EmailDetails(to, subject, body);
                emailService.sendSimpleMail(email);

                System.out.println("📧 Fallback email sent to: " + to);
            } catch (Exception ex) {
                System.err.println("❌ Fallback email also failed: " + ex.getMessage());
            }
        }
    }

    /**
     * ✅ Convert entity to DTO
     */
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