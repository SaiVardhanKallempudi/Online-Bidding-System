package com.application.example.online_bidding_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class BiddingResult {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long resultId;

        @OneToOne
        @JoinColumn(name = "stall_id", unique = true)
        private Stall stall;

        @ManyToOne
        @JoinColumn(name = "winner_id")
        private User winner;

        private BigDecimal winningPrice;
        private LocalDateTime resultTime;

}
