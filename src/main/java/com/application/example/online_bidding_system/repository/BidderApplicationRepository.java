package com. application.example.online_bidding_system.repository;

import com.application.example.online_bidding_system.entity. BidderApplication;
import com.application.example.online_bidding_system. entity.Status;
import com. application.example.online_bidding_system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype. Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BidderApplicationRepository extends JpaRepository<BidderApplication, Long> {
    Optional<BidderApplication> findByUser(User user);
    Optional<BidderApplication> findByUser_StudentId(Long studentId);
    Optional<BidderApplication> findByUser_StudentEmail(String studentEmail);
    List<BidderApplication> findByStatus(Status status);
    long countByStatus(Status status);
    boolean existsByUser_StudentId(Long studentId);
}