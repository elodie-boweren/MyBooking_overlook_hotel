//Core Event Management - create, update, delete, getById
//Validation & Business Rules - input validation, capacity checks, time validation
//Installation Availability - overlap detection, availability checks
//Search & Filtering - all search methods with various criteria
//Statistics & Analytics - revenue calculations, event counts
//Error Handling - NotFoundException, BusinessRuleException scenarios


package com.MyBooking.event.service;

import com.MyBooking.common.exception.BusinessRuleException;
import com.MyBooking.common.exception.NotFoundException;
import com.MyBooking.event.domain.Event;
import com.MyBooking.event.domain.EventType;
import com.MyBooking.event.repository.EventBookingRepository;
import com.MyBooking.event.repository.EventRepository;
import com.MyBooking.installation.domain.Installation;
import com.MyBooking.installation.domain.InstallationType;
import com.MyBooking.installation.repository.InstallationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventBookingRepository eventBookingRepository;

    @Mock
    private InstallationRepository installationRepository;

    @InjectMocks
    private EventService eventService;

    private Event testEvent;
    private Installation testInstallation;
    private LocalDateTime testStartTime;
    private LocalDateTime testEndTime;

    @BeforeEach
    void setUp() {
        // Setup test installation
        testInstallation = new Installation();
        testInstallation.setId(1L);
        testInstallation.setName("Conference Room A");
        testInstallation.setInstallationType(InstallationType.CONFERENCE_ROOM);
        testInstallation.setCapacity(50);
        testInstallation.setHourlyRate(new BigDecimal("100.00"));
        testInstallation.setCurrency("USD");

        // Setup test times
        testStartTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        testEndTime = testStartTime.plusHours(2);

        // Setup test event
        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setName("Team Meeting");
        testEvent.setDescription("Weekly team sync");
        testEvent.setEventType(EventType.CONFERENCE);
        testEvent.setStartAt(testStartTime);
        testEvent.setEndAt(testEndTime);
        testEvent.setCapacity(20);
        testEvent.setPrice(new BigDecimal("230.00")); // 100 * 2 hours + 15% service fee
        testEvent.setCurrency("USD");
        testEvent.setInstallation(testInstallation);
    }

    // ========== CORE EVENT MANAGEMENT TESTS ==========

    @Test
    void createEvent_WithValidData_ShouldCreateEvent() {
        // Given
        when(installationRepository.findById(1L)).thenReturn(Optional.of(testInstallation));
        when(eventRepository.findByInstallationId(1L)).thenReturn(Collections.emptyList());
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        // When
        Event result = eventService.createEvent(
            "Team Meeting", "Weekly team sync", EventType.CONFERENCE,
            testStartTime, testEndTime, 20, new BigDecimal("230.00"), "USD", 1L
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Team Meeting");
        assertThat(result.getEventType()).isEqualTo(EventType.CONFERENCE);
        assertThat(result.getCapacity()).isEqualTo(20);
        assertThat(result.getInstallation()).isEqualTo(testInstallation);

        verify(installationRepository).findById(1L);
        verify(eventRepository).findByInstallationId(1L);
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void createEvent_WithNonExistentInstallation_ShouldThrowNotFoundException() {
        // Given
        when(installationRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> eventService.createEvent(
            "Team Meeting", "Weekly team sync", EventType.CONFERENCE,
            testStartTime, testEndTime, 20, new BigDecimal("230.00"), "USD", 999L
        )).isInstanceOf(NotFoundException.class)
          .hasMessageContaining("Installation not found with ID: 999");

        verify(installationRepository).findById(999L);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void createEvent_WithCapacityExceedingInstallation_ShouldThrowBusinessRuleException() {
        // Given
        when(installationRepository.findById(1L)).thenReturn(Optional.of(testInstallation));

        // When & Then
        assertThatThrownBy(() -> eventService.createEvent(
            "Team Meeting", "Weekly team sync", EventType.CONFERENCE,
            testStartTime, testEndTime, 60, new BigDecimal("230.00"), "USD", 1L
        )).isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("Event capacity (60) exceeds installation capacity (50)");

        verify(installationRepository).findById(1L);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void createEvent_WithInvalidTimeRange_ShouldThrowBusinessRuleException() {
        // Given
        LocalDateTime invalidEndTime = testStartTime.minusHours(1); // End before start

        // When & Then
        assertThatThrownBy(() -> eventService.createEvent(
            "Team Meeting", "Weekly team sync", EventType.CONFERENCE,
            testStartTime, invalidEndTime, 20, new BigDecimal("230.00"), "USD", 1L
        )).isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("Event end time must be after start time");

        verify(installationRepository, never()).findById(any());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void createEvent_WithPastStartTime_ShouldThrowBusinessRuleException() {
        // Given
        LocalDateTime pastStartTime = LocalDateTime.now().minusHours(1);

        // When & Then
        assertThatThrownBy(() -> eventService.createEvent(
            "Team Meeting", "Weekly team sync", EventType.CONFERENCE,
            pastStartTime, testEndTime, 20, new BigDecimal("230.00"), "USD", 1L
        )).isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("Event start time cannot be in the past");

        verify(installationRepository, never()).findById(any());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void createEvent_WithInstallationConflict_ShouldThrowBusinessRuleException() {
        // Given
        Event conflictingEvent = new Event();
        conflictingEvent.setStartAt(testStartTime.plusMinutes(30));
        conflictingEvent.setEndAt(testEndTime.plusMinutes(30));

        when(installationRepository.findById(1L)).thenReturn(Optional.of(testInstallation));
        when(eventRepository.findByInstallationId(1L)).thenReturn(Arrays.asList(conflictingEvent));

        // When & Then
        assertThatThrownBy(() -> eventService.createEvent(
            "Team Meeting", "Weekly team sync", EventType.CONFERENCE,
            testStartTime, testEndTime, 20, new BigDecimal("230.00"), "USD", 1L
        )).isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("Installation is not available for the selected time");

        verify(installationRepository).findById(1L);
        verify(eventRepository).findByInstallationId(1L);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void updateEvent_WithValidData_ShouldUpdateEvent() {
        // Given
        Event updatedEvent = new Event();
        updatedEvent.setId(1L);
        updatedEvent.setName("Updated Meeting");
        updatedEvent.setEventType(EventType.CONFERENCE);
        updatedEvent.setStartAt(testStartTime.plusHours(1));
        updatedEvent.setEndAt(testEndTime.plusHours(1));
        updatedEvent.setCapacity(25);
        updatedEvent.setPrice(new BigDecimal("250.00"));
        updatedEvent.setCurrency("USD");
        updatedEvent.setInstallation(testInstallation);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(installationRepository.findById(1L)).thenReturn(Optional.of(testInstallation));
        when(eventRepository.findByInstallationId(1L)).thenReturn(Collections.emptyList());
        when(eventRepository.save(any(Event.class))).thenReturn(updatedEvent);

        // When
        Event result = eventService.updateEvent(1L, "Updated Meeting", "Updated description",
            EventType.CONFERENCE, testStartTime.plusHours(1), testEndTime.plusHours(1),
            25, new BigDecimal("250.00"), "USD", 1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Meeting");
        assertThat(result.getCapacity()).isEqualTo(25);

        verify(eventRepository).findById(1L);
        verify(installationRepository).findById(1L);
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void updateEvent_WithNonExistentEvent_ShouldThrowNotFoundException() {
        // Given
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> eventService.updateEvent(999L, "Updated Meeting", "Description",
            EventType.CONFERENCE, testStartTime, testEndTime, 20, new BigDecimal("230.00"), "USD", 1L
        )).isInstanceOf(NotFoundException.class)
          .hasMessageContaining("Event not found with ID: 999");

        verify(eventRepository).findById(999L);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void deleteEvent_WithValidEvent_ShouldDeleteEvent() {
        // Given - Create an event that starts more than 24 hours in the future
        Event futureEvent = new Event();
        futureEvent.setId(1L);
        futureEvent.setName("Future Event");
        futureEvent.setStartAt(LocalDateTime.now().plusDays(2)); // More than 24 hours away
        
        when(eventRepository.findById(1L)).thenReturn(Optional.of(futureEvent));
        when(eventBookingRepository.findByEventId(1L)).thenReturn(Collections.emptyList());
        doNothing().when(eventRepository).delete(any(Event.class));

        // When
        eventService.deleteEvent(1L, "Test deletion");

        // Then
        verify(eventRepository).findById(1L);
        verify(eventBookingRepository).findByEventId(1L);
        verify(eventRepository).delete(any(Event.class));
    }

    @Test
    void deleteEvent_WithNonExistentEvent_ShouldThrowNotFoundException() {
        // Given
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> eventService.deleteEvent(999L, "Test deletion"))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Event not found with ID: 999");

        verify(eventRepository).findById(999L);
        verify(eventRepository, never()).delete(any());
    }

    @Test
    void deleteEvent_Within24Hours_ShouldThrowBusinessRuleException() {
        // Given
        Event soonEvent = new Event();
        soonEvent.setId(1L);
        soonEvent.setStartAt(LocalDateTime.now().plusHours(12)); // Within 24 hours

        when(eventRepository.findById(1L)).thenReturn(Optional.of(soonEvent));

        // When & Then
        assertThatThrownBy(() -> eventService.deleteEvent(1L, "Test deletion"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Cannot delete events that start within 24 hours");

        verify(eventRepository).findById(1L);
        verify(eventRepository, never()).delete(any());
    }

    @Test
    void getEventById_WithValidId_ShouldReturnEvent() {
        // Given
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

        // When
        Event result = eventService.getEventById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Team Meeting");

        verify(eventRepository).findById(1L);
    }

    @Test
    void getEventById_WithNonExistentId_ShouldThrowNotFoundException() {
        // Given
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> eventService.getEventById(999L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Event not found with ID: 999");

        verify(eventRepository).findById(999L);
    }

    // ========== INSTALLATION AVAILABILITY TESTS ==========

    @Test
    void isInstallationAvailable_WithNoConflicts_ShouldReturnTrue() {
        // Given
        when(eventRepository.findByInstallationId(1L)).thenReturn(Collections.emptyList());

        // When
        boolean result = eventService.isInstallationAvailable(1L, testStartTime, testEndTime);

        // Then
        assertThat(result).isTrue();
        verify(eventRepository).findByInstallationId(1L);
    }

    @Test
    void isInstallationAvailable_WithConflicts_ShouldReturnFalse() {
        // Given
        Event conflictingEvent = new Event();
        conflictingEvent.setStartAt(testStartTime.plusMinutes(30));
        conflictingEvent.setEndAt(testEndTime.plusMinutes(30));

        when(eventRepository.findByInstallationId(1L)).thenReturn(Arrays.asList(conflictingEvent));

        // When
        boolean result = eventService.isInstallationAvailable(1L, testStartTime, testEndTime);

        // Then
        assertThat(result).isFalse();
        verify(eventRepository).findByInstallationId(1L);
    }

    // ========== SEARCH & FILTERING TESTS ==========

    @Test
    void searchEvents_WithValidCriteria_ShouldReturnFilteredResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Event> expectedPage = new PageImpl<>(Arrays.asList(testEvent), pageable, 1);

        when(eventRepository.findByCriteria("Team", EventType.CONFERENCE, 1L,
            new BigDecimal("200.00"), new BigDecimal("300.00"), 15, "USD", pageable))
            .thenReturn(expectedPage);

        // When
        Page<Event> result = eventService.searchEvents("Team", EventType.CONFERENCE, 1L,
            new BigDecimal("200.00"), new BigDecimal("300.00"), "USD", 15, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Team Meeting");

        verify(eventRepository).findByCriteria("Team", EventType.CONFERENCE, 1L,
            new BigDecimal("200.00"), new BigDecimal("300.00"), 15, "USD", pageable);
    }

    @Test
    void getEventsByType_ShouldReturnFilteredEvents() {
        // Given
        when(eventRepository.findByEventType(EventType.CONFERENCE)).thenReturn(Arrays.asList(testEvent));

        // When
        List<Event> result = eventService.getEventsByType(EventType.CONFERENCE);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEventType()).isEqualTo(EventType.CONFERENCE);

        verify(eventRepository).findByEventType(EventType.CONFERENCE);
    }

    @Test
    void getEventsByInstallation_ShouldReturnInstallationEvents() {
        // Given
        when(eventRepository.findByInstallationId(1L)).thenReturn(Arrays.asList(testEvent));

        // When
        List<Event> result = eventService.getEventsByInstallation(1L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInstallation().getId()).isEqualTo(1L);

        verify(eventRepository).findByInstallationId(1L);
    }

    @Test
    void getUpcomingEvents_ShouldReturnFutureEvents() {
        // Given
        Event pastEvent = new Event();
        pastEvent.setStartAt(LocalDateTime.now().minusDays(1));
        Event futureEvent = new Event();
        futureEvent.setStartAt(LocalDateTime.now().plusDays(1));

        when(eventRepository.findAll()).thenReturn(Arrays.asList(pastEvent, futureEvent, testEvent));

        // When
        List<Event> result = eventService.getUpcomingEvents();

        // Then
        assertThat(result).hasSize(2); // Only future events
        assertThat(result).allMatch(event -> !event.getStartAt().isBefore(LocalDateTime.now()));

        verify(eventRepository).findAll();
    }

    // ========== STATISTICS & ANALYTICS TESTS ==========

    @Test
    void getEventStatistics_ShouldReturnCorrectStatistics() {
        // Given
        Event pastEvent = new Event();
        pastEvent.setStartAt(LocalDateTime.now().minusDays(1));
        pastEvent.setPrice(new BigDecimal("100.00"));

        Event futureEvent = new Event();
        futureEvent.setStartAt(LocalDateTime.now().plusDays(1));
        futureEvent.setPrice(new BigDecimal("200.00"));

        when(eventRepository.findAll()).thenReturn(Arrays.asList(pastEvent, futureEvent, testEvent));

        // When
        EventService.EventStatistics result = eventService.getEventStatistics();

        // Then
        assertThat(result.getTotalEvents()).isEqualTo(3);
        assertThat(result.getUpcomingEvents()).isEqualTo(2);
        assertThat(result.getPastEvents()).isEqualTo(1);
        assertThat(result.getTotalRevenue()).isEqualTo(new BigDecimal("530.00")); // 100 + 200 + 230
        assertThat(result.getAveragePrice()).isEqualTo(new BigDecimal("176.67"));

        verify(eventRepository).findAll();
    }

    @Test
    void getRevenueByCurrency_ShouldReturnCorrectRevenue() {
        // Given
        Event usdEvent1 = new Event();
        usdEvent1.setCurrency("USD");
        usdEvent1.setPrice(new BigDecimal("100.00"));

        Event usdEvent2 = new Event();
        usdEvent2.setCurrency("USD");
        usdEvent2.setPrice(new BigDecimal("200.00"));

        when(eventRepository.findByCurrency("USD")).thenReturn(Arrays.asList(usdEvent1, usdEvent2));

        // When
        BigDecimal result = eventService.getRevenueByCurrency("USD");

        // Then
        assertThat(result).isEqualTo(new BigDecimal("300.00"));

        verify(eventRepository).findByCurrency("USD");
    }

    // ========== BUSINESS OPERATIONS TESTS ==========

    @Test
    void calculateEventPrice_ShouldCalculateCorrectPrice() {
        // Given
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusHours(3); // 3 hours

        // When
        BigDecimal result = eventService.calculateEventPrice(testInstallation, start, end);

        // Then
        // Expected: 100 * 3 hours + 15% service fee = 300 + 45 = 345
        assertThat(result).isEqualTo(new BigDecimal("345.00"));
    }

    @Test
    void calculateEventPrice_WithZeroHours_ShouldReturnZero() {
        // Given
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start; // Same time

        // When
        BigDecimal result = eventService.calculateEventPrice(testInstallation, start, end);

        // Then
        assertThat(result).isEqualTo(new BigDecimal("0.00"));
    }
}