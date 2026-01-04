package com.application.example.online_bidding_system.dto.request;

import lombok.Data;

@Data
public class OtpVerifyRequest {
    private String studentEmail;
    private String otp;
}
