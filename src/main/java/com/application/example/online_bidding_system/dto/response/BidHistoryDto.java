package com.application.example.online_bidding_system.dto. response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time. LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BidHistoryDto {
    private Long bidId;
    private Long stallId;
    private String studentName;
    private BigDecimal biddedPrice;
    private LocalDateTime bidTime;
}