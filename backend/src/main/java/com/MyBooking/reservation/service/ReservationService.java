// Service responsibilities: Reservation Management - Create, update, cancel reservations
// Availability Validation - Check room availability and prevent overlaps
// Business Rules - Pricing, dates, guest limits, status transitions
// Integration - Connect with RoomService, AuthService, and other services
// Guest Management - Handle guest information and preferences

package com.MyBooking.reservation.service;

import com.MyBooking.reservation.domain.Reservation;
import com.MyBooking.reservation.domain.ReservationStatus;
import com.MyBooking.reservation.repository.ReservationRepository;
import com.MyBooking.reservation.dto.*;
import com.MyBooking.room.domain.Room;
import com.MyBooking.room.domain.RoomStatus;
import com.MyBooking.room.repository.RoomRepository;
import com.MyBooking.room.service.RoomService;
import com.MyBooking.auth.domain.User;
import com.MyBooking.auth.domain.Role;
import com.MyBooking.auth.repository.UserRepository;
import com.MyBooking.loyalty.service.LoyaltyService;
import com.MyBooking.common.exception.BusinessRuleException;
import com.MyBooking.common.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReservationService {

    @Autowired
    private ReservationRepository reservationRepository;
    
    @Autowired
    private RoomRepository roomRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoomService roomService;

    @Autowired
    private LoyaltyService loyaltyService;

    // ========== RESERVATION MANAGEMENT ==========

    /**
     * Create a new reservation with comprehensive validation
     */
    public Reservation createReservation(Long roomId, Long clientId, LocalDate checkIn, 
                                       LocalDate checkOut, Integer numberOfGuests, String currency) {
        
        // Validate inputs
        validateReservationInputs(checkIn, checkOut, numberOfGuests, currency);
        
        // Get and validate room with pessimistic write lock to serialize concurrent bookings
        Room room = roomRepository.findByIdForUpdate(roomId)
            .orElseThrow(() -> new NotFoundException("Room not found with ID: " + roomId));
        
        // Get and validate client
        User client = userRepository.findById(clientId)
            .orElseThrow(() -> new NotFoundException("Client not found with ID: " + clientId));
        
        // Validate room capacity
        validateRoomCapacity(room, numberOfGuests);
        
        // Validate room operational status (OUT_OF_SERVICE cannot be booked)
        validateRoomOperationalStatus(room);
        
        // Check room availability
        checkRoomAvailability(roomId, checkIn, checkOut);
        
        // Calculate total price
        BigDecimal totalPrice = calculateTotalPrice(room, checkIn, checkOut, numberOfGuests);
        
        // Create reservation
        Reservation reservation = new Reservation();
        reservation.setCheckIn(checkIn);
        reservation.setCheckOut(checkOut);
        reservation.setNumberOfGuests(numberOfGuests);
        reservation.setTotalPrice(totalPrice);
        reservation.setCurrency(currency);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setClient(client);
        reservation.setRoom(room);
        
        Reservation savedReservation = reservationRepository.save(reservation);
        
        // Process loyalty points for the confirmed reservation
        try {
            loyaltyService.processReservationPoints(savedReservation.getId());
        } catch (Exception e) {
            // Log the error but don't fail the reservation creation
            System.err.println("Failed to process loyalty points for reservation " + savedReservation.getId() + ": " + e.getMessage());
        }
        
        return savedReservation;
    }

    /**
     * Create a new reservation with loyalty points redemption
     */
    public Reservation createReservationWithPoints(Long clientId, Long roomId, LocalDate checkIn, 
                                                 LocalDate checkOut, Integer numberOfGuests, 
                                                 String currency, Integer pointsToRedeem) {
        // Validate inputs
        if (clientId == null || roomId == null) {
            throw new BusinessRuleException("All reservation parameters are required");
        }
        validateReservationInputs(checkIn, checkOut, numberOfGuests, currency);

        // Get and validate room with pessimistic write lock to serialize concurrent bookings
        Room room = roomRepository.findByIdForUpdate(roomId)
            .orElseThrow(() -> new NotFoundException("Room not found with ID: " + roomId));

        // Validate room capacity
        validateRoomCapacity(room, numberOfGuests);
        
        // Validate room operational status
        validateRoomOperationalStatus(room);

        // Check room availability (INV-RES-001)
        checkRoomAvailability(roomId, checkIn, checkOut);

        // Get and validate client
        User client = userRepository.findById(clientId)
            .orElseThrow(() -> new NotFoundException("Client not found with ID: " + clientId));

        // Calculate base price
        BigDecimal basePrice = calculateTotalPrice(room, checkIn, checkOut, numberOfGuests);
        
        // Handle points redemption
        Integer actualPointsUsed = 0;
        BigDecimal pointsDiscount = BigDecimal.ZERO;
        BigDecimal finalPrice = basePrice;
        
        if (pointsToRedeem != null && pointsToRedeem > 0) {
            // Lock account and validate points redemption to avoid stale L1-cache reads before redeemPoints
            loyaltyService.validateReservationPointsRedemptionForUpdate(clientId, pointsToRedeem, basePrice);
            
            // Calculate discount
            pointsDiscount = loyaltyService.calculatePointsDiscountAmount(pointsToRedeem);
            finalPrice = basePrice.subtract(pointsDiscount).max(BigDecimal.ZERO);
            actualPointsUsed = pointsToRedeem;
        }
        
        // Create reservation
        Reservation reservation = new Reservation();
        reservation.setCheckIn(checkIn);
        reservation.setCheckOut(checkOut);
        reservation.setNumberOfGuests(numberOfGuests);
        reservation.setTotalPrice(finalPrice);
        reservation.setCurrency(currency);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setClient(client);
        reservation.setRoom(room);
        reservation.setPointsUsed(actualPointsUsed);
        reservation.setPointsDiscount(pointsDiscount);
        
        Reservation savedReservation = reservationRepository.save(reservation);
        
        // Redeem points if any were used
        if (actualPointsUsed > 0) {
            loyaltyService.redeemPoints(clientId, actualPointsUsed, 
                "Points redeemed for reservation #" + savedReservation.getId());
        }
        
        // Process loyalty points for the confirmed reservation (earn points for the final price)
        try {
            loyaltyService.processReservationPoints(savedReservation.getId());
        } catch (Exception e) {
            // Log the error but don't fail the reservation creation
            System.err.println("Failed to process loyalty points for reservation " + savedReservation.getId() + ": " + e.getMessage());
        }
        
        return savedReservation;
    }

    /**
     * Calculate final price with points discount
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateFinalPriceWithPoints(Long roomId, LocalDate checkIn, 
                                                  LocalDate checkOut, Integer numberOfGuests, 
                                                  Long clientId, Integer pointsToRedeem) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new BusinessRuleException("Check-out date must be after check-in date");
        }
        if (numberOfGuests == null || numberOfGuests < 1) {
            throw new BusinessRuleException("Number of guests must be at least 1");
        }
        // Get room
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new NotFoundException("Room not found with ID: " + roomId));

        // Calculate base price
        BigDecimal basePrice = calculateTotalPrice(room, checkIn, checkOut, numberOfGuests);
        
        // Calculate points discount if points are provided
        if (pointsToRedeem != null && pointsToRedeem > 0) {
            // Validate points redemption
            loyaltyService.validateReservationPointsRedemption(clientId, pointsToRedeem, basePrice);
            
            // Calculate discount
            BigDecimal pointsDiscount = loyaltyService.calculatePointsDiscountAmount(pointsToRedeem);
            return basePrice.subtract(pointsDiscount).max(BigDecimal.ZERO);
        }
        
        return basePrice;
    }

    /**
     * Get maximum redeemable points for a reservation
     */
    @Transactional(readOnly = true)
    public Integer getMaxRedeemablePoints(Long roomId, LocalDate checkIn, LocalDate checkOut, 
                                        Integer numberOfGuests, Long clientId) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new BusinessRuleException("Check-out date must be after check-in date");
        }
        if (numberOfGuests == null || numberOfGuests < 1) {
            throw new BusinessRuleException("Number of guests must be at least 1");
        }
        // Get room
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new NotFoundException("Room not found with ID: " + roomId));

        // Calculate base price
        BigDecimal basePrice = calculateTotalPrice(room, checkIn, checkOut, numberOfGuests);
        
        // Get maximum redeemable points
        return loyaltyService.calculateMaxRedeemablePoints(clientId, basePrice);
    }

    /**
     * Get reservation by ID
     */
    @Transactional(readOnly = true)
    public Reservation getReservationById(Long reservationId) {
        return reservationRepository.findById(reservationId)
            .orElseThrow(() -> new NotFoundException("Reservation not found with ID: " + reservationId));
    }

    // ========== VALIDATION METHODS ==========

    /**
     * Validate reservation inputs
     */
    private void validateReservationInputs(LocalDate checkIn, LocalDate checkOut, 
                                         Integer numberOfGuests, String currency) {
        if (checkIn == null || checkOut == null || numberOfGuests == null || currency == null) {
            throw new BusinessRuleException("All reservation parameters are required");
        }

        LocalDate today = LocalDate.now();
        
        // Check-in date validation
        if (checkIn.isBefore(today)) {
            throw new BusinessRuleException("Check-in date cannot be in the past");
        }
        
        // Check-out date validation
        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new BusinessRuleException("Check-out date must be after check-in date");
        }
        
        // Maximum stay validation (e.g., 30 days)
        long daysBetween = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (daysBetween > 30) {
            throw new BusinessRuleException("Maximum stay is 30 days");
        }
        
        // Guest count validation
        if (numberOfGuests < 1 || numberOfGuests > 10) {
            throw new BusinessRuleException("Number of guests must be between 1 and 10");
        }
        
        // Currency validation
        if (currency == null || currency.length() != 3) {
            throw new BusinessRuleException("Currency must be a 3-character code (e.g., USD, EUR)");
        }
    }

    /**
     * Validate room capacity
     */
    private void validateRoomCapacity(Room room, Integer numberOfGuests) {
        if (numberOfGuests > room.getCapacity()) {
            throw new BusinessRuleException("Number of guests (" + numberOfGuests + 
                ") exceeds room capacity (" + room.getCapacity() + ")");
        }
    }

    /**
     * Validate room operational readiness (INV-ROM-001 / INV-ROM-002)
     */
    private void validateRoomOperationalStatus(Room room) {
        if (room.getStatus() == RoomStatus.OUT_OF_SERVICE) {
            throw new BusinessRuleException("Room " + room.getNumber() + " is currently out of service and cannot be reserved");
        }
    }

    /**
     * Check room availability for new reservation
     */
    private void checkRoomAvailability(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        long conflictCount = reservationRepository.countConflictingReservations(roomId, checkIn, checkOut);
        
        if (conflictCount > 0) {
            throw new BusinessRuleException("Room is not available for the selected dates. " +
                "Conflicting reservations: " + conflictCount);
        }
    }

    /**
     * Calculate total price for a reservation
     */
    public BigDecimal calculateTotalPrice(Room room, LocalDate checkIn, LocalDate checkOut, Integer numberOfGuests) {
        long numberOfNights = ChronoUnit.DAYS.between(checkIn, checkOut);
        
        // Base price calculation
        BigDecimal basePrice = room.getPrice().multiply(BigDecimal.valueOf(numberOfNights));
        
        // Additional guest fee (if more than 2 guests)
        BigDecimal additionalGuestFee = BigDecimal.ZERO;
        if (numberOfGuests > 2) {
            int extraGuests = numberOfGuests - 2;
            BigDecimal feePerExtraGuest = BigDecimal.valueOf(25.00); // $25 per extra guest per night
            additionalGuestFee = feePerExtraGuest.multiply(BigDecimal.valueOf(extraGuests))
                                                .multiply(BigDecimal.valueOf(numberOfNights));
        }
        
        // Tax calculation (10% tax)
        BigDecimal subtotal = basePrice.add(additionalGuestFee);
        BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(0.10));
        
        // Total price
        BigDecimal totalPrice = subtotal.add(tax);
        
        return totalPrice.setScale(2, RoundingMode.HALF_UP);
    }

    // ========== UPDATE & CANCEL OPERATIONS ==========

    private BigDecimal applyPointsDiscount(BigDecimal basePrice, Reservation reservation) {
        if (reservation.getPointsUsed() != null && reservation.getPointsUsed() > 0
                && reservation.getPointsDiscount() != null) {
            return basePrice.subtract(reservation.getPointsDiscount()).max(BigDecimal.ZERO);
        }
        return basePrice;
    }

    /**
     * Update an existing reservation
     */
    public Reservation updateReservation(Long reservationId, LocalDate newCheckIn, LocalDate newCheckOut, 
                                       Integer newNumberOfGuests, String currency) {
        
        Reservation reservation = reservationRepository.findByIdForUpdate(reservationId)
            .orElseThrow(() -> new NotFoundException("Reservation not found with ID: " + reservationId));
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot update a cancelled reservation");
        }
        
        // Validate new inputs
        validateReservationInputs(newCheckIn, newCheckOut, newNumberOfGuests, currency);

        // Acquire pessimistic lock on room to serialize concurrent date updates
        Room room = roomRepository.findByIdForUpdate(reservation.getRoom().getId())
            .orElseThrow(() -> new NotFoundException("Room not found with ID: " + reservation.getRoom().getId()));
        
        // Validate room capacity and operational status
        validateRoomCapacity(room, newNumberOfGuests);
        validateRoomOperationalStatus(room);
        
        // Check availability for new dates (excluding current reservation)
        checkRoomAvailabilityForUpdate(room.getId(), newCheckIn, newCheckOut, reservationId);
        
        // Update reservation
        reservation.setCheckIn(newCheckIn);
        reservation.setCheckOut(newCheckOut);
        reservation.setNumberOfGuests(newNumberOfGuests);
        reservation.setCurrency(currency);
        
        // Recalculate price while preserving any loyalty points discount
        BigDecimal basePrice = calculateTotalPrice(room, newCheckIn, newCheckOut, newNumberOfGuests);
        reservation.setTotalPrice(applyPointsDiscount(basePrice, reservation));
        
        return reservationRepository.save(reservation);
    }

    /**
     * Cancel a reservation with business rules
     */
    @Transactional
    public void cancelReservation(Long reservationId, String reason) {
        Reservation reservation = reservationRepository.findByIdForUpdate(reservationId)
            .orElseThrow(() -> new NotFoundException("Reservation not found with ID: " + reservationId));
        
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessRuleException("Reservation is already cancelled");
        }
        
        // Check cancellation policy - allow cancellation up to check-in date
        LocalDate today = LocalDate.now();
        if (reservation.getCheckIn().isBefore(today)) {
            throw new BusinessRuleException("Cannot cancel reservation after check-in date has passed");
        }
        
        // Refund points if any were used
        if (reservation.getPointsUsed() != null && reservation.getPointsUsed() > 0) {
            loyaltyService.refundReservationPoints(reservationId, reservation.getPointsUsed());
        }

        // Claw back any points earned when reservation was created (INV-LOY-002)
        loyaltyService.clawbackReservationPoints(reservationId);
        
        // Update reservation status
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
    }

    /**
     * Check room availability for reservation update (excluding current reservation)
     */
    private void checkRoomAvailabilityForUpdate(Long roomId, LocalDate checkIn, LocalDate checkOut, Long excludeReservationId) {
        long conflictCount = reservationRepository.countConflictingReservations(roomId, checkIn, checkOut, excludeReservationId);
        
        if (conflictCount > 0) {
            throw new BusinessRuleException("Room is not available for the selected dates. " +
                "Conflicting reservations: " + conflictCount);
        }
    }

    // ========== AVAILABILITY & SEARCH ==========

    /**
     * Check if a room is available for the given date range
     */
    @Transactional(readOnly = true)
    public boolean isRoomAvailable(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        return reservationRepository.countConflictingReservations(roomId, checkIn, checkOut) == 0;
    }

    /**
     * Get available rooms for a date range
     * In our simplified model, rooms are available if they don't have conflicting reservations
     */
    @Transactional(readOnly = true)
    public List<Room> getAvailableRooms(LocalDate checkIn, LocalDate checkOut, Integer numberOfGuests) {
        return roomRepository.findAvailableRooms(checkIn, checkOut, numberOfGuests);
    }

    /**
     * Get reservations by guest
     */
    @Transactional(readOnly = true)
    public List<Reservation> getReservationsByGuest(Long clientId) {
        return reservationRepository.findByClientId(clientId);
    }

    /**
     * Get reservations by room
     */
    @Transactional(readOnly = true)
    public List<Reservation> getReservationsByRoom(Long roomId) {
        return reservationRepository.findByRoomId(roomId);
    }

    /**
     * Get reservations by status
     */
    @Transactional(readOnly = true)
    public List<Reservation> getReservationsByStatus(ReservationStatus status) {
        return reservationRepository.findByStatus(status);
    }

    // ========== RESERVATION STATUS MANAGEMENT ==========

    /**
     * Confirm a reservation (if needed for future business logic)
     */
    public Reservation confirmReservation(Long reservationId) {
        Reservation reservation = getReservationById(reservationId);
        
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            throw new BusinessRuleException("Reservation is already confirmed");
        }
        
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot confirm a cancelled reservation");
        }
        
        reservation.setStatus(ReservationStatus.CONFIRMED);
        return reservationRepository.save(reservation);
    }

    // ========== SEARCH & FILTERING ==========

    /**
     * Search reservations with multiple criteria
     */
    @Transactional(readOnly = true)
    public Page<Reservation> searchReservations(Long clientId, Long roomId, ReservationStatus status,
                                              BigDecimal minPrice, BigDecimal maxPrice, String currency,
                                              Integer minGuests, Integer maxGuests,
                                              LocalDate checkInFrom, LocalDate checkInTo,
                                              Pageable pageable) {
        return reservationRepository.findByCriteria(clientId, roomId, status, minPrice, maxPrice, 
            currency, minGuests, maxGuests, checkInFrom, checkInTo, pageable);
    }

    /**
     * Get reservations by date range
     */
    @Transactional(readOnly = true)
    public List<Reservation> getReservationsByDateRange(LocalDate startDate, LocalDate endDate) {
        return reservationRepository.findByCheckInBetween(startDate, endDate);
    }

    /**
     * Get upcoming reservations
     */
    @Transactional(readOnly = true)
    public List<Reservation> getUpcomingReservations() {
        LocalDate today = LocalDate.now();
        return reservationRepository.findByCheckInGreaterThanEqual(today);
    }

    /**
     * Get active reservations (current guests)
     * In our simplified model, these are confirmed reservations where today is between check-in and check-out
     */
    @Transactional(readOnly = true)
    public List<Reservation> getActiveReservations() {
        LocalDate today = LocalDate.now();
        return reservationRepository.findByCheckInLessThanEqualAndCheckOutGreaterThanEqual(today, today);
    }

    // ========== STATISTICS & ANALYTICS ==========

    /**
     * Get reservation statistics
     */
    @Transactional(readOnly = true)
    public ReservationStatistics getReservationStatistics() {
        long totalReservations = reservationRepository.count();
        long confirmedReservations = reservationRepository.countByStatus(ReservationStatus.CONFIRMED);
        long cancelledReservations = reservationRepository.countByStatus(ReservationStatus.CANCELLED);
        
        BigDecimal totalRevenue = reservationRepository.getTotalRevenue();
        BigDecimal averagePrice = reservationRepository.getAverageReservationPrice();
        
        return new ReservationStatistics(totalReservations, confirmedReservations, 
            cancelledReservations, totalRevenue, averagePrice);
    }

    /**
     * Get revenue by currency
     */
    @Transactional(readOnly = true)
    public BigDecimal getRevenueByCurrency(String currency) {
        return reservationRepository.getTotalRevenueByCurrency(currency);
    }

    /**
     * Get reservations overlapping with a specific date range for analytics.
     * Replaces in-memory full table scanning with an indexed database query (INV-ANL-001).
     */
    @Transactional(readOnly = true)
    public List<Reservation> getOverlappingReservationsInDateRange(LocalDate startDate, LocalDate endDate, ReservationStatus status) {
        return reservationRepository.findOverlappingReservationsInDateRange(startDate, endDate, status);
    }

    // ========== ADMIN OPERATIONS ==========

    /**
     * Get all reservations (Admin only)
     */
    @Transactional(readOnly = true)
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    /**
     * Get all reservations with pagination (Admin only)
     */
    @Transactional(readOnly = true)
    public Page<Reservation> getAllReservations(Pageable pageable) {
        return reservationRepository.findAll(pageable);
    }

    // ========== DTO CONVERSION METHODS ==========

    /**
     * Convert Reservation entity to ReservationResponseDto
     */
    private ReservationResponseDto convertToResponseDto(Reservation reservation) {
        ReservationResponseDto dto = new ReservationResponseDto();
        dto.setId(reservation.getId());
        dto.setCheckIn(reservation.getCheckIn());
        dto.setCheckOut(reservation.getCheckOut());
        dto.setNumberOfGuests(reservation.getNumberOfGuests());
        dto.setTotalPrice(reservation.getTotalPrice());
        dto.setCurrency(reservation.getCurrency());
        dto.setStatus(reservation.getStatus());
        dto.setClientId(reservation.getClient().getId());
        dto.setClientName(reservation.getClient().getFullName());
        dto.setClientEmail(reservation.getClient().getEmail());
        dto.setRoomId(reservation.getRoom().getId());
        dto.setRoomNumber(reservation.getRoom().getNumber());
        dto.setRoomType(reservation.getRoom().getRoomType().toString());
        dto.setCreatedAt(reservation.getCreatedAt());
        dto.setUpdatedAt(reservation.getUpdatedAt());
        return dto;
    }

    // ========== CONTROLLER-SPECIFIC METHODS ==========

    /**
     * Create reservation from DTO (Client controller)
     */
    public ReservationResponseDto createReservation(ReservationCreateRequestDto request, Long clientId) {
        Reservation reservation = createReservation(
            request.getRoomId(), 
            clientId, 
            request.getCheckIn(), 
            request.getCheckOut(), 
            request.getNumberOfGuests(), 
            request.getCurrency()
        );
        return convertToResponseDto(reservation);
    }

    /**
     * Create reservation with points redemption from DTO (Client controller)
     */
    public ReservationResponseDto createReservationWithPoints(ReservationCreateRequestDto request, Long clientId) {
        Reservation reservation = createReservationWithPoints(
            clientId,
            request.getRoomId(), 
            request.getCheckIn(), 
            request.getCheckOut(), 
            request.getNumberOfGuests(), 
            request.getCurrency(),
            request.getPointsUsed()
        );
        return convertToResponseDto(reservation);
    }

    /**
     * Get reservations by client ID with pagination (returns DTOs)
     */
    @Transactional(readOnly = true)
    public Page<ReservationResponseDto> getReservationsByClientId(Long clientId, Pageable pageable) {
        Page<Reservation> reservations = reservationRepository.findByClientId(clientId, pageable);
        return reservations.map(this::convertToResponseDto);
    }

    /**
     * Get reservation by ID and client ID (Client can only see their own)
     */
    @Transactional(readOnly = true)
    public ReservationResponseDto getReservationByIdAndClientId(Long id, Long clientId) {
        Reservation reservation = getReservationById(id);
        if (!reservation.getClient().getId().equals(clientId)) {
            throw new BusinessRuleException("You can only view your own reservations");
        }
        return convertToResponseDto(reservation);
    }

    /**
     * Update reservation from DTO (Client controller)
     */
    public ReservationResponseDto updateReservation(Long id, ReservationUpdateRequestDto request, Long clientId) {
        Reservation reservation = reservationRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new NotFoundException("Reservation not found with ID: " + id));
        if (!reservation.getClient().getId().equals(clientId)) {
            throw new BusinessRuleException("You can only update your own reservations");
        }
        
        Reservation updatedReservation = updateReservation(
            id, 
            request.getCheckIn(), 
            request.getCheckOut(), 
            request.getNumberOfGuests(), 
            reservation.getCurrency()
        );
        return convertToResponseDto(updatedReservation);
    }

    /**
     * Update reservation from DTO (Admin controller)
     */
    public ReservationResponseDto updateReservation(Long id, ReservationUpdateRequestDto request) {
        Reservation reservation = reservationRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new NotFoundException("Reservation not found with ID: " + id));
        Reservation updatedReservation = updateReservation(
            id, 
            request.getCheckIn(), 
            request.getCheckOut(), 
            request.getNumberOfGuests(), 
            reservation.getCurrency()
        );
        return convertToResponseDto(updatedReservation);
    }

    /**
     * Cancel reservation (Client controller)
     */
    @Transactional
    public void cancelReservation(Long id, Long clientId) {
        Reservation reservation = reservationRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new NotFoundException("Reservation not found with ID: " + id));
        if (!reservation.getClient().getId().equals(clientId)) {
            throw new BusinessRuleException("You can only cancel your own reservations");
        }
        cancelReservation(id, "Cancelled by client");
    }

    /**
     * Cancel reservation (Admin controller)
     */
    @Transactional
    public void cancelReservation(Long id) {
        cancelReservation(id, "Cancelled by admin");
    }

    /**
     * Get reservation by ID (returns DTO)
     */
    @Transactional(readOnly = true)
    public ReservationResponseDto getReservationByIdAsDto(Long id) {
        Reservation reservation = getReservationById(id);
        return convertToResponseDto(reservation);
    }

    /**
     * Search reservations with criteria DTO
     */
    @Transactional(readOnly = true)
    public Page<ReservationResponseDto> searchReservationsAsDto(ReservationSearchCriteriaDto criteria, Pageable pageable) {
        Page<Reservation> reservations = reservationRepository.findByCriteria(
            criteria.getClientId(),
            criteria.getRoomId(),
            criteria.getStatus(),
            null,
            null,
            null,
            null,
            null,
            criteria.getCheckInFrom(),
            criteria.getCheckInTo(),
            pageable
        );
        return reservations.map(this::convertToResponseDto);
    }

    /**
     * Reassign reservation to different room
     */
    public ReservationResponseDto reassignReservation(Long id, Long newRoomId) {
        Reservation reservation = reservationRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new NotFoundException("Reservation not found with ID: " + id));
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot reassign a cancelled reservation");
        }
        
        // Validate new room with pessimistic write lock
        Room newRoom = roomRepository.findByIdForUpdate(newRoomId)
            .orElseThrow(() -> new NotFoundException("Room not found with ID: " + newRoomId));
        
        // Validate new room capacity and operational status
        validateRoomCapacity(newRoom, reservation.getNumberOfGuests());
        validateRoomOperationalStatus(newRoom);
        
        // Check if new room is available for the reservation dates
        checkRoomAvailabilityForUpdate(newRoomId, reservation.getCheckIn(), reservation.getCheckOut(), id);
        
        // Update reservation and recalculate price for the new room
        reservation.setRoom(newRoom);
        BigDecimal basePrice = calculateTotalPrice(newRoom, reservation.getCheckIn(), reservation.getCheckOut(), reservation.getNumberOfGuests());
        reservation.setTotalPrice(applyPointsDiscount(basePrice, reservation));
        Reservation updatedReservation = reservationRepository.save(reservation);
        
        return convertToResponseDto(updatedReservation);
    }

    /**
     * Get reservations by room ID with pagination (returns DTOs)
     */
    @Transactional(readOnly = true)
    public Page<ReservationResponseDto> getReservationsByRoomId(Long roomId, Pageable pageable) {
        Page<Reservation> reservations = reservationRepository.findByRoomId(roomId, pageable);
        return reservations.map(this::convertToResponseDto);
    }

    /**
     * Get reservations by status with pagination (returns DTOs)
     */
    @Transactional(readOnly = true)
    public Page<ReservationResponseDto> getReservationsByStatus(ReservationStatus status, Pageable pageable) {
        Page<Reservation> reservations = reservationRepository.findByStatus(status, pageable);
        return reservations.map(this::convertToResponseDto);
    }

    /**
     * Get reservations by date range with pagination (returns DTOs)
     */
    @Transactional(readOnly = true)
    public Page<ReservationResponseDto> getReservationsByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Page<Reservation> reservations = reservationRepository.findByCheckInBetween(startDate, endDate, pageable);
        return reservations.map(this::convertToResponseDto);
    }

    /**
     * Get all reservations with pagination (returns DTOs) - Admin only
     */
    @Transactional(readOnly = true)
    public Page<ReservationResponseDto> getAllReservationsAsDto(Pageable pageable) {
        Page<Reservation> reservations = reservationRepository.findAll(pageable);
        return reservations.map(this::convertToResponseDto);
    }

    /**
     * Search all reservations with criteria (returns DTOs) - Admin only
     * This method can search across all reservations without requiring a specific clientId
     */
    @Transactional(readOnly = true)
    public Page<ReservationResponseDto> searchAllReservationsAsDto(ReservationSearchCriteriaDto criteria, Pageable pageable) {
        // Use the general search method that can handle null clientId for admin searches
        Page<Reservation> reservations = reservationRepository.findByCriteria(
            criteria.getClientId(), 
            criteria.getRoomId(), 
            criteria.getStatus(),
            null, // minPrice - not used in current criteria
            null, // maxPrice - not used in current criteria  
            null, // currency - not used in current criteria
            null, // minGuests - not used in current criteria
            null, // maxGuests - not used in current criteria
            criteria.getCheckInFrom(), 
            criteria.getCheckInTo(), 
            pageable
        );
        return reservations.map(this::convertToResponseDto);
    }

    /**
     * Get user ID by email (for legacy token extraction - use UserPrincipal.getId() instead)
     * @deprecated Identity should be injected via UserPrincipal from token claims to avoid redundant DB queries.
     */
    @Deprecated(forRemoval = true)
    @Transactional(readOnly = true)
    public Long getUserIdByEmail(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new NotFoundException("User not found with email: " + email));
        return user.getId();
    }

    // ========== INNER CLASSES ==========

    /**
     * Reservation statistics data class
     */
    public static class ReservationStatistics {
        private final long totalReservations;
        private final long confirmedReservations;
        private final long cancelledReservations;
        private final BigDecimal totalRevenue;
        private final BigDecimal averagePrice;

        public ReservationStatistics(long totalReservations, long confirmedReservations, 
                                   long cancelledReservations, BigDecimal totalRevenue, BigDecimal averagePrice) {
            this.totalReservations = totalReservations;
            this.confirmedReservations = confirmedReservations;
            this.cancelledReservations = cancelledReservations;
            this.totalRevenue = totalRevenue;
            this.averagePrice = averagePrice;
        }

        // Getters
        public long getTotalReservations() { return totalReservations; }
        public long getConfirmedReservations() { return confirmedReservations; }
        public long getCancelledReservations() { return cancelledReservations; }
        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public BigDecimal getAveragePrice() { return averagePrice; }
        
        public double getConfirmationRate() {
            return totalReservations > 0 ? (double) confirmedReservations / totalReservations * 100 : 0;
        }
        
        public double getCancellationRate() {
            return totalReservations > 0 ? (double) cancelledReservations / totalReservations * 100 : 0;
        }
    }
}
