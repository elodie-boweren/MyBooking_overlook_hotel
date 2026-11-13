package com.MyBooking.reservation.controller;

import com.MyBooking.reservation.dto.*;
import com.MyBooking.reservation.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for admin-specific reservation operations
 * Handles all reservation management operations by administrators
 */
@RestController
@RequestMapping("/api/admin/reservations")
public class AdminReservationController {

    @Autowired
    private ReservationService reservationService;

    /**
     * Get all reservations with filtering and pagination
     * GET /api/admin/reservations
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ReservationResponseDto>> getAllReservations(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String checkInFrom,
            @RequestParam(required = false) String checkInTo,
            @RequestParam(required = false) String checkOutFrom,
            @RequestParam(required = false) String checkOutTo,
            Pageable pageable) {
        // Check if any filters are provided
        boolean hasFilters = clientId != null || roomId != null || status != null ||
                           checkInFrom != null || checkInTo != null ||
                           checkOutFrom != null || checkOutTo != null;

        Page<ReservationResponseDto> reservations;

        if (hasFilters) {
            // Use admin search method when filters are provided
            ReservationSearchCriteriaDto criteria = new ReservationSearchCriteriaDto();
            criteria.setClientId(clientId);
            criteria.setRoomId(roomId);
            if (status != null) {
                criteria.setStatus(com.MyBooking.reservation.domain.ReservationStatus.valueOf(status));
            }
            if (checkInFrom != null) {
                criteria.setCheckInFrom(java.time.LocalDate.parse(checkInFrom));
            }
            if (checkInTo != null) {
                criteria.setCheckInTo(java.time.LocalDate.parse(checkInTo));
            }
            if (checkOutFrom != null) {
                criteria.setCheckOutFrom(java.time.LocalDate.parse(checkOutFrom));
            }
            if (checkOutTo != null) {
                criteria.setCheckOutTo(java.time.LocalDate.parse(checkOutTo));
            }

            reservations = reservationService.searchAllReservationsAsDto(criteria, pageable);
        } else {
            // Use getAllReservationsAsDto when no filters are provided
            reservations = reservationService.getAllReservationsAsDto(pageable);
        }

        return ResponseEntity.ok(reservations);
    }

    /**
     * Get reservation by ID (admin can view any reservation)
     * GET /api/admin/reservations/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReservationResponseDto> getReservation(@PathVariable Long id) {
        ReservationResponseDto reservation = reservationService.getReservationByIdAsDto(id);
        return ResponseEntity.ok(reservation);
    }

    /**
     * Update any reservation
     * PUT /api/admin/reservations/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReservationResponseDto> updateReservation(
            @PathVariable Long id,
            @Valid @RequestBody ReservationUpdateRequestDto request) {
        ReservationResponseDto updatedReservation = reservationService.updateReservation(id, request);
        return ResponseEntity.ok(updatedReservation);
    }

    /**
     * Cancel any reservation
     * DELETE /api/admin/reservations/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cancelReservation(@PathVariable Long id) {
        reservationService.cancelReservation(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reassign reservation to different room
     * POST /api/admin/reservations/{id}/reassign
     */
    @PostMapping("/{id}/reassign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReservationResponseDto> reassignReservation(
            @PathVariable Long id,
            @RequestParam Long newRoomId) {
        ReservationResponseDto updatedReservation = reservationService.reassignReservation(id, newRoomId);
        return ResponseEntity.ok(updatedReservation);
    }

    /**
     * Get reservations by client ID
     * GET /api/admin/reservations/client/{clientId}
     */
    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ReservationResponseDto>> getReservationsByClient(
            @PathVariable Long clientId,
            Pageable pageable) {
        Page<ReservationResponseDto> reservations = reservationService.getReservationsByClientId(clientId, pageable);
        return ResponseEntity.ok(reservations);
    }

    /**
     * Get reservations by room ID
     * GET /api/admin/reservations/room/{roomId}
     */
    @GetMapping("/room/{roomId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ReservationResponseDto>> getReservationsByRoom(
            @PathVariable Long roomId,
            Pageable pageable) {
        Page<ReservationResponseDto> reservations = reservationService.getReservationsByRoomId(roomId, pageable);
        return ResponseEntity.ok(reservations);
    }
}
