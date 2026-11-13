// Service responsibilities: Event Management - Create, update, delete events
// Installation Integration - Manage event-installation relationships and availability
// Business Rules - Pricing, capacity, time validation, event type management
// Search & Filtering - Find events by criteria, date ranges, installation
// Statistics & Analytics - Event performance, revenue tracking

package com.MyBooking.event.service;

import com.MyBooking.event.domain.Event;
import com.MyBooking.event.domain.EventType;
import com.MyBooking.event.domain.EventBooking;
import com.MyBooking.event.domain.EventBookingStatus;
import com.MyBooking.event.repository.EventRepository;
import com.MyBooking.event.repository.EventBookingRepository;
import com.MyBooking.installation.domain.Installation;
import com.MyBooking.installation.domain.InstallationType;
import com.MyBooking.installation.repository.InstallationRepository;
import com.MyBooking.auth.domain.User;
import com.MyBooking.auth.repository.UserRepository;
import com.MyBooking.reservation.domain.Reservation;
import com.MyBooking.reservation.repository.ReservationRepository;
import com.MyBooking.common.exception.BusinessRuleException;
import com.MyBooking.common.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class EventService {

    @Autowired
    private EventRepository eventRepository;
    
    @Autowired
    private EventBookingRepository eventBookingRepository;
    
    @Autowired
    private InstallationRepository installationRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ReservationRepository reservationRepository;

    // ========== EVENT MANAGEMENT ==========

    /**
     * Create a new event with comprehensive validation
     */
    public Event createEvent(String name, String description, EventType eventType, 
                           LocalDateTime startAt, LocalDateTime endAt, Integer capacity,
                           BigDecimal price, String currency, Long installationId) {
        
        // Validate inputs
        validateEventInputs(name, eventType, startAt, endAt, capacity, price, currency);
        
        // Get and lock installation to serialize concurrent event scheduling
        Installation installation = installationRepository.findByIdForUpdate(installationId)
            .orElseThrow(() -> new NotFoundException("Installation not found with ID: " + installationId));
        
        // Validate event capacity against installation capacity
        validateEventCapacity(installation, capacity);
        
        // Check installation availability
        checkInstallationAvailability(installationId, startAt, endAt);
        
        // Create event
        Event event = new Event();
        event.setName(name);
        event.setDescription(description);
        event.setEventType(eventType);
        event.setStartAt(startAt);
        event.setEndAt(endAt);
        event.setCapacity(capacity);
        event.setPrice(price);
        event.setCurrency(currency);
        event.setInstallation(installation);
        
        return eventRepository.save(event);
    }

    /**
     * Update an existing event
     */
    public Event updateEvent(Long eventId, String name, String description, EventType eventType,
                           LocalDateTime startAt, LocalDateTime endAt, Integer capacity,
                           BigDecimal price, String currency, Long installationId) {
        
        Event event = eventRepository.findByIdForUpdate(eventId)
            .orElseThrow(() -> new NotFoundException("Event not found with ID: " + eventId));
        
        // Validate new inputs
        validateEventInputs(name, eventType, startAt, endAt, capacity, price, currency);
        
        // Get and lock installation to serialize concurrent event scheduling
        Installation installation = installationRepository.findByIdForUpdate(installationId)
            .orElseThrow(() -> new NotFoundException("Installation not found with ID: " + installationId));
        
        // Validate event capacity against installation capacity and existing confirmed bookings
        validateEventCapacity(installation, capacity);
        int confirmedParticipants = eventBookingRepository.countConfirmedParticipantsByEventId(eventId);
        if (capacity < confirmedParticipants) {
            throw new BusinessRuleException("Event capacity (" + capacity +
                ") cannot be less than already confirmed participants (" + confirmedParticipants + ")");
        }
        
        // Check installation availability (excluding current event)
        checkInstallationAvailabilityForUpdate(installationId, startAt, endAt, eventId);
        
        // Update event
        event.setName(name);
        event.setDescription(description);
        event.setEventType(eventType);
        event.setStartAt(startAt);
        event.setEndAt(endAt);
        event.setCapacity(capacity);
        event.setPrice(price);
        event.setCurrency(currency);
        event.setInstallation(installation);
        
        return eventRepository.save(event);
    }

    /**
     * Delete an event with business rules
     */
    public void deleteEvent(Long eventId, String reason) {
        Event event = eventRepository.findByIdForUpdate(eventId)
            .orElseThrow(() -> new NotFoundException("Event not found with ID: " + eventId));
        
        // Check if event is in the past
        LocalDateTime now = LocalDateTime.now();
        if (event.getStartAt().isBefore(now)) {
            throw new BusinessRuleException("Cannot delete events that have already started");
        }
        
        // Check if event starts within 24 hours
        long hoursUntilStart = ChronoUnit.HOURS.between(now, event.getStartAt());
        if (hoursUntilStart < 24) {
            throw new BusinessRuleException("Cannot delete events that start within 24 hours");
        }
        
        // Check if event has existing confirmed bookings via DB count under event lock
        long confirmedBookings = eventBookingRepository.countByEventIdAndStatus(eventId, EventBookingStatus.CONFIRMED);
        if (confirmedBookings > 0) {
            throw new BusinessRuleException(
                "Cannot delete event with " + confirmedBookings + " confirmed booking(s). " +
                "Please cancel all bookings first or contact support for assistance."
            );
        }
        
        eventRepository.delete(event);
    }

    /**
     * Get event by ID
     */
    @Transactional(readOnly = true)
    public Event getEventById(Long eventId) {
        return eventRepository.findById(eventId)
            .orElseThrow(() -> new NotFoundException("Event not found with ID: " + eventId));
    }

    // ========== VALIDATION METHODS ==========

    /**
     * Validate event inputs
     */
    private void validateEventInputs(String name, EventType eventType, LocalDateTime startAt, 
                                   LocalDateTime endAt, Integer capacity, BigDecimal price, String currency) {
        
        LocalDateTime now = LocalDateTime.now();
        
        // Name validation
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessRuleException("Event name is required");
        }
        
        if (name.length() > 100) {
            throw new BusinessRuleException("Event name must not exceed 100 characters");
        }
        
        // Event type validation
        if (eventType == null) {
            throw new BusinessRuleException("Event type is required");
        }
        
        // Time validation
        if (startAt == null || endAt == null) {
            throw new BusinessRuleException("Event start time and end time are required");
        }
        if (startAt.isBefore(now)) {
            throw new BusinessRuleException("Event start time cannot be in the past");
        }
        
        if (endAt.isBefore(startAt) || endAt.isEqual(startAt)) {
            throw new BusinessRuleException("Event end time must be after start time");
        }
        
        // Duration validation (maximum 8 hours)
        long durationHours = ChronoUnit.HOURS.between(startAt, endAt);
        if (durationHours > 8) {
            throw new BusinessRuleException("Event duration cannot exceed 8 hours");
        }
        
        // Capacity validation
        if (capacity == null || capacity < 1 || capacity > 100) {
            throw new BusinessRuleException("Event capacity must be between 1 and 100");
        }
        
        // Price validation
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Event price must be greater than 0");
        }
        
        // Currency validation
        if (currency == null || currency.length() != 3) {
            throw new BusinessRuleException("Currency must be a 3-character code (e.g., USD, EUR)");
        }
    }

    /**
     * Validate event capacity against installation capacity
     */
    private void validateEventCapacity(Installation installation, Integer eventCapacity) {
        if (eventCapacity > installation.getCapacity()) {
            throw new BusinessRuleException("Event capacity (" + eventCapacity + 
                ") exceeds installation capacity (" + installation.getCapacity() + ")");
        }
    }

    /**
     * Check installation availability for new event
     */
    private void checkInstallationAvailability(Long installationId, LocalDateTime startAt, LocalDateTime endAt) {
        if (eventRepository.existsOverlappingEventForInstallation(installationId, startAt, endAt)) {
            throw new BusinessRuleException("Installation is not available for the selected time.");
        }
    }

    /**
     * Check installation availability for event update (excluding current event)
     */
    private void checkInstallationAvailabilityForUpdate(Long installationId, LocalDateTime startAt, 
                                                       LocalDateTime endAt, Long excludeEventId) {
        if (eventRepository.existsOverlappingEventForInstallationExcludingEvent(
                installationId, startAt, endAt, excludeEventId)) {
            throw new BusinessRuleException("Installation is not available for the selected time.");
        }
    }

    // ========== INSTALLATION & AVAILABILITY ==========

    /**
     * Check if an installation is available for the given time range
     */
    @Transactional(readOnly = true)
    public boolean isInstallationAvailable(Long installationId, LocalDateTime startAt, LocalDateTime endAt) {
        return !eventRepository.existsOverlappingEventForInstallation(installationId, startAt, endAt);
    }

    /**
     * Get available installations for a time range
     */
    @Transactional(readOnly = true)
    public List<Installation> getAvailableInstallations(LocalDateTime startAt, LocalDateTime endAt, 
                                                       Integer requiredCapacity, InstallationType installationType) {
        return installationRepository.findAvailableInstallations(startAt, endAt, requiredCapacity, installationType);
    }

    /**
     * Get available installations for a time range (all types)
     */
    @Transactional(readOnly = true)
    public List<Installation> getAvailableInstallations(LocalDateTime startAt, LocalDateTime endAt, Integer requiredCapacity) {
        return getAvailableInstallations(startAt, endAt, requiredCapacity, null);
    }

    // ========== SEARCH & FILTERING ==========

    /**
     * Search events with multiple criteria
     */
    @Transactional(readOnly = true)
    public Page<Event> searchEvents(String name, EventType eventType, Long installationId,
                                  BigDecimal minPrice, BigDecimal maxPrice, String currency,
                                  Integer minCapacity, Pageable pageable) {
        return eventRepository.findByCriteria(name, eventType, installationId, minPrice, maxPrice,
            minCapacity, currency, pageable);
    }

    /**
     * Get events by event type
     */
    @Transactional(readOnly = true)
    public List<Event> getEventsByType(EventType eventType) {
        return eventRepository.findByEventType(eventType);
    }

    /**
     * Get events by event type with pagination
     */
    @Transactional(readOnly = true)
    public Page<Event> getEventsByType(EventType eventType, Pageable pageable) {
        return eventRepository.findByEventType(eventType, pageable);
    }

    /**
     * Get events by installation
     */
    @Transactional(readOnly = true)
    public List<Event> getEventsByInstallation(Long installationId) {
        return eventRepository.findByInstallationId(installationId);
    }

    /**
     * Get events by installation with pagination
     */
    @Transactional(readOnly = true)
    public Page<Event> getEventsByInstallation(Long installationId, Pageable pageable) {
        return eventRepository.findByInstallationId(installationId, pageable);
    }

    /**
     * Get events by date range
     */
    @Transactional(readOnly = true)
    public List<Event> getEventsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return eventRepository.findByStartAtBetween(startDate, endDate);
    }

    /**
     * Get upcoming events
     */
    @Transactional(readOnly = true)
    public List<Event> getUpcomingEvents() {
        return eventRepository.findByStartAtGreaterThanEqual(LocalDateTime.now());
    }

    /**
     * Get events by capacity range
     */
    @Transactional(readOnly = true)
    public List<Event> getEventsByCapacity(Integer minCapacity, Integer maxCapacity) {
        return eventRepository.findByCapacityBetween(minCapacity, maxCapacity);
    }

    /**
     * Get events by price range
     */
    @Transactional(readOnly = true)
    public List<Event> getEventsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return eventRepository.findByPriceBetween(minPrice, maxPrice);
    }

    // ========== BUSINESS OPERATIONS ==========

    /**
     * Calculate event price based on installation hourly rate
     */
    public BigDecimal calculateEventPrice(Installation installation, LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
            throw new BusinessRuleException("Event end time must be after start time");
        }
        long durationMinutes = ChronoUnit.MINUTES.between(startAt, endAt);
        long durationHours = (durationMinutes + 59) / 60;
        
        // Base price calculation
        BigDecimal basePrice = installation.getHourlyRate().multiply(BigDecimal.valueOf(durationHours));
        
        // Add 15% service fee
        BigDecimal serviceFee = basePrice.multiply(BigDecimal.valueOf(0.15));
        
        // Total price
        BigDecimal totalPrice = basePrice.add(serviceFee);
        
        return totalPrice.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Get events by installation type
     */
    @Transactional(readOnly = true)
    public List<Event> getEventsByInstallationType(InstallationType installationType) {
        return eventRepository.findByInstallationType(installationType);
    }

    /**
     * Get events by installation type with pagination
     */
    @Transactional(readOnly = true)
    public Page<Event> getEventsByInstallationType(InstallationType installationType, Pageable pageable) {
        return eventRepository.findByInstallationType(installationType, pageable);
    }

    // ========== STATISTICS & ANALYTICS ==========

    /**
     * Get event statistics
     */
    @Transactional(readOnly = true)
    public EventStatistics getEventStatistics() {
        long totalEvents = eventRepository.count();
        long upcomingEvents = eventRepository.countUpcomingEvents(LocalDateTime.now());
        long pastEvents = totalEvents - upcomingEvents;

        BigDecimal totalRevenue = eventRepository.getTotalEventRevenue();
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        BigDecimal averagePrice = totalEvents > 0
            ? totalRevenue.divide(BigDecimal.valueOf(totalEvents), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        return new EventStatistics(totalEvents, upcomingEvents, pastEvents, totalRevenue, averagePrice);
    }

    /**
     * Get revenue by currency
     */
    @Transactional(readOnly = true)
    public BigDecimal getRevenueByCurrency(String currency) {
        BigDecimal revenue = eventRepository.getRevenueByCurrency(currency);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    /**
     * Get events by currency
     */
    @Transactional(readOnly = true)
    public List<Event> getEventsByCurrency(String currency) {
        return eventRepository.findByCurrency(currency);
    }

    // ========== ADMIN OPERATIONS ==========

    /**
     * Get all events (Admin only)
     */
    @Transactional(readOnly = true)
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    /**
     * Get all events with pagination (Admin only)
     */
    @Transactional(readOnly = true)
    public Page<Event> getAllEvents(Pageable pageable) {
        return eventRepository.findAll(pageable);
    }

    /**
     * Get events by name containing (case-insensitive)
     */
    @Transactional(readOnly = true)
    public List<Event> getEventsByNameContaining(String name) {
        return eventRepository.findByNameContainingIgnoreCase(name);
    }

    /**
     * Get events by name containing with pagination
     */
    @Transactional(readOnly = true)
    public Page<Event> getEventsByNameContaining(String name, Pageable pageable) {
        return eventRepository.findByNameContainingIgnoreCase(name, pageable);
    }

    // ========== DTO METHODS ==========
    
    /**
     * Get all events as DTOs with pagination
     */
    @Transactional(readOnly = true)
    public Page<com.MyBooking.event.dto.EventResponseDto> getAllEventsAsDto(Pageable pageable) {
        Page<Event> events = getAllEvents(pageable);
        return events.map(this::convertToResponseDto);
    }
    
    /**
     * Get event by ID as DTO
     */
    @Transactional(readOnly = true)
    public com.MyBooking.event.dto.EventResponseDto getEventByIdAsDto(Long eventId) {
        Event event = getEventById(eventId);
        return convertToResponseDto(event);
    }
    
    /**
     * Create event from DTO
     */
    public com.MyBooking.event.dto.EventResponseDto createEventAsDto(com.MyBooking.event.dto.EventCreateRequestDto request) {
        Event event = createEvent(
            request.getName(),
            request.getDescription(),
            request.getEventType(),
            request.getStartAt(),
            request.getEndAt(),
            request.getCapacity(),
            request.getPrice(),
            request.getCurrency(),
            request.getInstallationId()
        );
        return convertToResponseDto(event);
    }
    
    /**
     * Update event from DTO
     */
    public com.MyBooking.event.dto.EventResponseDto updateEventAsDto(Long eventId, com.MyBooking.event.dto.EventUpdateRequestDto request) {
        Event event = eventRepository.findByIdForUpdate(eventId)
            .orElseThrow(() -> new NotFoundException("Event not found with ID: " + eventId));

        String name = request.getName() != null ? request.getName() : event.getName();
        String description = request.getDescription() != null ? request.getDescription() : event.getDescription();
        EventType eventType = request.getEventType() != null ? request.getEventType() : event.getEventType();
        LocalDateTime startAt = request.getStartAt() != null ? request.getStartAt() : event.getStartAt();
        LocalDateTime endAt = request.getEndAt() != null ? request.getEndAt() : event.getEndAt();
        Integer capacity = request.getCapacity() != null ? request.getCapacity() : event.getCapacity();
        BigDecimal price = request.getPrice() != null ? request.getPrice() : event.getPrice();
        String currency = request.getCurrency() != null ? request.getCurrency() : event.getCurrency();
        Long installationId = request.getInstallationId() != null
            ? request.getInstallationId()
            : event.getInstallation().getId();

        Event updatedEvent = updateEvent(
            eventId,
            name,
            description,
            eventType,
            startAt,
            endAt,
            capacity,
            price,
            currency,
            installationId
        );
        return convertToResponseDto(updatedEvent);
    }
    
    /**
     * Search events as DTOs
     */
    @Transactional(readOnly = true)
    public Page<com.MyBooking.event.dto.EventResponseDto> searchEventsAsDto(com.MyBooking.event.dto.EventSearchCriteriaDto criteria, Pageable pageable) {
        Page<Event> events = searchEvents(criteria.getName(), criteria.getEventType(), criteria.getInstallationId(), 
                                        criteria.getMinPrice(), criteria.getMaxPrice(), null, 
                                        criteria.getMinCapacity(), pageable);
        return events.map(this::convertToResponseDto);
    }
    
    /**
     * Get upcoming events as DTOs
     */
    @Transactional(readOnly = true)
    public Page<com.MyBooking.event.dto.EventResponseDto> getUpcomingEventsAsDto(Pageable pageable) {
        Page<Event> upcomingEvents = eventRepository.findByStartAtGreaterThanEqual(LocalDateTime.now(), pageable);
        return upcomingEvents.map(this::convertToResponseDto);
    }
    
    /**
     * Get events by type as DTOs
     */
    @Transactional(readOnly = true)
    public Page<com.MyBooking.event.dto.EventResponseDto> getEventsByTypeAsDto(EventType eventType, Pageable pageable) {
        Page<Event> events = getEventsByType(eventType, pageable);
        return events.map(this::convertToResponseDto);
    }
    
    /**
     * Get events by installation as DTOs
     */
    @Transactional(readOnly = true)
    public Page<com.MyBooking.event.dto.EventResponseDto> getEventsByInstallationAsDto(Long installationId, Pageable pageable) {
        Page<Event> events = getEventsByInstallation(installationId, pageable);
        return events.map(this::convertToResponseDto);
    }
    
    /**
     * Convert Event entity to EventResponseDto
     */
    private com.MyBooking.event.dto.EventResponseDto convertToResponseDto(Event event) {
        return new com.MyBooking.event.dto.EventResponseDto(
            event.getId(),
            event.getName(),
            event.getDescription(),
            event.getEventType(),
            event.getStartAt(),
            event.getEndAt(),
            event.getCapacity(),
            event.getPrice(),
            event.getCurrency(),
            event.getInstallation().getId(),
            event.getInstallation().getName(),
            event.getInstallation().getInstallationType().toString(),
            event.getCreatedAt(),
            event.getUpdatedAt()
        );
    }
    
    // ========== EVENT BOOKING DTO METHODS ==========
    
    /**
     * Create event booking from DTO with pessimistic row locking and aggregate capacity enforcement [INV-EVT-001, INV-EVT-002].
     */
    @Transactional
    public com.MyBooking.event.dto.EventBookingResponseDto createEventBookingAsDto(com.MyBooking.event.dto.EventBookingCreateRequestDto request, Long userId) {
        if (request == null || request.getEventId() == null) {
            throw new BusinessRuleException("Event ID is required");
        }
        
        // Acquire pessimistic write lock on the event to serialize concurrent bookings
        Event event = eventRepository.findByIdForUpdate(request.getEventId())
            .orElseThrow(() -> new NotFoundException("Event not found with ID: " + request.getEventId()));
        
        // Enforce temporal invariant: event must be in the future [INV-EVT-002]
        if (event.getStartAt().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Cannot book an event that has already started or taken place");
        }
        
        // Get user and validate
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));
        
        // Get reservation if provided (optional for standalone event bookings)
        Reservation reservation = null;
        if (request.getReservationId() != null) {
            reservation = reservationRepository.findById(request.getReservationId())
                .orElseThrow(() -> new NotFoundException("Reservation not found with ID: " + request.getReservationId()));
            if (reservation.getClient() == null || !reservation.getClient().getId().equals(userId)) {
                throw new BusinessRuleException("Access denied: Reservation does not belong to the user");
            }
            if (reservation.getStatus() == com.MyBooking.reservation.domain.ReservationStatus.CANCELLED) {
                throw new BusinessRuleException("Cannot link an event booking to a cancelled reservation");
            }
        }
        
        // Validate participant count
        Integer numberOfParticipants = request.getNumberOfParticipants() != null ? request.getNumberOfParticipants() : 1;
        if (numberOfParticipants < 1) {
            throw new BusinessRuleException("Number of participants must be at least 1");
        }
        
        // Query aggregate confirmed participants and enforce capacity invariant [INV-EVT-001]
        int currentBooked = eventBookingRepository.countConfirmedParticipantsByEventId(event.getId());
        int remainingCapacity = event.getCapacity() - currentBooked;
        if (numberOfParticipants > remainingCapacity) {
            throw new BusinessRuleException(
                "Event capacity exceeded. Requested: " + numberOfParticipants + 
                ", remaining capacity: " + remainingCapacity + 
                " (Total capacity: " + event.getCapacity() + ", currently booked: " + currentBooked + ")"
            );
        }
        
        // Create event booking
        EventBooking booking = new EventBooking();
        booking.setEvent(event);
        booking.setClient(user);
        booking.setReservation(reservation); // Can be null for standalone bookings
        booking.setStatus(EventBookingStatus.CONFIRMED);
        booking.setEventDateTime(event.getStartAt());
        booking.setBookingDate(LocalDateTime.now());
        booking.setNumberOfParticipants(numberOfParticipants);
        long durationMinutes = ChronoUnit.MINUTES.between(event.getStartAt(), event.getEndAt());
        booking.setDurationHours((int) Math.max(1L, (durationMinutes + 59) / 60));
        booking.setTotalPrice(event.getPrice().multiply(BigDecimal.valueOf(numberOfParticipants)));
        
        EventBooking savedBooking = eventBookingRepository.save(booking);
        return convertToEventBookingResponseDto(savedBooking);
    }
    
    /**
     * Get event bookings by user as DTOs
     */
    @Transactional(readOnly = true)
    public Page<com.MyBooking.event.dto.EventBookingResponseDto> getEventBookingsByUserAsDto(Long userId, Pageable pageable) {
        Page<EventBooking> bookings = eventBookingRepository.findByClientId(userId, pageable);
        return bookings.map(this::convertToEventBookingResponseDto);
    }
    
    /**
     * Get event booking by ID as DTO (with user validation)
     */
    @Transactional(readOnly = true)
    public com.MyBooking.event.dto.EventBookingResponseDto getEventBookingByIdAsDto(Long bookingId, Long userId) {
        EventBooking booking = eventBookingRepository.findById(bookingId)
            .orElseThrow(() -> new NotFoundException("Event booking not found with ID: " + bookingId));
        
        // Validate that the booking belongs to the user
        if (!booking.getClient().getId().equals(userId)) {
            throw new BusinessRuleException("Access denied: This booking does not belong to the user");
        }
        
        return convertToEventBookingResponseDto(booking);
    }
    
    /**
     * Get event booking by ID as DTO (admin view)
     */
    @Transactional(readOnly = true)
    public com.MyBooking.event.dto.EventBookingResponseDto getEventBookingByIdAsDto(Long bookingId) {
        EventBooking booking = eventBookingRepository.findById(bookingId)
            .orElseThrow(() -> new NotFoundException("Event booking not found with ID: " + bookingId));
        return convertToEventBookingResponseDto(booking);
    }
    
    /**
     * Cancel event booking as DTO
     */
    @Transactional
    public com.MyBooking.event.dto.EventBookingResponseDto cancelEventBookingAsDto(Long bookingId, Long userId, String reason) {
        EventBooking booking = eventBookingRepository.findByIdForUpdate(bookingId)
            .orElseThrow(() -> new NotFoundException("Event booking not found with ID: " + bookingId));
        
        // Validate that the booking belongs to the user
        if (!booking.getClient().getId().equals(userId)) {
            throw new BusinessRuleException("Access denied: This booking does not belong to the user");
        }
        
        // Check if booking can be cancelled
        if (booking.getStatus() != EventBookingStatus.CONFIRMED) {
            throw new BusinessRuleException("Only confirmed bookings can be cancelled");
        }
        
        // Check if event is in the future
        if (booking.getEventDateTime().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Cannot cancel past events");
        }
        
        booking.setStatus(EventBookingStatus.CANCELLED);
        EventBooking savedBooking = eventBookingRepository.save(booking);
        return convertToEventBookingResponseDto(savedBooking);
    }
    
    /**
     * Get all event bookings as DTOs (admin view)
     */
    @Transactional(readOnly = true)
    public Page<com.MyBooking.event.dto.EventBookingResponseDto> getAllEventBookingsAsDto(Pageable pageable) {
        Page<EventBooking> bookings = eventBookingRepository.findAll(pageable);
        return bookings.map(this::convertToEventBookingResponseDto);
    }
    
    /**
     * Update event booking status as DTO
     */
    public com.MyBooking.event.dto.EventBookingResponseDto updateEventBookingStatusAsDto(Long bookingId, com.MyBooking.event.domain.EventBookingStatus status) {
        EventBooking booking = eventBookingRepository.findByIdForUpdate(bookingId)
            .orElseThrow(() -> new NotFoundException("Event booking not found with ID: " + bookingId));

        if (status == EventBookingStatus.CONFIRMED && booking.getStatus() != EventBookingStatus.CONFIRMED) {
            Event event = eventRepository.findByIdForUpdate(booking.getEvent().getId())
                .orElseThrow(() -> new NotFoundException("Event not found with ID: " + booking.getEvent().getId()));
            if (event.getStartAt().isBefore(LocalDateTime.now())) {
                throw new BusinessRuleException("Cannot confirm a booking for an event that has already started or taken place");
            }
            int currentBooked = eventBookingRepository.countConfirmedParticipantsByEventId(event.getId());
            int requested = booking.getNumberOfParticipants() != null ? booking.getNumberOfParticipants() : 1;
            if (currentBooked + requested > event.getCapacity()) {
                throw new BusinessRuleException("Event capacity exceeded. Remaining capacity: " + (event.getCapacity() - currentBooked));
            }
        }
        
        booking.setStatus(status);
        EventBooking savedBooking = eventBookingRepository.save(booking);
        return convertToEventBookingResponseDto(savedBooking);
    }
    
    /**
     * Get user ID by email (legacy helper - use UserPrincipal.getId() instead)
     * @deprecated Identity should be injected via UserPrincipal from token claims to avoid redundant DB queries.
     */
    @Deprecated(forRemoval = true)
    public Long getUserIdByEmail(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new NotFoundException("User not found with email: " + email));
        return user.getId();
    }
    
    /**
     * Convert EventBooking entity to EventBookingResponseDto
     */
    private com.MyBooking.event.dto.EventBookingResponseDto convertToEventBookingResponseDto(EventBooking booking) {
        return new com.MyBooking.event.dto.EventBookingResponseDto(
            booking.getId(),
            booking.getEvent().getId(),
            booking.getEvent().getName(),
            booking.getEvent().getEventType(),
            booking.getEvent().getStartAt(),
            booking.getEvent().getEndAt(),
            booking.getEvent().getPrice(),
            booking.getEvent().getCurrency(),
            booking.getClient().getId(),
            booking.getClient().getFirstName(),
            booking.getClient().getLastName(),
            booking.getClient().getEmail(),
            booking.getReservation() != null ? booking.getReservation().getId() : null,
            booking.getStatus(),
            booking.getCreatedAt(),
            booking.getUpdatedAt()
        );
    }

    /**
     * Calculate total revenue from confirmed event bookings within a date/time range.
     * Replaces hardcoded mock analytics placeholders with indexed database push-down SUM (INV-ANL-002).
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalEventRevenueBetweenDates(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return eventBookingRepository.calculateTotalRevenueBetweenDates(
            startDateTime, endDateTime, EventBookingStatus.CONFIRMED);
    }
    
    // ========== INNER CLASSES ==========

    /**
     * Event statistics data class
     */
    public static class EventStatistics {
        private final long totalEvents;
        private final long upcomingEvents;
        private final long pastEvents;
        private final BigDecimal totalRevenue;
        private final BigDecimal averagePrice;

        public EventStatistics(long totalEvents, long upcomingEvents, long pastEvents, 
                             BigDecimal totalRevenue, BigDecimal averagePrice) {
            this.totalEvents = totalEvents;
            this.upcomingEvents = upcomingEvents;
            this.pastEvents = pastEvents;
            this.totalRevenue = totalRevenue;
            this.averagePrice = averagePrice;
        }

        // Getters
        public long getTotalEvents() { return totalEvents; }
        public long getUpcomingEvents() { return upcomingEvents; }
        public long getPastEvents() { return pastEvents; }
        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public BigDecimal getAveragePrice() { return averagePrice; }
        
        public double getUpcomingEventsPercentage() {
            return totalEvents > 0 ? (double) upcomingEvents / totalEvents * 100 : 0;
        }
        
        public double getPastEventsPercentage() {
            return totalEvents > 0 ? (double) pastEvents / totalEvents * 100 : 0;
        }
    }
}
