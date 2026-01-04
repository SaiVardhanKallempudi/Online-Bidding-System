package com.application.example.online_bidding_system.controller;

import com.application.example.online_bidding_system.dto.response.BiddingResultResponse;
import com.application.example.online_bidding_system.service.BiddingResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/results")
public class BiddingResultController {

    @Autowired
    private BiddingResultService resultService;


    // Get result by stallId
    @GetMapping("/stall/{stallId}")
    public ResponseEntity<?> getResultByStall(@PathVariable Long stallId) {
        return resultService.getResultByStallId(stallId);
    }

    // Get all winners
    @GetMapping("/winners")
    public List<BiddingResultResponse> getAllWinners() {
        return resultService.getAllResults();
    }


}
