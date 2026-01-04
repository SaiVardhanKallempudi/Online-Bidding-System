package com.application.example.online_bidding_system.dto. request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time. LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateStallRequest {
    private Integer stallNo;
    private String stallName;
    private String description;
    private String location;
    private String category;
    private String image;
    private BigDecimal basePrice;
    private BigDecimal originalPrice;
    private Integer maxBidders;
    private LocalDateTime biddingStart;
    private LocalDateTime biddingEnd;
}