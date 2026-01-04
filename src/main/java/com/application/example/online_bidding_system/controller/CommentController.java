package com.application.example.online_bidding_system. controller;

import com.application.example. online_bidding_system.dto.request.CommentRequest;
import com.application.example.online_bidding_system. dto.response.CommentResponse;
import com.application.example.online_bidding_system.entity. Comment;
import com.application.example. online_bidding_system.entity. Stall;
import com.application.example. online_bidding_system.entity.User;
import com.application.example.online_bidding_system.exception. BadRequestException;
import com.application. example.online_bidding_system.exception.ResourceNotFoundException;
import com.application. example.online_bidding_system.exception.UnauthorizedException;
import com. application.example.online_bidding_system.repository.CommentRepository;
import com.application.example. online_bidding_system.repository.StallRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory. annotation.Autowired;
import org. springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework. security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java. util.List;
import java.util. Map;
import java.util.stream. Collectors;

@RestController
@RequestMapping("/api/comments")
@CrossOrigin(origins = "http://localhost:4200")
public class CommentController {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private StallRepository stallRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Add a comment to a stall
     */
    @PostMapping
    public ResponseEntity<CommentResponse> addComment(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CommentRequest request) {

        if (user == null) {
            throw new UnauthorizedException("You must be logged in to comment");
        }

        Stall stall = stallRepository.findById(request.getStallId())
                .orElseThrow(() -> new ResourceNotFoundException("Stall", "id", request.getStallId()));

        if (request.getCommentText() == null || request.getCommentText().trim().isEmpty()) {
            throw new BadRequestException("Comment text cannot be empty");
        }

        Comment comment = new Comment();
        comment.setStall(stall);
        comment.setUser(user);
        comment.setCommentText(request.getCommentText().trim());

        commentRepository.save(comment);

        CommentResponse response = mapToResponse(comment);

        // Broadcast comment to all users watching this stall via WebSocket
        messagingTemplate.convertAndSend(
                "/topic/stall/" + request.getStallId() + "/comments",
                response
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get all comments for a stall
     */
    @GetMapping("/stall/{stallId}")
    public ResponseEntity<List<CommentResponse>> getCommentsByStall(@PathVariable Long stallId) {
        // Verify stall exists
        if (! stallRepository.existsById(stallId)) {
            throw new ResourceNotFoundException("Stall", "id", stallId);
        }

        List<Comment> comments = commentRepository
                .findByStall_StallIdAndIsDeletedFalseOrderByCreatedAtDesc(stallId);

        List<CommentResponse> responses = comments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Delete a comment (only by the comment owner)
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Map<String, Object>> deleteComment(
            @AuthenticationPrincipal User user,
            @PathVariable Long commentId) {

        if (user == null) {
            throw new UnauthorizedException("You must be logged in to delete comments");
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        // Only allow owner to delete
        if (! comment.getUser().getStudentId().equals(user.getStudentId())) {
            throw new UnauthorizedException("You can only delete your own comments");
        }

        comment.setDeleted(true);
        commentRepository.save(comment);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Comment deleted successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Get comment count for a stall
     */
    @GetMapping("/stall/{stallId}/count")
    public ResponseEntity<Map<String, Long>> getCommentCount(@PathVariable Long stallId) {
        // Verify stall exists
        if (! stallRepository.existsById(stallId)) {
            throw new ResourceNotFoundException("Stall", "id", stallId);
        }

        long count = commentRepository.countByStall_StallId(stallId);
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    /**
     * Get comments by user
     */
    @GetMapping("/my-comments")
    public ResponseEntity<List<CommentResponse>> getMyComments(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new UnauthorizedException("You must be logged in to view your comments");
        }

        List<Comment> comments = commentRepository
                .findByUser_StudentIdAndIsDeletedFalseOrderByCreatedAtDesc(user. getStudentId());

        List<CommentResponse> responses = comments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    private CommentResponse mapToResponse(Comment comment) {
        CommentResponse response = new CommentResponse();
        response.setCommentId(comment.getCommentId());
        response.setStallId(comment. getStall().getStallId());
        response.setUserId(comment.getUser().getStudentId());
        response.setUserName(comment.getUser().getStudentName());
        response.setUserProfilePicture(comment.getUser().getProfilePicture());
        response.setCommentText(comment.getCommentText());
        response.setCreatedAt(comment.getCreatedAt());
        return response;
    }
}