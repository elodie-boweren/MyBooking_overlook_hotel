package com.MyBooking.reservation.controller;

import com.MyBooking.reservation.dto.*;
import com.MyBooking.reservation.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for employee-specific reservation operations
 * Handles read-only reservation operations by employees
 */
@RestController
@RequestMapping("/api/employee/reservations")
public class EmployeeReservationController {

    @Autowired
    private ReservationService reservationService;

    /**
     * Search reservations with filtering (read-only)
     * GET /api/employee/reservations/search
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Page<ReservationResponseDto>> searchReservations(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String checkInFrom,
            @RequestParam(required = false) String checkInTo,
            @RequestParam(required = false) String checkOutFrom,
            @RequestParam(required = false) String checkOutTo,
            Pageable pageable) {
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

        Page<ReservationResponseDto> reservations = reservationService.searchReservationsAsDto(criteria, pageable);
        return ResponseEntity.ok(reservations);
    }

    /**
     * Get reservation details (read-only)
     * GET /api/employee/reservations/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ReservationResponseDto> getReservation(@PathVariable Long id) {
        ReservationResponseDto reservation = reservationService.getReservationByIdAsDto(id);
        return ResponseEntity.ok(reservation);
    }

    /**
     * Get reservations by client ID (read-only)
     * GET /api/employee/reservations/client/{clientId}
     */
    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Page<ReservationResponseDto>> getReservationsByClient(
            @PathVariable Long clientId,
            Pageable pageable) {
        Page<ReservationResponseDto> reservations = reservationService.getReservationsByClientId(clientId, pageable);
        return ResponseEntity.ok(reservations);
    }

    /**
     * Get reservations by room ID (read-only)
     * GET /api/employee/reservations/room/{roomId}
     */
    @GetMapping("/room/{roomId}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Page<ReservationResponseDto>> getReservationsByRoom(
            @PathVariable Long roomId,
            Pageable pageable) {
        Page<ReservationResponseDto> reservations = reservationService.getReservationsByRoomId(roomId, pageable);
        return ResponseEntity.ok(reservations);
    }

    /**
     * Get reservations by status (read-only)
     * GET /api/employee/reservations/status/{status}
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Page<ReservationResponseDto>> getReservationsByStatus(
            @PathVariable String status,
            Pageable pageable) {
        com.MyBooking.reservation.domain.ReservationStatus reservationStatus =
            com.MyBooking.reservation.domain.ReservationStatus.valueOf(status);
        Page<ReservationResponseDto> reservations = reservationService.getReservationsByStatus(reservationStatus, pageable);
        return ResponseEntity.ok(reservations);
    }

    /**
     * Get reservations for a specific date range (read-only)
     * GET /api/employee/reservations/date-range
     */
    @GetMapping("/date-range")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Page<ReservationResponseDto>> getReservationsByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate,
            Pageable pageable) {
        java.time.LocalDate start = java.time.LocalDate.parse(startDate);
        java.time.LocalDate end = java.time.LocalDate.parse(endDate);
        Page<ReservationResponseDto> reservations = reservationService.getReservationsByDateRange(start, end, pageable);
        return ResponseEntity.ok(reservations);
    }
}
