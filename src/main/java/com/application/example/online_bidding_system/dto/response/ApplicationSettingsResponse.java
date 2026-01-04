package com.application.example.online_bidding_system. dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationSettingsResponse {
    private Long id;
    private String festName;
    private String academicYear;

    private Timestamp applicationStartTime;
    private Timestamp applicationEndTime;
    private boolean isApplicationOpen;

    private Timestamp biddingStartTime;
    private Timestamp biddingEndTime;
    private boolean isBiddingOpen;

    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String createdByName;
}