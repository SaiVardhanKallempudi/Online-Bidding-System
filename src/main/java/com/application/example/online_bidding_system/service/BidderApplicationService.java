package com.application. example.online_bidding_system.service;

import com. application.example.online_bidding_system.dto.request.BidderApplicationRequest;
import com.application. example.online_bidding_system.dto.response.BidderApplicationResponse;
import com.application. example.online_bidding_system.entity.Status;
import org.springframework.http.ResponseEntity;

import java.util. List;

public interface BidderApplicationService {

    ResponseEntity<BidderApplicationResponse> applyAsBidder(BidderApplicationRequest request);

    ResponseEntity<String> approveApplication(Long applicationId);

    ResponseEntity<String> rejectApplication(Long applicationId);

    ResponseEntity<String> updateApplicationStatus(Long applicationId, Status newStatus);

    String getApplicationStatusByEmail(String email);

    List<BidderApplicationResponse> getApplicationsByStatus(Status status);

    Status hasUserAlreadyApplied(String studentEmail);

    List<BidderApplicationResponse> getAllApplications();

    // Add this method
    List<BidderApplicationResponse> getAllUsers();
}