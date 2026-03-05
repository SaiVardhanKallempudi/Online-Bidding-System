package com.application.example.online_bidding_system.service;

import com.application.example.online_bidding_system.dto.request.CreateNotificationRequest;
import com.application.example.online_bidding_system.entity.NotificationType;
import com.application.example.online_bidding_system.entity.RelatedEntityType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * ✅ Central hub for all in-app notifications.
 *
 * Called from:
 *  AuthService                   → notifyWelcome          (email signup verified)
 *  CustomOAuth2UserService       → notifyWelcome          (Google signup)
 *  BidderApplicationServiceImpl  → notifyApplicationReceived / Approved / Rejected
 *  BidService                    → notifyOutbid / notifyAuctionWon  (auto-close)
 *  BiddingResultService          → notifyAuctionWon / notifyAuctionLost
 */
@Service
public class NotificationTriggerService {

    @Autowired
    private NotificationService notificationService;

    // ─────────────────────────────────────────────────────────────
    // WELCOME
    // ─────────────────────────────────────────────────────────────

    /** New user registered (email signup OTP verified OR Google login first time) */
    public void notifyWelcome(Long userId, String name) {
        send(userId,
                NotificationType.SYSTEM_ANNOUNCEMENT,
                RelatedEntityType.SYSTEM, null,
                "Welcome to BidMart! 🎉",
                "Hi " + name + "! Your account is ready. Browse stalls and apply as a bidder to start participating in live auctions."
        );
    }

    // ─────────────────────────────────────────────────────────────
    // BIDDER APPLICATION
    // ─────────────────────────────────────────────────────────────

    /** User submitted application — confirmation */
    public void notifyApplicationReceived(Long userId) {
        send(userId,
                NotificationType.SYSTEM_ANNOUNCEMENT,
                RelatedEntityType.APPLICATION, null,
                "Application Received 📝",
                "Your bidder application has been submitted. You will be notified once an admin reviews it."
        );
    }

    /** Admin approved the application */
    public void notifyApplicationApproved(Long userId) {
        send(userId,
                NotificationType.APPLICATION_APPROVED,
                RelatedEntityType.APPLICATION, null,
                "Bidder Application Approved ✅",
                "Congratulations! Your bidder application has been approved. Log out and log back in to activate your bidder access and start placing bids."
        );
    }

    /** Admin rejected the application */
    public void notifyApplicationRejected(Long userId, String reason) {
        String msg = (reason != null && !reason.isBlank())
                ? "Your application was not approved. Reason: " + reason
                : "Your bidder application was not approved at this time. You may reapply later.";
        send(userId,
                NotificationType.APPLICATION_REJECTED,
                RelatedEntityType.APPLICATION, null,
                "Bidder Application Rejected ❌",
                msg
        );
    }

    // ─────────────────────────────────────────────────────────────
    // BID EVENTS
    // ─────────────────────────────────────────────────────────────

    /** Previous highest bidder was outbid */
    public void notifyOutbid(Long previousBidderId, String stallName, Long stallId) {
        send(previousBidderId,
                NotificationType.BID_OUTBID,
                RelatedEntityType.STALL, stallId,
                "You've Been Outbid ⚠️",
                "Someone placed a higher bid on \"" + stallName + "\". Place a new bid now to get back in the lead!"
        );
    }

    // ─────────────────────────────────────────────────────────────
    // AUCTION RESULTS
    // ─────────────────────────────────────────────────────────────

    /** Winner declared */
    public void notifyAuctionWon(Long winnerId, String stallName, Long stallId, String winningPrice) {
        send(winnerId,
                NotificationType.AUCTION_WON,
                RelatedEntityType.STALL, stallId,
                "You Won the Auction! 🎉",
                "Congratulations! You won the bid for \"" + stallName + "\" with a winning price of ₹" + winningPrice + ". Check your email for details."
        );
    }

    /** Losing bidders notified when auction ends */
    public void notifyAuctionLost(Long userId, String stallName, Long stallId) {
        send(userId,
                NotificationType.AUCTION_LOST,
                RelatedEntityType.STALL, stallId,
                "Auction Ended 🏁",
                "The auction for \"" + stallName + "\" has ended. You did not win this time — check out other available stalls!"
        );
    }

    /** Notify when an auction starts (optional — call from startBidding if needed) */
    public void notifyAuctionStarted(Long userId, String stallName, Long stallId) {
        send(userId,
                NotificationType.AUCTION_STARTED,
                RelatedEntityType.STALL, stallId,
                "Auction Started 🏁",
                "Bidding has started for \"" + stallName + "\". Place your bid before time runs out!"
        );
    }

    /** Notify when auction is ending soon (call from scheduler if needed) */
    public void notifyAuctionEndingSoon(Long userId, String stallName, Long stallId, int minutesLeft) {
        send(userId,
                NotificationType.AUCTION_ENDING,
                RelatedEntityType.STALL, stallId,
                "Auction Ending Soon ⏰",
                "Only " + minutesLeft + " minute(s) left for \"" + stallName + "\". Place your final bid now!"
        );
    }

    // ─────────────────────────────────────────────────────────────
    // SYSTEM
    // ─────────────────────────────────────────────────────────────

    /** Broadcast any admin message to a specific user */
    public void notifySystemAnnouncement(Long userId, String title, String message) {
        send(userId,
                NotificationType.SYSTEM_ANNOUNCEMENT,
                RelatedEntityType.SYSTEM, null,
                title, message
        );
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────

    private void send(Long userId, NotificationType type, RelatedEntityType entityType,
                      Long entityId, String title, String message) {
        try {
            CreateNotificationRequest req = new CreateNotificationRequest();
            req.userId           = userId;
            req.type             = type;
            req.title            = title;
            req.message          = message;
            req.relatedEntityId  = entityId;
            req.relatedEntityType = entityType;
            notificationService.createNotification(req);
            System.out.println("📢 Notification [" + type.name() + "] → userId: " + userId);
        } catch (Exception e) {
            // Never let a notification failure break the main business logic
            System.err.println("❌ Notification [" + type.name() + "] failed for userId " + userId + ": " + e.getMessage());
        }
    }
}