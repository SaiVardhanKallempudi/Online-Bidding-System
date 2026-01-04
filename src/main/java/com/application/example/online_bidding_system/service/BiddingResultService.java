package com. application.example.online_bidding_system.service;

import com.application.example.online_bidding_system.dto. email.EmailDetails;
import com. application.example.online_bidding_system.dto.response.BiddingResultResponse;
import com.application.example. online_bidding_system.entity.*;
import com.application.example.online_bidding_system. exception.BadRequestException;
import com. application.example.online_bidding_system.exception.ResourceNotFoundException;
import com. application.example.online_bidding_system.repository.BidRepository;
import com. application.example.online_bidding_system.repository.BiddingResultRepository;
import com.application. example.online_bidding_system.repository.StallRepository;
import org.springframework.beans. factory.annotation. Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework. scheduling.annotation. Scheduled;
import org.springframework.stereotype. Service;

import java.time.LocalDateTime;
import java.util. List;
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
     * Get result for a specific stall
     */
    public ResponseEntity<? > getResultByStallId(Long stallId) {
        Stall stall = stallRepository.findById(stallId)
                .orElseThrow(() -> new ResourceNotFoundException("Stall", "id", stallId));

        LocalDateTime now = LocalDateTime.now();

        // Check if bidding has ended
        if (stall.getBiddingEnd() != null && stall.getBiddingEnd().isAfter(now)) {
            throw new BadRequestException("Result not available.  Bidding ends at:  " + stall.getBiddingEnd());
        }

        Optional<BiddingResult> resultOpt = biddingResultRepository.findByStall(stall);

        if (resultOpt.isEmpty()) {
            return ResponseEntity.ok("No result declared for this stall yet.");
        }

        return ResponseEntity.ok(convertToDto(resultOpt. get()));
    }

    /**
     * Get all declared results
     */
    public List<BiddingResultResponse> getAllResults() {
        LocalDateTime now = LocalDateTime.now();

        return biddingResultRepository.findAll().stream()
                .filter(result -> result. getStall().getBiddingEnd() != null &&
                        result. getStall().getBiddingEnd().isBefore(now))
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get results by winner
     */
    public List<BiddingResultResponse> getResultsByWinner(Long studentId) {
        return biddingResultRepository.findByWinner_StudentId(studentId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Manually declare result for a stall
     */
    public ResponseEntity<?> declareResultManually(Long stallId) {
        Stall stall = stallRepository.findById(stallId)
                .orElseThrow(() -> new ResourceNotFoundException("Stall", "id", stallId));

        // Check if result already exists
        if (biddingResultRepository.findByStall(stall).isPresent()) {
            throw new BadRequestException("Result already declared for this stall");
        }

        // Find highest bid
        Optional<Bid> highestBid = bidRepository. findTopByStallOrderByBiddedPriceDesc(stall);

        if (highestBid.isEmpty()) {
            throw new BadRequestException("No bids found for this stall");
        }

        // Update stall status to CLOSED
        stall. setStatus(StallStatus.CLOSED);
        stallRepository.save(stall);

        // Declare result
        BiddingResult result = declareResult(highestBid.get(), stall);

        return ResponseEntity.ok(convertToDto(result));
    }

    /**
     * Convert entity to DTO
     */
    private BiddingResultResponse convertToDto(BiddingResult result) {
        BiddingResultResponse dto = new BiddingResultResponse();
        dto.setResultId(result.getResultId());
        dto.setStallId(result.getStall().getStallId());
        dto.setStallName(result. getStall().getStallName());
        dto.setWinningPrice(result.getWinningPrice());
        dto.setWinnerId(result.getWinner().getStudentId());
        dto.setWinnerName(result.getWinner().getStudentName());
        dto.setWinnerEmail(result.getWinner().getStudentEmail());
        dto.setResultTime(result. getResultTime());
        return dto;
    }

    /**
     * Scheduled task to check and declare winners every minute
     */
    @Scheduled(fixedRate = 60000)
    public void checkAndDeclareWinners() {
        List<Stall> closedStalls = stallRepository.findByStatus(StallStatus. CLOSED);

        for (Stall stall : closedStalls) {
            // Skip if result already declared
            if (biddingResultRepository. findByStall(stall).isPresent()) {
                continue;
            }

            // Find highest bid
            Optional<Bid> highestBid = bidRepository.findTopByStallOrderByBiddedPriceDesc(stall);

            if (highestBid.isPresent()) {
                declareResult(highestBid.get(), stall);
            }
        }
    }

    /**
     * Declare result and send notifications
     */
    private BiddingResult declareResult(Bid winningBid, Stall stall) {
        BiddingResult result = new BiddingResult();
        result.setStall(stall);
        result.setWinner(winningBid.getBidder());
        result.setWinningPrice(winningBid.getBiddedPrice());
        result.setResultTime(LocalDateTime.now());

        biddingResultRepository. save(result);

        // Send email notification
        sendWinnerEmail(result);

        System.out.println("✅ Winner declared for stall: " + stall.getStallName() +
                " - Winner: " + winningBid.getBidder().getStudentName() +
                " - Amount: ₹" + winningBid.getBiddedPrice());

        return result;
    }

    /**
     * Send winner notification email
     */
    private void sendWinnerEmail(BiddingResult result) {
        try {
            String toEmail = result.getWinner().getStudentEmail();
            String studentName = result.getWinner().getStudentName();
            String stallName = result.getStall().getStallName();
            String winningAmount = result.getWinningPrice().toString();

            emailService.sendWinnerEmail(toEmail, studentName, stallName, winningAmount);

        } catch (Exception e) {
            System.err.println("❌ Failed to send winner email: " + e.getMessage());

            // Fallback to simple email
            try {
                String to = result.getWinner().getStudentEmail();
                String subject = "🎉 Congratulations! You've Won the Stall Bid";
                String body = "Dear " + result.getWinner().getStudentName() + ",\n\n" +
                        "You have won the bid for the stall:  " + result.getStall().getStallName() + "\n" +
                        "Winning Price: ₹" + result.getWinningPrice() + "\n" +
                        "Result Time: " + result.getResultTime() + "\n\n" +
                        "Thank you for participating!\n" +
                        "Regards,\nOnline Bidding Team";

                EmailDetails email = new EmailDetails(to, subject, body);
                emailService.sendSimpleMail(email);
            } catch (Exception ex) {
                System.err.println("❌ Fallback email also failed: " + ex.getMessage());
            }
        }
    }
}