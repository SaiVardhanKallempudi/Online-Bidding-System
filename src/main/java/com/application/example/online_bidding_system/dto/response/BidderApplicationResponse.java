package com.application.example.online_bidding_system.dto.response;

import com.application.example.online_bidding_system.entity.Status;
import lombok.Data;

import java.sql.Timestamp;

@Data
public class BidderApplicationResponse {
    private Long applicationId;
    private Long userId;
    private String studentName;
    private String studentEmail;
    private Long phoneNumber;
    private String collageId;
    private String department;
    private Integer year;
    private String gender;
    private String phone;
    private String profilePicture;
    private Status status;
    private String reason;
    private String preferredStallCategory;
    private Timestamp appliedAt;
    private Timestamp reviewedAt;
    private String rejectionReason;
}