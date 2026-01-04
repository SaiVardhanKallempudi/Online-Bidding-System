package com.application.example.online_bidding_system.controller;

import com.application.example.online_bidding_system. dto.request.BidRequest;
import com.application.example.online_bidding_system.dto. response.BidResponse;
import com. application.example.online_bidding_system.dto.websocket.BidNotification;
import com.application.example. online_bidding_system.service.BidService;
import com.application.example.online_bidding_system.service.NotificationService;
import org.springframework.beans. factory.annotation. Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation. DestinationVariable;
import org. springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org. springframework.stereotype.Controller;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Controller
public class BidWebSocketController {

    @Autowired
    private BidService bidService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private NotificationService notificationService;

    /**
     * Handle incoming bid via WebSocket
     * Client sends to:  /app/bid/place
     * Response broadcast to: /topic/stall/{stallId}/bids
     */
    @MessageMapping("/bid/place")
    public void placeBid(BidRequest bidRequest) {
        ResponseEntity<BidResponse> responseEntity = bidService. placeBid(bidRequest);
        BidResponse response = responseEntity.getBody();

        if (response != null && response.getBidId() != null) {
            // Create notification
            BidNotification notification = new BidNotification();
            notification.setType("NEW_BID");
            notification.setStallId(response. getStallId());
            notification.setStallName(response.getStallName());
            notification.setCurrentHighestBid(response. getBiddedPrice());
            notification.setHighestBidderName(response.getStudentName());
            notification.setHighestBidderId(response.getStudentId());

            // Convert LocalDateTime to Timestamp
            if (response.getBidTime() != null) {
                notification. setTimestamp(Timestamp.valueOf(response.getBidTime()));
            } else {
                notification.setTimestamp(Timestamp.valueOf(LocalDateTime.now()));
            }

            notification.setMessage("New bid of ₹" + response.getBiddedPrice() + " by " + response.getStudentName());

            // Broadcast to all subscribers of this stall
            messagingTemplate.convertAndSend(
                    "/topic/stall/" + response.getStallId() + "/bids",
                    notification
            );
        }
    }

    /**
     * Subscribe to stall updates
     * Clients can join:  /app/stall/{stallId}/join
     */
    @MessageMapping("/stall/{stallId}/join")
    @SendTo("/topic/stall/{stallId}/users")
    public String joinStallRoom(@DestinationVariable Long stallId, String username) {
        bidService.incrementViewer(stallId);
        return username + " joined bidding for stall #" + stallId;
    }

    /**
     * Leave stall room
     */
    @MessageMapping("/stall/{stallId}/leave")
    @SendTo("/topic/stall/{stallId}/users")
    public String leaveStallRoom(@DestinationVariable Long stallId, String username) {
        bidService.decrementViewer(stallId);
        return username + " left bidding for stall #" + stallId;
    }

    /**
     * Get live bid updates for a stall
     * Clients subscribe to: /topic/stall/{stallId}/bids
     */
    @MessageMapping("/stall/{stallId}/subscribe")
    public void subscribeToStall(@DestinationVariable Long stallId) {
        System.out.println("Client subscribed to stall: " + stallId);
        bidService.incrementViewer(stallId);
    }

    /**
     * Get viewer count for a stall
     */
    @MessageMapping("/stall/{stallId}/viewers")
    @SendTo("/topic/stall/{stallId}/viewers")
    public int getViewerCount(@DestinationVariable Long stallId) {
        return bidService.getViewerCount(stallId);
    }
}