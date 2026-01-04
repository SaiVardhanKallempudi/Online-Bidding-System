package com.application. example.online_bidding_system.repository;

import com. application.example.online_bidding_system.entity.Bid;
import com. application.example.online_bidding_system.entity. Stall;
import com.application.example. online_bidding_system.entity.User;
import org. springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java. util.Optional;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    // Find bids by stall
    List<Bid> findByStall(Stall stall);

    // Find highest bid for a stall
    Optional<Bid> findTopByStallOrderByBiddedPriceDesc(Stall stall);

    // Find bids by stall ID ordered by price descending
    List<Bid> findByStall_StallIdOrderByBiddedPriceDesc(Long stallId);


    // Find bids by bidder ordered by time descending
    List<Bid> findByBidderOrderByBidTimeDesc(User bidder);

    // Find bids by bidder's student ID
    List<Bid> findByBidder_StudentId(Long studentId);

    // Count bids by bidder
    long countByBidder(User bidder);

    // Count bids by bidder's student ID
    long countByBidder_StudentId(Long studentId);

    // Count bids by stall
    long countByStall(Stall stall);

    // Count bids by stall ID
    long countByStall_StallId(Long stallId);
}