package com.MyBooking.reservation.controller;

import com.MyBooking.common.security.UserPrincipal;
import com.MyBooking.reservation.dto.*;
import com.MyBooking.reservation.domain.ReservationStatus;
import com.MyBooking.reservation.service.ReservationService;
import com.MyBooking.common.exception.BusinessRuleException;
import com.MyBooking.common.exception.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for client-specific reservation operations
 * Handles reservation creation, viewing, updating, and cancellation by clients
 */
@RestController
@RequestMapping("/api/client/reservations")
public class ClientReservationController {

    @Autowired
    private ReservationService reservationService;

    /**
     * Create a new reservation
     * POST /api/client/reservations
     */
    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ReservationResponseDto> createReservation(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReservationCreateRequestDto request) {
        Long clientId = principal.getId();

        // Use points redemption method if points are being used
        ReservationResponseDto response;
        if (request.getPointsUsed() != null && request.getPointsUsed() > 0) {
            response = reservationService.createReservationWithPoints(request, clientId);
        } else {
            response = reservationService.createReservation(request, clientId);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get my reservations with pagination
     * GET /api/client/reservations/my
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Page<ReservationResponseDto>> getMyReservations(
            @AuthenticationPrincipal UserPrincipal principal,
            Pageable pageable) {
        Long clientId = principal.getId();
        Page<ReservationResponseDto> reservations = reservationService.getReservationsByClientId(clientId, pageable);
        return ResponseEntity.ok(reservations);
    }

    /**
     * Search my reservations with criteria
     * GET /api/client/reservations/search
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Page<ReservationResponseDto>> searchMyReservations(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) String checkInFrom,
            @RequestParam(required = false) String checkInTo,
            @RequestParam(required = false) String checkOutFrom,
            @RequestParam(required = false) String checkOutTo,
            Pageable pageable) {
        Long clientId = principal.getId();

        ReservationSearchCriteriaDto criteria = new ReservationSearchCriteriaDto();
        criteria.setClientId(clientId);
        criteria.setRoomId(roomId);
        criteria.setStatus(status);

        if (checkInFrom != null && !checkInFrom.trim().isEmpty()) {
            criteria.setCheckInFrom(java.time.LocalDate.parse(checkInFrom));
        }
        if (checkInTo != null && !checkInTo.trim().isEmpty()) {
            criteria.setCheckInTo(java.time.LocalDate.parse(checkInTo));
        }
        if (checkOutFrom != null && !checkOutFrom.trim().isEmpty()) {
            criteria.setCheckOutFrom(java.time.LocalDate.parse(checkOutFrom));
        }
        if (checkOutTo != null && !checkOutTo.trim().isEmpty()) {
            criteria.setCheckOutTo(java.time.LocalDate.parse(checkOutTo));
        }

        Page<ReservationResponseDto> reservations = reservationService.searchReservationsAsDto(criteria, pageable);
        return ResponseEntity.ok(reservations);
    }

    /**
     * Get reservation by ID (client can only view their own reservations)
     * GET /api/client/reservations/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ReservationResponseDto> getReservation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            Long clientId = principal.getId();
            ReservationResponseDto reservation = reservationService.getReservationByIdAndClientId(id, clientId);
            return ResponseEntity.ok(reservation);
        } catch (BusinessRuleException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * Update my reservation
     * PUT /api/client/reservations/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ReservationResponseDto> updateReservation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReservationUpdateRequestDto request) {
        Long clientId = principal.getId();
        ReservationResponseDto updatedReservation = reservationService.updateReservation(id, request, clientId);
        return ResponseEntity.ok(updatedReservation);
    }

    /**
     * Cancel my reservation
     * DELETE /api/client/reservations/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Void> cancelReservation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long clientId = principal.getId();
        reservationService.cancelReservation(id, clientId);
        return ResponseEntity.noContent().build();
    }
}
