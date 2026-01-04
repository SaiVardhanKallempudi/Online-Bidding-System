package com.application. example.online_bidding_system.dto.response;

import lombok.Data;

import java.sql. Timestamp;

@Data
public class CommentResponse {
    private Long commentId;
    private Long stallId;
    private Long userId;
    private String userName;
    private String userProfilePicture;
    private String commentText;
    private Timestamp createdAt;
}