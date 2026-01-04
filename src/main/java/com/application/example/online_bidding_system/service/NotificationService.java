package com.application. example.online_bidding_system.service;

import com. application.example.online_bidding_system.dto.websocket.BidNotification;
import com. application.example.online_bidding_system.entity. Stall;
import org.springframework.beans. factory.annotation. Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org. springframework.stereotype.Service;

import java. math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Service
public class NotificationService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private Emailservice emailService;

    /**
     * Broadcast new bid to all users watching a stall
     */
    public void broadcastNewBid(Long stallId, String stallName, BigDecimal amount,
                                String bidderName, int totalBids) {
        BidNotification notification = new BidNotification();
        notification. setType("NEW_BID");
        notification.setStallId(stallId);
        notification.setStallName(stallName);
        notification.setCurrentHighestBid(amount);
        notification.setHighestBidderName(bidderName);
        notification. setTotalBids(totalBids);
        notification.setTimestamp(Timestamp. valueOf(LocalDateTime.now()));
        notification.setMessage("New highest bid: ₹" + amount);

        messagingTemplate.convertAndSend("/topic/stall/" + stallId + "/bids", notification);
    }

    /**
     * Notify user they've been outbid
     */
    public void notifyOutbid(Long userId, Long stallId, String stallName,
                             BigDecimal newHighestBid, String newHighestBidder) {
        BidNotification notification = new BidNotification();
        notification. setType("OUTBID");
        notification.setStallId(stallId);
        notification.setStallName(stallName);
        notification.setCurrentHighestBid(newHighestBid);
        notification.setHighestBidderName(newHighestBidder);
        notification.setMessage("You've been outbid on " + stallName + "! New highest:  ₹" + newHighestBid);

        // Send via WebSocket
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                notification
        );
    }

    /**
     * Notify winner via WebSocket
     */
    public void notifyWinner(Long userId, Stall stall, BigDecimal winningPrice) {
        BidNotification notification = new BidNotification();
        notification.setType("WINNER");
        notification.setStallId(stall.getStallId());
        notification.setStallName(stall.getStallName());
        notification. setCurrentHighestBid(winningPrice);
        notification. setMessage("🎉 Congratulations! You won " + stall.getStallName() + " at ₹" + winningPrice);

        // Send to specific user
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                notification
        );

        // Broadcast winner announcement to everyone watching the stall
        notification.setMessage("🏆 Winner declared for " + stall.getStallName());
        messagingTemplate.convertAndSend("/topic/stall/" + stall. getStallId() + "/winner", notification);
    }

    /**
     * Broadcast stall status change
     */
    public void broadcastStallStatusChange(Stall stall, String message) {
        BidNotification notification = new BidNotification();
        notification. setType("STALL_STATUS");
        notification.setStallId(stall.getStallId());
        notification.setStallName(stall.getStallName());
        notification.setMessage(message);
        notification.setTimestamp(Timestamp. valueOf(LocalDateTime. now()));

        messagingTemplate.convertAndSend("/topic/stalls/status", notification);
    }

    /**
     * Send outbid email notification
     */
    public void sendOutbidEmailNotification(String email, String studentName,
                                            String stallName, BigDecimal newHighestBid) {
        try {
            emailService.sendOutbidEmail(email, studentName, stallName, newHighestBid. toString());
        } catch (Exception e) {
            System. err.println("Failed to send outbid email: " + e.getMessage());
        }
    }


}