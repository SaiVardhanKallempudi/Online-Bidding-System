package com.application.example. online_bidding_system.dto.response;

import com.application.example. online_bidding_system.entity.StallStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StallResponse {
    private Long stallId;
    private Integer stallNo;
    private String stallName;
    private String description;
    private String location;
    private String category;
    private String image;
    private BigDecimal basePrice;
    private BigDecimal originalPrice;
    private BigDecimal currentHighestBid;
    private Integer maxBidders;
    private StallStatus status;
    private LocalDateTime biddingStart;
    private LocalDateTime biddingEnd;
    private long bidCount;
}