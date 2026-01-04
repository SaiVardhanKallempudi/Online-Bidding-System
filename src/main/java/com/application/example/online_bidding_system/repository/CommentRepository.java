package com. application.example.online_bidding_system.repository;

import com.application.example.online_bidding_system.entity. Comment;
import com.application.example. online_bidding_system.entity. Stall;
import org.springframework.data. jpa.repository. JpaRepository;
import org.springframework. stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByStallAndIsDeletedFalseOrderByCreatedAtDesc(Stall stall);
    List<Comment> findByStall_StallIdAndIsDeletedFalseOrderByCreatedAtDesc(Long stallId);
    List<Comment> findByUser_StudentIdAndIsDeletedFalseOrderByCreatedAtDesc(Long studentId);
    long countByStall_StallId(Long stallId);
}