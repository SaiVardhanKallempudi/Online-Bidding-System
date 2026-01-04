package com. application.example.online_bidding_system.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta. validation.constraints. Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math. BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BidRequest {

    @NotNull(message = "Stall ID is required")
    private Long stallId;

    // This will be set from authenticated user, so not required in request
    private Long bidderId;

    @NotNull(message = "Bid price is required")
    @Positive(message = "Bid price must be positive")
    private BigDecimal biddedPrice;
}