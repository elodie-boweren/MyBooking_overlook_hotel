package com.MyBooking.feedback.controller;

import com.MyBooking.common.security.UserPrincipal;
import com.MyBooking.feedback.dto.*;
import com.MyBooking.feedback.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for feedback reply management
 */
@RestController
@RequestMapping("/api/feedback-replies")
public class FeedbackReplyController {

    private final FeedbackService feedbackService;

    @Autowired
    public FeedbackReplyController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    // ==================== FEEDBACK REPLY ENDPOINTS ====================

    /**
     * Create reply to feedback (admin/employee only)
     */
    @PostMapping("/feedback/{feedbackId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    public ResponseEntity<FeedbackReplyResponseDto> createReply(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long feedbackId,
            @Valid @RequestBody CreateReplyRequestDto request) {
        
        Long adminUserId = principal.getId();
        FeedbackReplyResponseDto response = feedbackService.createFeedbackReplyAsDto(
            feedbackId, adminUserId, request.getMessage());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get reply by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE') or hasRole('CLIENT')")
    public ResponseEntity<FeedbackReplyResponseDto> getReplyById(@PathVariable Long id) {
        FeedbackReplyResponseDto response = feedbackService.getFeedbackReplyByIdAsDto(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Update reply (admin/employee only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    public ResponseEntity<FeedbackReplyResponseDto> updateReply(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateReplyRequestDto request) {
        
        Long adminUserId = principal.getId();
        FeedbackReplyResponseDto response = feedbackService.updateFeedbackReplyAsDto(
            id, adminUserId, request.getMessage());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Delete reply (admin/employee only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    public ResponseEntity<Void> deleteReply(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        
        Long adminUserId = principal.getId();
        feedbackService.deleteReply(id, adminUserId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get all replies for a feedback
     */
    @GetMapping("/feedback/{feedbackId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE') or hasRole('CLIENT')")
    public ResponseEntity<Page<FeedbackReplyResponseDto>> getRepliesByFeedback(
            @PathVariable Long feedbackId,
            Pageable pageable) {
        
        Page<FeedbackReplyResponseDto> response = feedbackService.getFeedbackRepliesByFeedbackIdAsDto(feedbackId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get my replies (admin/employee only)
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    public ResponseEntity<Page<FeedbackReplyResponseDto>> getMyReplies(
            @AuthenticationPrincipal UserPrincipal principal,
            Pageable pageable) {
        
        Long adminUserId = principal.getId();
        Page<FeedbackReplyResponseDto> response = feedbackService.getFeedbackRepliesByAdminEmployeeIdAsDto(adminUserId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all replies (admin only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<FeedbackReplyResponseDto>> getAllReplies(Pageable pageable) {
        Page<FeedbackReplyResponseDto> response = feedbackService.getAllFeedbackRepliesAsDto(pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get recent replies (admin only)
     */
    @GetMapping("/recent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<FeedbackReplyResponseDto>> getRecentReplies(
            @RequestParam(defaultValue = "7") int days,
            Pageable pageable) {
        
        Page<FeedbackReplyResponseDto> response = feedbackService.getRecentFeedbackRepliesAsDto(days, pageable);
        return ResponseEntity.ok(response);
    }
}
