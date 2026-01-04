package com.application.example.online_bidding_system. dto.request;

import jakarta.validation. constraints.Email;
import jakarta.validation. constraints.NotBlank;
import jakarta.validation.constraints. Size;
import lombok. Data;

@Data
public class SignUpRequest {
    @NotBlank(message = "Name is required")
    private String studentName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String studentEmail;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    private String collageId;
    private String department;
    private Integer year;
    private String gender;
    private String phone;
    private String address;
}