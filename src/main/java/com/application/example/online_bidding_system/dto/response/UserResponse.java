package com. application.example.online_bidding_system.dto.response;

import com.application.example. online_bidding_system.entity.Role;
import lombok.Data;

@Data
public class UserResponse {
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private String collageId;
    private String department;
    private Integer year;
    private String gender;
    private String phone;
    private Role role;
    private String profilePicture;
    private boolean emailVerified;
    private String address;
}