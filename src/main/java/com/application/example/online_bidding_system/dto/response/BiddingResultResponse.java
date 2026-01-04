package com.application.example.online_bidding_system.dto. response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BiddingResultResponse {
    private Long resultId;
    private Long stallId;
    private String stallName;
    private Long winnerId;
    private String winnerName;
    private String winnerEmail;
    private BigDecimal winningPrice;
    private LocalDateTime resultTime;
}