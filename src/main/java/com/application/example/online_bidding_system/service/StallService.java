package com.application.example.online_bidding_system.service;

import com.application. example.online_bidding_system.dto.request.CreateStallRequest;
import com.application. example.online_bidding_system.dto.response.StallResponse;
import com.application.example. online_bidding_system.entity. Stall;
import com.application.example.online_bidding_system.entity. StallStatus;
import com.application. example.online_bidding_system.entity.User;
import com. application.example.online_bidding_system.exception.BadRequestException;
import com.application.example.online_bidding_system.exception. ResourceNotFoundException;
import com.application.example.online_bidding_system.repository. BidRepository;
import com.application.example.online_bidding_system.repository. BiddingResultRepository;
import com.application.example. online_bidding_system.repository.StallRepository;
import org.springframework.beans. factory.annotation. Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org. springframework.stereotype.Service;
import org. springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java. math.BigDecimal;
import java.time.LocalDateTime;
import java.util. HashMap;
import java. util.List;
import java.util. Map;
import java.util.stream. Collectors;

@Service
@SuppressWarnings("unused")
public class StallService {

    @Autowired
    private StallRepository stallRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private BiddingResultRepository biddingResultRepository;

    /**
     * Add a new stall
     */
    public ResponseEntity<StallResponse> addStall(CreateStallRequest request) {
        // Validate required fields
        if (request.getStallName() == null || request.getStallName().trim().isEmpty()) {
            throw new BadRequestException("Stall name is required");
        }
        if (request.getOriginalPrice() == null) {
            throw new BadRequestException("Original price is required");
        }

        Stall stall = mapRequestToEntity(request);

        // Set base price (default to 0 if not provided)
        if (request. getBasePrice() != null) {
            stall.setBasePrice(request.getBasePrice());
        } else {
            stall.setBasePrice(BigDecimal. ZERO);
        }

        // Set current highest bid to 0
        stall.setCurrentHighestBid(BigDecimal. ZERO);

        // Set status
        stall.setStatus(StallStatus.AVAILABLE);

        stallRepository.save(stall);

        return ResponseEntity.ok(mapEntityToResponse(stall));
    }

    /**
     * Add a new stall with admin user
     */
    public ResponseEntity<StallResponse> addStall(CreateStallRequest request, User createdBy) {
        Stall stall = mapRequestToEntity(request);

        // Set base price
        if (request. getBasePrice() != null) {
            stall.setBasePrice(request.getBasePrice());
        } else {
            stall.setBasePrice(BigDecimal.ZERO);
        }

        stall.setCurrentHighestBid(BigDecimal.ZERO);
        stall.setStatus(StallStatus. AVAILABLE);
        stall.setCreatedBy(createdBy);

        stallRepository. save(stall);

        return ResponseEntity.ok(mapEntityToResponse(stall));
    }

    /**
     * Delete stall by ID
     */
    public void deleteByStallId(Long stallId) {
        Stall stall = stallRepository.findById(stallId)
                .orElseThrow(() -> new ResourceNotFoundException("Stall", "id", stallId));

        // Check if stall is active
        if (stall.getStatus() == StallStatus.ACTIVE) {
            throw new BadRequestException("Cannot delete an active stall.  Close bidding first.");
        }

        // Check if stall has bids
        long bidCount = bidRepository.countByStall_StallId(stallId);
        if (bidCount > 0) {
            throw new BadRequestException("Cannot delete stall with existing bids.  Stall has " + bidCount + " bids.");
        }

        stallRepository.delete(stall);
    }

    /**
     * Update stall
     */
    public ResponseEntity<StallResponse> updateStall(Long stallId, CreateStallRequest request, MultipartFile imageFile) throws IOException {
        Stall stall = stallRepository.findById(stallId)
                .orElseThrow(() -> new ResourceNotFoundException("Stall", "id", stallId));

        // Check if stall is closed with result
        if (stall.getStatus() == StallStatus. CLOSED && stall.getResult() != null) {
            throw new BadRequestException("Cannot update a stall with declared result");
        }

        // Update fields if provided
        if (request.getStallName() != null) {
            stall.setStallName(request.getStallName());
        }
        if (request. getStallNo() != null) {
            stall.setStallNo(request.getStallNo());
        }
        if (request.getLocation() != null) {
            stall. setLocation(request. getLocation());
        }
        if (request.getOriginalPrice() != null) {
            stall.setOriginalPrice(request.getOriginalPrice());
        }
        if (request.getBasePrice() != null) {
            stall.setBasePrice(request.getBasePrice());
        }
        if (request.getDescription() != null) {
            stall. setDescription(request. getDescription());
        }
        if (request.getBiddingStart() != null) {
            stall.setBiddingStart(request. getBiddingStart());
        }
        if (request.getBiddingEnd() != null) {
            stall.setBiddingEnd(request. getBiddingEnd());
        }
        if (request.getMaxBidders() != null) {
            stall.setMaxBidders(request.getMaxBidders());
        }
        if (request.getCategory() != null) {
            stall.setCategory(request.getCategory());
        }

        // Update image if present
        if (imageFile != null && ! imageFile.isEmpty()) {
            stall.setImage(imageFile.getOriginalFilename());
        } else if (request.getImage() != null) {
            stall. setImage(request. getImage());
        }

        Stall updated = stallRepository.save(stall);
        return ResponseEntity. ok(mapEntityToResponse(updated));
    }

    /**
     * Update stall without image file
     */
    public ResponseEntity<StallResponse> updateStall(Long stallId, CreateStallRequest request) {
        try {
            return updateStall(stallId, request, null);
        } catch (IOException e) {
            throw new RuntimeException("Error updating stall", e);
        }
    }

    /**
     * Get all stalls
     */
    public List<StallResponse> getAllStalls() {
        List<Stall> stalls = stallRepository.findAll();
        return stalls. stream()
                .map(this::mapEntityToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get stall by ID
     */
    public ResponseEntity<StallResponse> getStallById(Long stallId) {
        Stall stall = stallRepository.findById(stallId)
                .orElseThrow(() -> new ResourceNotFoundException("Stall", "id", stallId));

        return ResponseEntity.ok(mapEntityToResponse(stall));
    }

    /**
     * Get stall entity by ID (for internal use)
     */
    public Stall getStallEntityById(Long stallId) {
        return stallRepository. findById(stallId)
                .orElseThrow(() -> new ResourceNotFoundException("Stall", "id", stallId));
    }

    /**
     * Get stall by stall number
     */
    public ResponseEntity<StallResponse> getStallByNumber(int stallNo) {
        Stall stall = stallRepository.findByStallNo(stallNo)
                .orElseThrow(() -> new ResourceNotFoundException("Stall", "stallNo", stallNo));

        return ResponseEntity.ok(mapEntityToResponse(stall));
    }

    /**
     * Get stalls by status
     */
    public List<StallResponse> getStallsByStatus(StallStatus status) {
        return stallRepository.findByStatus(status).stream()
                .map(this::mapEntityToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get active stalls
     */
    public List<StallResponse> getActiveStalls() {
        return stallRepository.findAllActiveStalls().stream()
                .map(this::mapEntityToResponse)
                .collect(Collectors. toList());
    }

    /**
     * Get available stalls
     */
    public List<StallResponse> getAvailableStalls() {
        return stallRepository.findByStatus(StallStatus.AVAILABLE).stream()
                .map(this::mapEntityToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get closed stalls
     */
    public List<StallResponse> getClosedStalls() {
        return stallRepository.findByStatus(StallStatus. CLOSED).stream()
                .map(this::mapEntityToResponse)
                .collect(Collectors. toList());
    }

    /**
     * Get stalls with minimum bidders
     */
    public List<StallResponse> getStallsByMinBidders(int minBidders) {
        return stallRepository.findByMaxBiddersGreaterThan(minBidders).stream()
                .map(this:: mapEntityToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get stall counts by status
     */
    public Map<String, Long> getStallCounts() {
        Map<String, Long> counts = new HashMap<>();
        counts.put("total", stallRepository.count());
        counts.put("available", stallRepository.countByStatus(StallStatus.AVAILABLE));
        counts.put("active", stallRepository.countByStatus(StallStatus.ACTIVE));
        counts.put("closed", stallRepository.countByStatus(StallStatus. CLOSED));
        return counts;
    }

    /**
     * Start bidding for a stall
     */
    public ResponseEntity<Map<String, Object>> startBidding(Long stallId) {
        Stall stall = stallRepository.findById(stallId)
                .orElseThrow(() -> new ResourceNotFoundException("Stall", "id", stallId));

        if (stall.getStatus() == StallStatus. ACTIVE) {
            throw new BadRequestException("Bidding is already active for this stall");
        }

        if (stall.getStatus() == StallStatus.CLOSED) {
            throw new BadRequestException("Cannot restart bidding for a closed stall");
        }

        stall.setStatus(StallStatus. ACTIVE);
        if (stall.getBiddingStart() == null) {
            stall.setBiddingStart(LocalDateTime.now());
        }
        stallRepository.save(stall);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Bidding started for stall:  " + stall. getStallName());
        response.put("biddingStart", stall.getBiddingStart());

        return ResponseEntity. ok(response);
    }

    /**
     * Stop bidding for a stall
     */
    public ResponseEntity<Map<String, Object>> stopBidding(Long stallId) {
        Stall stall = stallRepository.findById(stallId)
                .orElseThrow(() -> new ResourceNotFoundException("Stall", "id", stallId));

        if (stall.getStatus() != StallStatus. ACTIVE) {
            throw new BadRequestException("Bidding is not active for this stall");
        }

        stall.setStatus(StallStatus. CLOSED);
        stall.setBiddingEnd(LocalDateTime.now());
        stallRepository.save(stall);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Bidding stopped for stall: " + stall.getStallName());
        response.put("biddingEnd", stall.getBiddingEnd());

        return ResponseEntity.ok(response);
    }

    /**
     * Scheduled task:  Update stall status automatically
     */
    @Scheduled(fixedRate = 60000)
    public void updateStallStatusAutomatically() {
        List<Stall> allStalls = stallRepository. findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Stall stall : allStalls) {
            LocalDateTime start = stall.getBiddingStart();
            LocalDateTime end = stall.getBiddingEnd();

            // Skip if already closed with result
            if (stall.getStatus() == StallStatus.CLOSED && stall.getResult() != null) {
                continue;
            }

            if (start != null && end != null) {
                StallStatus newStatus = stall.getStatus();

                if (now.isBefore(start)) {
                    newStatus = StallStatus.AVAILABLE;
                } else if (now. isAfter(start) && now.isBefore(end)) {
                    newStatus = StallStatus. ACTIVE;
                } else if (now.isAfter(end)) {
                    newStatus = StallStatus.CLOSED;
                }

                // Only save if status changed
                if (newStatus != stall.getStatus()) {
                    stall.setStatus(newStatus);
                    stallRepository. save(stall);
                    System.out.println("✅ Stall '" + stall. getStallName() + "' status updated to:  " + newStatus);
                }
            }
        }
    }

    /**
     * Update current highest bid for a stall
     */
    public void updateCurrentHighestBid(Long stallId, BigDecimal amount) {
        Stall stall = stallRepository.findById(stallId)
                .orElseThrow(() -> new ResourceNotFoundException("Stall", "id", stallId));

        stall.setCurrentHighestBid(amount);
        stallRepository.save(stall);
    }

    // ==================== MAPPER METHODS ====================

    /**
     * Map CreateStallRequest to Stall entity
     */
    private Stall mapRequestToEntity(CreateStallRequest request) {
        Stall stall = new Stall();
        stall. setStallName(request.getStallName());
        stall.setStallNo(request. getStallNo());
        stall. setLocation(request.getLocation());
        stall.setOriginalPrice(request.getOriginalPrice());
        stall.setBasePrice(request.getBasePrice());
        stall.setDescription(request.getDescription());
        stall.setBiddingStart(request.getBiddingStart());
        stall.setBiddingEnd(request.getBiddingEnd());
        stall.setImage(request.getImage());
        stall.setMaxBidders(request.getMaxBidders());
        stall. setCategory(request.getCategory());
        return stall;
    }

    /**
     * Map Stall entity to StallResponse
     */
    private StallResponse mapEntityToResponse(Stall stall) {
        StallResponse response = new StallResponse();
        response.setStallId(stall.getStallId());
        response.setStallNo(stall.getStallNo());
        response.setStallName(stall.getStallName());
        response.setDescription(stall.getDescription());
        response.setLocation(stall.getLocation());
        response.setCategory(stall.getCategory());
        response.setImage(stall.getImage());
        response.setBasePrice(stall. getBasePrice());
        response.setOriginalPrice(stall.getOriginalPrice());
        response.setCurrentHighestBid(stall. getCurrentHighestBid());
        response.setMaxBidders(stall.getMaxBidders());
        response.setStatus(stall.getStatus());
        response.setBiddingStart(stall.getBiddingStart());
        response.setBiddingEnd(stall.getBiddingEnd());

        // Add bid count
        long bidCount = bidRepository.countByStall_StallId(stall.getStallId());
        response.setBidCount(bidCount);

        return response;
    }
}