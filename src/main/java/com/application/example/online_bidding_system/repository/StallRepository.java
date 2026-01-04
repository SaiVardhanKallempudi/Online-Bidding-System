package com.application.example.online_bidding_system.repository;

import com.application. example.online_bidding_system.entity.Stall;
import com.application.example. online_bidding_system.entity.StallStatus;
import org. springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util. Optional;

@Repository
public interface StallRepository extends JpaRepository<Stall, Long> {

    // Find by stall number (supports both int and Integer)
    Optional<Stall> findByStallNo(Integer stallNo);

    // Find by status
    List<Stall> findByStatus(StallStatus status);

    // Count by status
    long countByStatus(StallStatus status);

    // Find all active stalls
    @Query("SELECT s FROM Stall s WHERE s.status = 'ACTIVE'")
    List<Stall> findAllActiveStalls();

    // Find by max bidders greater than
    List<Stall> findByMaxBiddersGreaterThan(int minBidders);

    // Find by category
    List<Stall> findByCategory(String category);

    // Find by stall name containing (search)
    List<Stall> findByStallNameContainingIgnoreCase(String name);

    // Find by created by user
    List<Stall> findByCreatedBy_StudentId(Long studentId);

    // Check if stall number exists
    boolean existsByStallNo(Integer stallNo);
}