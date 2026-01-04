package com.application.example. online_bidding_system.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BidNotification {
    private String type;
    private Long stallId;
    private String stallName;
    private BigDecimal currentHighestBid;
    private String highestBidderName;
    private Long highestBidderId;
    private int totalBids;
    private Timestamp timestamp;
    private String message;
}