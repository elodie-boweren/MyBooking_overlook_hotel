package com.MyBooking.event.controller;

import com.MyBooking.common.security.UserPrincipal;
import com.MyBooking.event.dto.EventBookingCreateRequestDto;
import com.MyBooking.event.dto.EventBookingResponseDto;
import com.MyBooking.event.domain.EventBookingStatus;
import com.MyBooking.event.service.EventService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/event-bookings")
public class EventBookingController {
    
    private final EventService eventService;
    
    public EventBookingController(EventService eventService) {
        this.eventService = eventService;
    }
    
    /**
     * Create event booking
     * Access: CLIENT only
     */
    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<EventBookingResponseDto> createEventBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody EventBookingCreateRequestDto request) {
        Long clientId = principal.getId();
        EventBookingResponseDto booking = eventService.createEventBookingAsDto(request, clientId);
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }
    
    /**
     * Get my event bookings
     * Access: CLIENT only
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Page<EventBookingResponseDto>> getMyEventBookings(
            @AuthenticationPrincipal UserPrincipal principal,
            Pageable pageable) {
        Long clientId = principal.getId();
        Page<EventBookingResponseDto> bookings = eventService.getEventBookingsByUserAsDto(clientId, pageable);
        return ResponseEntity.ok(bookings);
    }
    
    /**
     * Get event booking details
     * Access: CLIENT (own bookings only)
     */
    @GetMapping("/{bookingId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<EventBookingResponseDto> getEventBookingById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long bookingId) {
        Long clientId = principal.getId();
        EventBookingResponseDto booking = eventService.getEventBookingByIdAsDto(bookingId, clientId);
        return ResponseEntity.ok(booking);
    }
    
    /**
     * Cancel event booking
     * Access: CLIENT (own bookings only)
     */
    @PutMapping("/{bookingId}/cancel")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<EventBookingResponseDto> cancelEventBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long bookingId,
            @RequestParam(required = false) String reason) {
        Long clientId = principal.getId();
        EventBookingResponseDto booking = eventService.cancelEventBookingAsDto(bookingId, clientId, reason);
        return ResponseEntity.ok(booking);
    }
    
    /**
     * Get all event bookings (admin view)
     * Access: ADMIN only
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<EventBookingResponseDto>> getAllEventBookings(Pageable pageable) {
        Page<EventBookingResponseDto> bookings = eventService.getAllEventBookingsAsDto(pageable);
        return ResponseEntity.ok(bookings);
    }
    
    /**
     * Get event booking details (admin view)
     * Access: ADMIN only
     */
    @GetMapping("/admin/{bookingId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventBookingResponseDto> getEventBookingByIdAdmin(@PathVariable Long bookingId) {
        EventBookingResponseDto booking = eventService.getEventBookingByIdAsDto(bookingId);
        return ResponseEntity.ok(booking);
    }
    
    /**
     * Update event booking status
     * Access: ADMIN only
     */
    @PutMapping("/{bookingId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventBookingResponseDto> updateEventBookingStatus(
            @PathVariable Long bookingId,
            @RequestParam EventBookingStatus status) {
        EventBookingResponseDto booking = eventService.updateEventBookingStatusAsDto(bookingId, status);
        return ResponseEntity.ok(booking);
    }
}
