package com.application. example.online_bidding_system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints. NotNull;
import jakarta. validation.constraints.Size;
import lombok. Data;

@Data
public class CommentRequest {
    @NotNull(message = "Stall ID is required")
    private Long stallId;

    @NotBlank(message = "Comment text is required")
    @Size(max = 500, message = "Comment must be less than 500 characters")
    private String commentText;
}