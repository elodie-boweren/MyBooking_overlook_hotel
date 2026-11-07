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
 * Controller for feedback management (Client access)
 */
@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Autowired
    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    // ==================== CLIENT FEEDBACK ENDPOINTS ====================

    /**
     * Create new feedback for a reservation
     */
    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<FeedbackResponseDto> createFeedback(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateFeedbackRequestDto request) {
        
        Long userId = principal.getId();
        FeedbackResponseDto response = feedbackService.createFeedbackAsDto(
            userId, request.getReservationId(), request.getRating(), request.getComment());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get feedback by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT') or hasRole('EMPLOYEE') or hasRole('ADMIN')")
    public ResponseEntity<FeedbackResponseDto> getFeedbackById(@PathVariable Long id) {
        FeedbackResponseDto response = feedbackService.getFeedbackByIdAsDto(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Update feedback (only by the user who created it)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<FeedbackResponseDto> updateFeedback(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateFeedbackRequestDto request) {
        
        Long userId = principal.getId();
        FeedbackResponseDto response = feedbackService.updateFeedbackAsDto(
            id, userId, request.getRating(), request.getComment());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Delete feedback (only by the user who created it)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Void> deleteFeedback(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        
        Long userId = principal.getId();
        feedbackService.deleteFeedback(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get my feedbacks (paginated)
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Page<FeedbackResponseDto>> getMyFeedbacks(
            @AuthenticationPrincipal UserPrincipal principal,
            Pageable pageable) {
        
        Long userId = principal.getId();
        Page<FeedbackResponseDto> response = feedbackService.getFeedbacksByUserIdAsDto(userId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get feedbacks for a specific reservation
     */
    @GetMapping("/reservation/{reservationId}")
    @PreAuthorize("hasRole('CLIENT') or hasRole('EMPLOYEE') or hasRole('ADMIN')")
    public ResponseEntity<Page<FeedbackResponseDto>> getFeedbacksByReservation(
            @PathVariable Long reservationId,
            Pageable pageable) {
        
        Page<FeedbackResponseDto> response = feedbackService.getFeedbacksByReservationIdAsDto(reservationId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Search feedbacks with criteria
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('CLIENT') or hasRole('EMPLOYEE') or hasRole('ADMIN')")
    public ResponseEntity<Page<FeedbackResponseDto>> searchFeedbacks(
            @ModelAttribute FeedbackSearchCriteriaDto criteria,
            Pageable pageable) {
        
        Page<FeedbackResponseDto> response = feedbackService.searchFeedbacksAsDto(criteria, pageable);
        return ResponseEntity.ok(response);
    }
}
