package com.application. example.online_bidding_system.dto.response;

import com.application.example. online_bidding_system.entity.Status;
import lombok. Data;

import java.sql.Timestamp;

@Data
public class BidderApplicationResponse {
    private Long applicationId;
    private String studentName;
    private String studentEmail;
    private Long phoneNumber;
    private Status status;
    private String reason;
    private String preferredStallCategory;
    private Timestamp appliedAt;
    private Timestamp reviewedAt;
    private String rejectionReason;
}