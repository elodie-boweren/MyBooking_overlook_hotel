package com.MyBooking.room.controller;

import com.MyBooking.room.domain.*;
import com.MyBooking.room.dto.RoomCreateRequestDto;
import com.MyBooking.room.service.RoomService;
import com.MyBooking.auth.domain.User;
import com.MyBooking.common.exception.BusinessRuleException;
import com.MyBooking.common.exception.NotFoundException;
import com.MyBooking.common.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    @Autowired
    private RoomService roomService;

    // ========== CLIENT & ADMIN ENDPOINTS ==========

    /**
     * GET /api/rooms - List/filter rooms with proper pagination (CLIENT & ADMIN ONLY)
     * Supports filtering by: roomType, capacity, price range, status, dates
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public ResponseEntity<Page<Room>> getRooms(
            @RequestParam(required = false) RoomType roomType,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) RoomStatus status,
            @RequestParam(required = false) LocalDate checkIn,
            @RequestParam(required = false) LocalDate checkOut,
            Pageable pageable) {
        
        try {
            Page<Room> rooms;
            
            // If dates are provided, get available rooms for date range WITH PROPER PAGINATION
            if (checkIn != null && checkOut != null) {
                if (roomType != null || minCapacity != null) {
                    // Use filtered availability search with pagination
                    rooms = roomService.getAvailableRoomsForDateRangeWithFilters(
                        checkIn, checkOut, roomType, minCapacity, pageable);
                } else {
                    // Use basic availability search with pagination
                    rooms = roomService.getAvailableRoomsForDateRange(checkIn, checkOut, pageable);
                }
            } else {
                // Use criteria-based filtering with pagination
                rooms = roomService.getRoomsByCriteria(roomType, minCapacity, maxPrice, status, pageable);
            }
            
            return ResponseEntity.ok(rooms);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/rooms/{roomId} - Get room details (CLIENT & ADMIN ONLY)
     */
    @GetMapping("/{roomId}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public ResponseEntity<Room> getRoomById(@PathVariable Long roomId) {
        try {
            Room room = roomService.getRoomById(roomId);
            return ResponseEntity.ok(room);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/rooms/{roomId}/availability - Check room availability (CLIENT & ADMIN ONLY)
     */
    @GetMapping("/{roomId}/availability")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public ResponseEntity<?> getRoomAvailability(
            @PathVariable Long roomId,
            @RequestParam(required = false) LocalDate checkIn,
            @RequestParam(required = false) LocalDate checkOut) {
        
        try {
            Room room = roomService.getRoomById(roomId);
            
            if (checkIn != null && checkOut != null) {
                boolean isAvailable = roomService.isRoomAvailable(roomId, checkIn, checkOut);
                return ResponseEntity.ok(new AvailabilityResponse(roomId, isAvailable, checkIn, checkOut));
            } else {
                String status = roomService.getRoomAvailabilityStatus(roomId);
                return ResponseEntity.ok(new AvailabilityResponse(roomId, status));
            }
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== ADMIN ENDPOINTS ==========

    /**
     * POST /api/rooms - Create new room (ADMIN only)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Room> createRoom(@Valid @RequestBody RoomCreateRequestDto roomRequest) {
        try {
            Room createdRoom = roomService.createRoom(
                roomRequest.getNumber(),
                roomRequest.getRoomType(),
                roomRequest.getPrice(),
                roomRequest.getCurrency(),
                roomRequest.getCapacity(),
                roomRequest.getDescription()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(createdRoom);
        } catch (BusinessRuleException e) {
            e.printStackTrace(); // Log the error for debugging
            throw e; // Let GlobalExceptionHandler handle it with proper error message
        } catch (Exception e) {
            e.printStackTrace(); // Log the error for debugging
            throw e; // Let GlobalExceptionHandler handle it
        }
    }

    /**
     * PUT /api/rooms/{roomId} - Update room (ADMIN only)
     */
    @PutMapping("/{roomId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Room> updateRoom(@PathVariable Long roomId, @Valid @RequestBody Room room) {
        try {
            Room updatedRoom = roomService.updateRoom(
                roomId,
                room.getNumber(),
                room.getRoomType(),
                room.getPrice(),
                room.getCurrency(),
                room.getCapacity(),
                room.getDescription(),
                room.getStatus()
            );
            return ResponseEntity.ok(updatedRoom);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DELETE /api/rooms/{roomId} - Delete room (ADMIN only)
     */
    @DeleteMapping("/{roomId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long roomId) {
        try {
            roomService.deleteRoom(roomId);
            return ResponseEntity.noContent().build();
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== EMPLOYEE ENDPOINTS ==========

    /**
     * GET /api/rooms/employee/search - Search rooms by number/status (EMPLOYEE only)
     */
    @GetMapping("/employee/search")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Page<Room>> searchRooms(
            @RequestParam(required = false) String number,
            @RequestParam(required = false) RoomStatus status,
            @RequestParam(required = false) RoomType roomType,
            Pageable pageable) {
        
        try {
            Page<Room> rooms;
            
            if (number != null && !number.trim().isEmpty()) {
                // Search by room number - return single room as page
                Room room = roomService.getRoomByNumber(number);
                rooms = new PageImpl<>(List.of(room), pageable, 1);
            } else if (status != null) {
                // Search by status with pagination
                rooms = roomService.getRoomsByStatus(status, pageable);
            } else if (roomType != null) {
                // Search by room type with pagination
                rooms = roomService.getRoomsByType(roomType, pageable);
            } else {
                // Return all rooms with pagination
                rooms = roomService.getAllRooms(pageable);
            }
            
            return ResponseEntity.ok(rooms);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PUT /api/employee/rooms/{roomId}/status - Update room status (EMPLOYEE only)
     */
    @PutMapping("/employee/{roomId}/status")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Room> updateRoomStatus(
            @PathVariable Long roomId,
            @RequestBody RoomStatusUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        try {
            User user = roomService.getUserByUsername(principal.getEmail());
            
            // Use the simpler updateRoomStatus method without notes and reason
            Room updatedRoom = roomService.updateRoomStatus(
                roomId,
                request.getNewStatus(),
                user
            );
            
            return ResponseEntity.ok(updatedRoom);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== INNER CLASSES ==========

    public static class AvailabilityResponse {
        private Long roomId;
        private boolean available;
        private String status;
        private LocalDate checkIn;
        private LocalDate checkOut;

        public AvailabilityResponse(Long roomId, boolean available, LocalDate checkIn, LocalDate checkOut) {
            this.roomId = roomId;
            this.available = available;
            this.checkIn = checkIn;
            this.checkOut = checkOut;
        }

        public AvailabilityResponse(Long roomId, String status) {
            this.roomId = roomId;
            this.status = status;
        }

        // Getters and setters
        public Long getRoomId() { return roomId; }
        public void setRoomId(Long roomId) { this.roomId = roomId; }
        public boolean isAvailable() { return available; }
        public void setAvailable(boolean available) { this.available = available; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDate getCheckIn() { return checkIn; }
        public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
        public LocalDate getCheckOut() { return checkOut; }
        public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }
    }

    public static class RoomStatusUpdateRequest {
        private RoomStatus newStatus;
        private String notes;
        private String updateReason;

        // Getters and setters
        public RoomStatus getNewStatus() { return newStatus; }
        public void setNewStatus(RoomStatus newStatus) { this.newStatus = newStatus; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public String getUpdateReason() { return updateReason; }
        public void setUpdateReason(String updateReason) { this.updateReason = updateReason; }
    }
}
