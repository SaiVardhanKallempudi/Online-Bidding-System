package com.application.example.online_bidding_system.dto.email;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmailDetails {
    private String to;
    private String subject;
    private String body;
}
