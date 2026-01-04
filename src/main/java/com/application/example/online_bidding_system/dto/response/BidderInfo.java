package com.application.example. online_bidding_system.dto.response;

import lombok. AllArgsConstructor;
import lombok. Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BidderInfo {
    private String studentName;
    private BigDecimal biddedPrice;
}