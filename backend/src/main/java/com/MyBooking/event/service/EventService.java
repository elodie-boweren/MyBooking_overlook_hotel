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
        
        // Get and validate installation
        Installation installation = installationRepository.findById(installationId)
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
        
        Event event = getEventById(eventId);
        
        // Validate new inputs
        validateEventInputs(name, eventType, startAt, endAt, capacity, price, currency);
        
        // Get and validate installation
        Installation installation = installationRepository.findById(installationId)
            .orElseThrow(() -> new NotFoundException("Installation not found with ID: " + installationId));
        
        // Validate event capacity against installation capacity
        validateEventCapacity(installation, capacity);
        
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
        Event event = getEventById(eventId);
        
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
        
        // Check if event has existing bookings
        List<EventBooking> existingBookings = eventBookingRepository.findByEventId(eventId);
        if (!existingBookings.isEmpty()) {
            long confirmedBookings = existingBookings.stream()
                .filter(booking -> booking.getStatus() == EventBookingStatus.CONFIRMED)
                .count();
            
            if (confirmedBookings > 0) {
                throw new BusinessRuleException(
                    "Cannot delete event with " + confirmedBookings + " confirmed booking(s). " +
                    "Please cancel all bookings first or contact support for assistance."
                );
            }
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
        if (capacity < 1 || capacity > 100) {
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
        // Get all events for this installation and check for overlaps manually
        List<Event> installationEvents = eventRepository.findByInstallationId(installationId);
        
        for (Event event : installationEvents) {
            if (isTimeOverlapping(startAt, endAt, event.getStartAt(), event.getEndAt())) {
                throw new BusinessRuleException("Installation is not available for the selected time. " +
                    "Conflicting with event: " + event.getName());
            }
        }
    }

    /**
     * Check installation availability for event update (excluding current event)
     */
    private void checkInstallationAvailabilityForUpdate(Long installationId, LocalDateTime startAt, 
                                                       LocalDateTime endAt, Long excludeEventId) {
        // Get all events for this installation and check for overlaps manually
        List<Event> installationEvents = eventRepository.findByInstallationId(installationId);
        
        for (Event event : installationEvents) {
            // Skip the current event being updated
            if (event.getId().equals(excludeEventId)) {
                continue;
            }
            
            if (isTimeOverlapping(startAt, endAt, event.getStartAt(), event.getEndAt())) {
                throw new BusinessRuleException("Installation is not available for the selected time. " +
                    "Conflicting with event: " + event.getName());
            }
        }
    }
    
    /**
     * Check if two time ranges overlap
     */
    private boolean isTimeOverlapping(LocalDateTime start1, LocalDateTime end1, 
                                    LocalDateTime start2, LocalDateTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    // ========== INSTALLATION & AVAILABILITY ==========

    /**
     * Check if an installation is available for the given time range
     */
    @Transactional(readOnly = true)
    public boolean isInstallationAvailable(Long installationId, LocalDateTime startAt, LocalDateTime endAt) {
        List<Event> installationEvents = eventRepository.findByInstallationId(installationId);
        
        for (Event event : installationEvents) {
            if (isTimeOverlapping(startAt, endAt, event.getStartAt(), event.getEndAt())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get available installations for a time range
     */
    @Transactional(readOnly = true)
    public List<Installation> getAvailableInstallations(LocalDateTime startAt, LocalDateTime endAt, 
                                                       Integer requiredCapacity, InstallationType installationType) {
        // Get all installations
        List<Installation> allInstallations = installationRepository.findAll();
        
        // Filter by type if specified
        if (installationType != null) {
            allInstallations = allInstallations.stream()
                .filter(installation -> installation.getInstallationType() == installationType)
                .toList();
        }
        
        // Filter by capacity and availability
        return allInstallations.stream()
            .filter(installation -> installation.getCapacity() >= requiredCapacity)
            .filter(installation -> isInstallationAvailable(installation.getId(), startAt, endAt))
            .toList();
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
        // Get all events and filter by date range manually
        List<Event> allEvents = eventRepository.findAll();
        return allEvents.stream()
            .filter(event -> !event.getStartAt().isBefore(startDate) && !event.getStartAt().isAfter(endDate))
            .toList();
    }

    /**
     * Get upcoming events
     */
    @Transactional(readOnly = true)
    public List<Event> getUpcomingEvents() {
        LocalDateTime now = LocalDateTime.now();
        List<Event> allEvents = eventRepository.findAll();
        return allEvents.stream()
            .filter(event -> !event.getStartAt().isBefore(now))
            .toList();
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
        long durationHours = ChronoUnit.HOURS.between(startAt, endAt);
        
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
        List<Event> allEvents = eventRepository.findAll();
        return allEvents.stream()
            .filter(event -> event.getInstallation().getInstallationType() == installationType)
            .toList();
    }

    /**
     * Get events by installation type with pagination
     */
    @Transactional(readOnly = true)
    public Page<Event> getEventsByInstallationType(InstallationType installationType, Pageable pageable) {
        List<Event> filteredEvents = getEventsByInstallationType(installationType);
        // Simple pagination implementation
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filteredEvents.size());
        List<Event> pageContent = filteredEvents.subList(start, end);
        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, filteredEvents.size());
    }

    // ========== STATISTICS & ANALYTICS ==========

    /**
     * Get event statistics
     */
    @Transactional(readOnly = true)
    public EventStatistics getEventStatistics() {
        List<Event> allEvents = eventRepository.findAll();
        long totalEvents = allEvents.size();
        
        LocalDateTime now = LocalDateTime.now();
        long upcomingEvents = allEvents.stream()
            .filter(event -> !event.getStartAt().isBefore(now))
            .count();
        long pastEvents = totalEvents - upcomingEvents;
        
        BigDecimal totalRevenue = allEvents.stream()
            .map(Event::getPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal averagePrice = totalEvents > 0 ? 
            totalRevenue.divide(BigDecimal.valueOf(totalEvents), 2, RoundingMode.HALF_UP) : 
            BigDecimal.ZERO;
        
        return new EventStatistics(totalEvents, upcomingEvents, pastEvents, totalRevenue, averagePrice);
    }

    /**
     * Get revenue by currency
     */
    @Transactional(readOnly = true)
    public BigDecimal getRevenueByCurrency(String currency) {
        List<Event> events = eventRepository.findByCurrency(currency);
        return events.stream()
            .map(Event::getPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
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
        Event event = getEventById(eventId);
        
        if (request.getName() != null) event.setName(request.getName());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getEventType() != null) event.setEventType(request.getEventType());
        if (request.getStartAt() != null) event.setStartAt(request.getStartAt());
        if (request.getEndAt() != null) event.setEndAt(request.getEndAt());
        if (request.getCapacity() != null) event.setCapacity(request.getCapacity());
        if (request.getPrice() != null) event.setPrice(request.getPrice());
        if (request.getCurrency() != null) event.setCurrency(request.getCurrency());
        if (request.getInstallationId() != null) {
            Installation installation = installationRepository.findById(request.getInstallationId())
                .orElseThrow(() -> new NotFoundException("Installation not found with ID: " + request.getInstallationId()));
            event.setInstallation(installation);
        }
        
        Event updatedEvent = updateEvent(
            event.getId(),
            event.getName(),
            event.getDescription(),
            event.getEventType(),
            event.getStartAt(),
            event.getEndAt(),
            event.getCapacity(),
            event.getPrice(),
            event.getCurrency(),
            event.getInstallation().getId()
        );
        return convertToResponseDto(updatedEvent);
    }
    
    /**
     * Search events as DTOs
     */
    @Transactional(readOnly = true)
    public Page<com.MyBooking.event.dto.EventResponseDto> searchEventsAsDto(com.MyBooking.event.dto.EventSearchCriteriaDto criteria, Pageable pageable) {
        Page<Event> events = searchEvents(criteria.getName(), criteria.getEventType(), criteria.getInstallationId(), 
                                        criteria.getMinPrice(), criteria.getMaxPrice(), "EUR", 
                                        criteria.getMinCapacity(), pageable);
        return events.map(this::convertToResponseDto);
    }
    
    /**
     * Get upcoming events as DTOs
     */
    @Transactional(readOnly = true)
    public Page<com.MyBooking.event.dto.EventResponseDto> getUpcomingEventsAsDto(Pageable pageable) {
        // For now, get all events and filter - in a real app, this should be a repository method
        Page<Event> allEvents = getAllEvents(pageable);
        return allEvents.map(this::convertToResponseDto);
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
     * Create event booking from DTO
     */
    public com.MyBooking.event.dto.EventBookingResponseDto createEventBookingAsDto(com.MyBooking.event.dto.EventBookingCreateRequestDto request, Long userId) {
        // Get event and validate
        Event event = getEventById(request.getEventId());
        
        // Get user and validate
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));
        
        // Get reservation if provided (optional for standalone event bookings)
        Reservation reservation = null;
        if (request.getReservationId() != null) {
            reservation = reservationRepository.findById(request.getReservationId())
                .orElseThrow(() -> new NotFoundException("Reservation not found with ID: " + request.getReservationId()));
        }
        
        // Validate participant count
        Integer numberOfParticipants = request.getNumberOfParticipants() != null ? request.getNumberOfParticipants() : 1;
        if (numberOfParticipants < 1) {
            throw new IllegalArgumentException("Number of participants must be at least 1");
        }
        if (numberOfParticipants > event.getCapacity()) {
            throw new IllegalArgumentException("Number of participants exceeds event capacity of " + event.getCapacity());
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
        booking.setDurationHours((int) ChronoUnit.HOURS.between(event.getStartAt(), event.getEndAt()));
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
    public com.MyBooking.event.dto.EventBookingResponseDto cancelEventBookingAsDto(Long bookingId, Long userId, String reason) {
        EventBooking booking = eventBookingRepository.findById(bookingId)
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
        EventBooking booking = eventBookingRepository.findById(bookingId)
            .orElseThrow(() -> new NotFoundException("Event booking not found with ID: " + bookingId));
        
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
