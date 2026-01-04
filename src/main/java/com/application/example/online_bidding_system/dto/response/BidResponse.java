package com.application.example.online_bidding_system.dto. response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math. BigDecimal;
import java.time. LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BidResponse {
    private Long bidId;
    private Long stallId;
    private String stallName;
    private Long studentId;
    private String studentName;
    private BigDecimal biddedPrice;
    private LocalDateTime bidTime;
    private String message;

    // Constructor for error messages
    public BidResponse(String message, BigDecimal amount) {
        this.message = message;
        this.biddedPrice = amount;
    }
}