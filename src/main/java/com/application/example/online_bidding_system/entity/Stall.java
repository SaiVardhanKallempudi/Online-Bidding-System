package com.application. example.online_bidding_system.entity;

import jakarta.persistence.*;
import lombok. Getter;
import lombok.Setter;

import java.math. BigDecimal;
import java.time. LocalDateTime;
import java.util. List;

@Entity
@Table(name = "stalls")
@Getter
@Setter
public class Stall {
    @Id
    @GeneratedValue(strategy = GenerationType. IDENTITY)
    private Long stallId;

    private Integer stallNo;

    @Column(nullable = false)
    private String stallName;

    @Column(length = 1000)
    private String description;

    private String category;

    private String location;

    private String image;

    // Base price - minimum starting bid
    @Column(nullable = false)
    private BigDecimal basePrice = BigDecimal. ZERO;

    // Original price - price at which stall auto-closes
    @Column(nullable = false)
    private BigDecimal originalPrice;

    // Current highest bid
    private BigDecimal currentHighestBid = BigDecimal. ZERO;

    private Integer maxBidders;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StallStatus status = StallStatus. AVAILABLE;

    private LocalDateTime biddingStart;

    private LocalDateTime biddingEnd;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    @OneToMany(mappedBy = "stall", fetch = FetchType. LAZY, cascade = CascadeType.ALL)
    private List<Bid> bids;

    @OneToOne
    @JoinColumn(name = "result_id")
    private BiddingResult result;

    @OneToMany(mappedBy = "stall", fetch = FetchType. LAZY, cascade = CascadeType. ALL)
    private List<Comment> comments;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (currentHighestBid == null) {
            currentHighestBid = BigDecimal. ZERO;
        }
        if (basePrice == null) {
            basePrice = BigDecimal. ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime. now();
    }
}