package com.MyBooking.event.repository;

import com.MyBooking.event.domain.Event;
import com.MyBooking.event.domain.EventType;
import com.MyBooking.installation.domain.Installation;
import com.MyBooking.installation.domain.InstallationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.Rollback;
import com.MyBooking.hotel_management.HotelManagementApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("repository-test")
@ContextConfiguration(classes = HotelManagementApplication.class)
@EntityScan({"com.MyBooking.event.domain", "com.MyBooking.installation.domain", "com.MyBooking.auth.domain", "com.MyBooking.reservation.domain", "com.MyBooking.room.domain"})
@EnableJpaRepositories("com.MyBooking.event.repository")
class EventRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EventRepository eventRepository;

    private Installation spaInstallation;
    private Installation conferenceInstallation;
    private Installation fitnessInstallation;
    private Event spaEvent;
    private Event conferenceEvent;
    private Event yogaEvent;
    private Event fitnessEvent;
    private Event weddingEvent;
    private Event expensiveEvent;

    @BeforeEach
    @Transactional
    @Rollback
    void setUp() {
        // Create test installations
        spaInstallation = new Installation("Spa Center", InstallationType.SPA_ROOM, 50, new BigDecimal("1000.00"), "USD");
        entityManager.persistAndFlush(spaInstallation);

        conferenceInstallation = new Installation("Conference Hall", InstallationType.CONFERENCE_ROOM, 200, new BigDecimal("2000.00"), "USD");
        entityManager.persistAndFlush(conferenceInstallation);

        fitnessInstallation = new Installation("Fitness Center", InstallationType.GYM, 30, new BigDecimal("500.00"), "USD");
        entityManager.persistAndFlush(fitnessInstallation);

        // Create test events
        spaEvent = new Event("Relaxing Massage", EventType.SPA, 
                           LocalDateTime.of(2024, 1, 15, 10, 0),
                           LocalDateTime.of(2024, 1, 15, 11, 0),
                           1, new BigDecimal("150.00"), "USD", spaInstallation);
        spaEvent.setDescription("Professional massage therapy session");
        entityManager.persistAndFlush(spaEvent);

        conferenceEvent = new Event("Business Meeting", EventType.CONFERENCE,
                                  LocalDateTime.of(2024, 1, 20, 9, 0),
                                  LocalDateTime.of(2024, 1, 20, 17, 0),
                                  50, new BigDecimal("800.00"), "USD", conferenceInstallation);
        conferenceEvent.setDescription("Corporate business meeting");
        entityManager.persistAndFlush(conferenceEvent);

        yogaEvent = new Event("Morning Yoga", EventType.YOGA_CLASS,
                            LocalDateTime.of(2024, 1, 18, 7, 0),
                            LocalDateTime.of(2024, 1, 18, 8, 0),
                            20, new BigDecimal("25.00"), "USD", fitnessInstallation);
        yogaEvent.setDescription("Beginner-friendly yoga class");
        entityManager.persistAndFlush(yogaEvent);

        fitnessEvent = new Event("HIIT Training", EventType.FITNESS,
                               LocalDateTime.of(2024, 1, 22, 18, 0),
                               LocalDateTime.of(2024, 1, 22, 19, 0),
                               15, new BigDecimal("35.00"), "USD", fitnessInstallation);
        fitnessEvent.setDescription("High-intensity interval training");
        entityManager.persistAndFlush(fitnessEvent);

        weddingEvent = new Event("Wedding Reception", EventType.WEDDING,
                               LocalDateTime.of(2024, 2, 14, 18, 0),
                               LocalDateTime.of(2024, 2, 14, 23, 0),
                               100, new BigDecimal("5000.00"), "USD", conferenceInstallation);
        weddingEvent.setDescription("Elegant wedding reception");
        entityManager.persistAndFlush(weddingEvent);

        expensiveEvent = new Event("Premium Spa Package", EventType.SPA,
                                 LocalDateTime.of(2024, 1, 25, 14, 0),
                                 LocalDateTime.of(2024, 1, 25, 17, 0),
                                 1, new BigDecimal("300.00"), "USD", spaInstallation);
        expensiveEvent.setDescription("Luxury spa treatment package");
        entityManager.persistAndFlush(expensiveEvent);
    }

    // ==================== BASIC FINDER METHODS TESTS ====================

    @Test
    void testFindByName() {
        Optional<Event> found = eventRepository.findByName("Relaxing Massage");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Relaxing Massage");
    }

    @Test
    void testFindByNameNotFound() {
        Optional<Event> found = eventRepository.findByName("Non-existent Event");
        assertThat(found).isEmpty();
    }

    @Test
    void testExistsByName() {
        assertThat(eventRepository.existsByName("Relaxing Massage")).isTrue();
        assertThat(eventRepository.existsByName("Non-existent Event")).isFalse();
    }

    @Test
    void testFindByEventType() {
        List<Event> spaEvents = eventRepository.findByEventType(EventType.SPA);
        assertThat(spaEvents).hasSize(2);
        assertThat(spaEvents).extracting(Event::getName)
                .containsExactlyInAnyOrder("Relaxing Massage", "Premium Spa Package");
    }

    @Test
    void testFindByEventTypeWithPagination() {
        Pageable pageable = PageRequest.of(0, 1);
        Page<Event> spaEventsPage = eventRepository.findByEventType(EventType.SPA, pageable);
        assertThat(spaEventsPage.getContent()).hasSize(1);
        assertThat(spaEventsPage.getTotalElements()).isEqualTo(2);
    }

    @Test
    void testFindByInstallation() {
        List<Event> spaInstallationEvents = eventRepository.findByInstallation(spaInstallation);
        assertThat(spaInstallationEvents).hasSize(2);
        assertThat(spaInstallationEvents).extracting(Event::getName)
                .containsExactlyInAnyOrder("Relaxing Massage", "Premium Spa Package");
    }

    @Test
    void testFindByInstallationId() {
        List<Event> conferenceEvents = eventRepository.findByInstallationId(conferenceInstallation.getId());
        assertThat(conferenceEvents).hasSize(2);
        assertThat(conferenceEvents).extracting(Event::getName)
                .containsExactlyInAnyOrder("Business Meeting", "Wedding Reception");
    }

    @Test
    void testFindByInstallationIdWithPagination() {
        Pageable pageable = PageRequest.of(0, 1);
        Page<Event> conferenceEventsPage = eventRepository.findByInstallationId(conferenceInstallation.getId(), pageable);
        assertThat(conferenceEventsPage.getContent()).hasSize(1);
        assertThat(conferenceEventsPage.getTotalElements()).isEqualTo(2);
    }

    // ==================== PRICE-BASED QUERIES TESTS ====================

    @Test
    void testFindByPriceBetween() {
        List<Event> events = eventRepository.findByPriceBetween(new BigDecimal("100.00"), new BigDecimal("200.00"));
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getName()).isEqualTo("Relaxing Massage");
    }

    @Test
    void testFindByPriceBetweenWithPagination() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Event> eventsPage = eventRepository.findByPriceBetween(new BigDecimal("0.00"), new BigDecimal("100.00"), pageable);
        assertThat(eventsPage.getContent()).hasSize(2);
        assertThat(eventsPage.getTotalElements()).isEqualTo(2);
    }

    @Test
    void testFindByPriceGreaterThanEqual() {
        List<Event> events = eventRepository.findByPriceGreaterThanEqual(new BigDecimal("500.00"));
        assertThat(events).hasSize(2);
        assertThat(events).extracting(Event::getName)
                .containsExactlyInAnyOrder("Business Meeting", "Wedding Reception");
    }

    @Test
    void testFindByPriceLessThanEqual() {
        List<Event> events = eventRepository.findByPriceLessThanEqual(new BigDecimal("50.00"));
        assertThat(events).hasSize(2);
        assertThat(events).extracting(Event::getName)
                .containsExactlyInAnyOrder("Morning Yoga", "HIIT Training");
    }

    // ==================== CAPACITY-BASED QUERIES TESTS ====================

    @Test
    void testFindByCapacityBetween() {
        List<Event> events = eventRepository.findByCapacityBetween(10, 30);
        assertThat(events).hasSize(2);
        assertThat(events).extracting(Event::getName)
                .containsExactlyInAnyOrder("Morning Yoga", "HIIT Training");
    }

    @Test
    void testFindByCapacityGreaterThanEqual() {
        List<Event> events = eventRepository.findByCapacityGreaterThanEqual(50);
        assertThat(events).hasSize(2);
        assertThat(events).extracting(Event::getName)
                .containsExactlyInAnyOrder("Business Meeting", "Wedding Reception");
    }

    @Test
    void testFindByCapacityLessThanEqual() {
        List<Event> events = eventRepository.findByCapacityLessThanEqual(20);
        assertThat(events).hasSize(4);
        assertThat(events).extracting(Event::getName)
                .containsExactlyInAnyOrder("Relaxing Massage", "Morning Yoga", "HIIT Training", "Premium Spa Package");
    }

    // ==================== CURRENCY-BASED QUERIES TESTS ====================

    @Test
    void testFindByCurrency() {
        List<Event> events = eventRepository.findByCurrency("USD");
        assertThat(events).hasSize(6);
    }

    @Test
    void testFindByCurrencyWithPagination() {
        Pageable pageable = PageRequest.of(0, 3);
        Page<Event> eventsPage = eventRepository.findByCurrency("USD", pageable);
        assertThat(eventsPage.getContent()).hasSize(3);
        assertThat(eventsPage.getTotalElements()).isEqualTo(6);
    }

    // ==================== SEARCH AND FILTERING QUERIES TESTS ====================

    @Test
    void testFindByNameContainingIgnoreCase() {
        List<Event> events = eventRepository.findByNameContainingIgnoreCase("yoga");
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getName()).isEqualTo("Morning Yoga");
    }

    @Test
    void testFindByKeyword() {
        List<Event> events = eventRepository.findByKeyword("spa");
        assertThat(events).hasSize(1);
        assertThat(events).extracting(Event::getName)
                .containsExactlyInAnyOrder("Premium Spa Package");
    }

    // ==================== TIME-BASED QUERIES TESTS ====================

    @Test
    void testFindByStartTimeBetween() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 20, 0, 0);
        List<Event> events = eventRepository.findByStartTimeBetween(start, end);
        assertThat(events).hasSize(2);
        assertThat(events).extracting(Event::getName)
                .containsExactlyInAnyOrder("Relaxing Massage", "Morning Yoga");
    }

    @Test
    void testCheckEventAvailability() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 15, 10, 30);
        LocalDateTime end = LocalDateTime.of(2024, 1, 15, 11, 30);
        List<Event> conflictingEvents = eventRepository.checkEventAvailability(start, end);
        assertThat(conflictingEvents).hasSize(1);
        assertThat(conflictingEvents.get(0).getName()).isEqualTo("Relaxing Massage");
    }

    @Test
    void testCheckEventAvailabilityNoConflict() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 16, 10, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 16, 11, 0);
        List<Event> conflictingEvents = eventRepository.checkEventAvailability(start, end);
        assertThat(conflictingEvents).isEmpty();
    }

    // ==================== COMBINED CRITERIA QUERIES TESTS ====================

    @Test
    void testFindByEventTypeAndPriceRange() {
        List<Event> events = eventRepository.findByEventTypeAndPriceRange(EventType.SPA, 
                                                                         new BigDecimal("100.00"), 
                                                                         new BigDecimal("200.00"));
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getName()).isEqualTo("Relaxing Massage");
    }

    @Test
    void testFindByInstallationAndMinCapacity() {
        List<Event> events = eventRepository.findByInstallationAndMinCapacity(spaInstallation.getId(), 1);
        assertThat(events).hasSize(2);
        assertThat(events).extracting(Event::getName)
                .containsExactlyInAnyOrder("Relaxing Massage", "Premium Spa Package");
    }

    @Test
    void testFindEventsForParticipants() {
        List<Event> events = eventRepository.findEventsForParticipants(25);
        assertThat(events).hasSize(2);
        assertThat(events).extracting(Event::getName)
                .containsExactlyInAnyOrder("Business Meeting", "Wedding Reception");
    }

    // ==================== STATISTICS AND ANALYTICS TESTS ====================

    @Test
    void testCountByEventType() {
        Long spaCount = eventRepository.countByEventType(EventType.SPA);
        assertThat(spaCount).isEqualTo(2L);
    }

    @Test
    void testCountByInstallationId() {
        Long conferenceCount = eventRepository.countByInstallationId(conferenceInstallation.getId());
        assertThat(conferenceCount).isEqualTo(2L);
    }

    @Test
    void testCountByCurrency() {
        Long usdCount = eventRepository.countByCurrency("USD");
        assertThat(usdCount).isEqualTo(6L);
    }

    @Test
    void testGetAveragePriceByEventType() {
        Optional<BigDecimal> avgPrice = eventRepository.getAveragePriceByEventType(EventType.SPA);
        assertThat(avgPrice).isPresent();
        assertThat(avgPrice.get()).isEqualByComparingTo(new BigDecimal("225.00")); // (150 + 300) / 2
    }

    @Test
    void testGetAveragePriceByCurrency() {
        Optional<BigDecimal> avgPrice = eventRepository.getAveragePriceByCurrency("USD");
        assertThat(avgPrice).isPresent();
        // Total: 150 + 800 + 25 + 35 + 5000 + 300 = 6310, Average: 6310 / 6 = 1051.666666666667
        assertThat(avgPrice.get()).isEqualByComparingTo(new BigDecimal("1051.666666666667"));
    }

    @Test
    void testGetMinPriceByEventType() {
        Optional<BigDecimal> minPrice = eventRepository.getMinPriceByEventType(EventType.SPA);
        assertThat(minPrice).isPresent();
        assertThat(minPrice.get()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    void testGetMaxPriceByEventType() {
        Optional<BigDecimal> maxPrice = eventRepository.getMaxPriceByEventType(EventType.SPA);
        assertThat(maxPrice).isPresent();
        assertThat(maxPrice.get()).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    void testFindCheapestEvents() {
        List<Event> cheapestEvents = eventRepository.findCheapestEvents();
        assertThat(cheapestEvents).hasSize(1);
        assertThat(cheapestEvents.get(0).getName()).isEqualTo("Morning Yoga");
    }

    @Test
    void testFindMostExpensiveEvents() {
        List<Event> mostExpensiveEvents = eventRepository.findMostExpensiveEvents();
        assertThat(mostExpensiveEvents).hasSize(1);
        assertThat(mostExpensiveEvents.get(0).getName()).isEqualTo("Wedding Reception");
    }

    // ==================== EXISTENCE CHECKS TESTS ====================

    @Test
    void testExistsByEventType() {
        assertThat(eventRepository.existsByEventType(EventType.SPA)).isTrue();
        assertThat(eventRepository.existsByEventType(EventType.CONFERENCE)).isTrue();
    }

    @Test
    void testExistsByInstallationId() {
        assertThat(eventRepository.existsByInstallationId(spaInstallation.getId())).isTrue();
        assertThat(eventRepository.existsByInstallationId(999L)).isFalse();
    }

    @Test
    void testExistsByCurrency() {
        assertThat(eventRepository.existsByCurrency("USD")).isTrue();
        assertThat(eventRepository.existsByCurrency("EUR")).isFalse();
    }

    // ==================== ADVANCED SEARCH TESTS ====================

    @Test
    void testFindByCriteria() {
        List<Event> events = eventRepository.findByCriteria("Yoga", EventType.YOGA_CLASS, null, 
                                                           new BigDecimal("20.00"), new BigDecimal("30.00"), 
                                                           15, "USD");
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getName()).isEqualTo("Morning Yoga");
    }

    @Test
    void testFindByCriteriaWithPagination() {
        Pageable pageable = PageRequest.of(0, 2);
        Page<Event> eventsPage = eventRepository.findByCriteria(null, EventType.SPA, null, 
                                                               new BigDecimal("100.00"), new BigDecimal("400.00"), 
                                                               1, "USD", pageable);
        assertThat(eventsPage.getContent()).hasSize(2);
        assertThat(eventsPage.getTotalElements()).isEqualTo(2);
    }

    @Test
    void testExistsOverlappingEventForInstallationAndSelfExclusion() {
        // spaEvent is 2024-01-15 10:00 to 11:00 on spaInstallation
        LocalDateTime overlappingStart = LocalDateTime.of(2024, 1, 15, 10, 30);
        LocalDateTime overlappingEnd = LocalDateTime.of(2024, 1, 15, 11, 30);
        LocalDateTime adjacentStart = LocalDateTime.of(2024, 1, 15, 11, 0);
        LocalDateTime adjacentEnd = LocalDateTime.of(2024, 1, 15, 12, 0);

        assertThat(eventRepository.existsOverlappingEventForInstallation(
                spaInstallation.getId(), overlappingStart, overlappingEnd)).isTrue();
        assertThat(eventRepository.existsOverlappingEventForInstallation(
                spaInstallation.getId(), adjacentStart, adjacentEnd)).isFalse();
        assertThat(eventRepository.existsOverlappingEventForInstallation(
                conferenceInstallation.getId(), overlappingStart, overlappingEnd)).isFalse();

        // Excluding spaEvent itself should allow the update
        assertThat(eventRepository.existsOverlappingEventForInstallationExcludingEvent(
                spaInstallation.getId(), overlappingStart, overlappingEnd, spaEvent.getId())).isFalse();
        // Excluding a different event still detects spaEvent as conflicting
        assertThat(eventRepository.existsOverlappingEventForInstallationExcludingEvent(
                spaInstallation.getId(), overlappingStart, overlappingEnd, conferenceEvent.getId())).isTrue();
    }
}