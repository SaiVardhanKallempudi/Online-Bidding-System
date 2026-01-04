package com.application.example. online_bidding_system.dto.request;

import lombok. Data;

@Data
public class BidderApplicationRequest {
    private String collageId;
    private String studentName;
    private String studentEmail;
    private Long phoneNumber;
    private String otp;
    private boolean termsAccepted;
    private String reason;
    private String preferredStallCategory;
}