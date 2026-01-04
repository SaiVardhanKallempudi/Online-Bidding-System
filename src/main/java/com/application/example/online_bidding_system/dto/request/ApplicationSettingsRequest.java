package com.application.example.online_bidding_system.dto. request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationSettingsRequest {

    // Fest Information
    private String festName;
    private String academicYear;

    // Application Window (for students to apply as bidders)
    private LocalDateTime applicationStartTime;
    private LocalDateTime applicationEndTime;
    private Boolean isApplicationOpen;

    // Bidding Window (for bidders to place bids)
    private LocalDateTime biddingStartTime;
    private LocalDateTime biddingEndTime;
    private Boolean isBiddingOpen;
}