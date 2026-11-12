package com.MyBooking.event.repository;

import com.MyBooking.event.domain.Event;
import com.MyBooking.event.domain.EventBooking;
import com.MyBooking.event.domain.EventBookingStatus;
import com.MyBooking.event.domain.EventType;
import com.MyBooking.installation.domain.Installation;
import com.MyBooking.installation.domain.InstallationType;
import com.MyBooking.reservation.domain.Reservation;
import com.MyBooking.reservation.domain.ReservationStatus;
import com.MyBooking.room.domain.Room;
import com.MyBooking.room.domain.RoomStatus;
import com.MyBooking.room.domain.RoomType;
import com.MyBooking.auth.domain.User;
import com.MyBooking.auth.domain.Role;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("repository-test")
@ContextConfiguration(classes = HotelManagementApplication.class)
@EntityScan({"com.MyBooking.event.domain", "com.MyBooking.installation.domain", "com.MyBooking.auth.domain", "com.MyBooking.reservation.domain", "com.MyBooking.room.domain"})
@EnableJpaRepositories("com.MyBooking.event.repository")
class EventBookingRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EventBookingRepository eventBookingRepository;

    private User client1;
    private User client2;
    private User client3;
    private Room room1;
    private Room room2;
    private Reservation reservation1;
    private Reservation reservation2;
    private Installation spaInstallation;
    private Installation conferenceInstallation;
    private Event spaEvent;
    private Event conferenceEvent;
    private Event yogaEvent;
    private EventBooking spaBooking;
    private EventBooking conferenceBooking;
    private EventBooking yogaBooking;
    private EventBooking cancelledBooking;
    private EventBooking expensiveBooking;

    @BeforeEach
    @Transactional
    @Rollback
    void setUp() {
        // Create test users
        client1 = new User("John", "Doe", "john.doe@email.com", "password123", 
                          "1234567890", "123 Main St", LocalDate.of(1990, 1, 1), Role.CLIENT);
        entityManager.persistAndFlush(client1);

        client2 = new User("Jane", "Smith", "jane.smith@email.com", "password123", 
                          "1987654321", "456 Oak Ave", LocalDate.of(1985, 5, 15), Role.CLIENT);
        entityManager.persistAndFlush(client2);

        client3 = new User("Bob", "Johnson", "bob.johnson@email.com", "password123", 
                          "5555555555", "789 Pine Rd", LocalDate.of(1992, 8, 20), Role.CLIENT);
        entityManager.persistAndFlush(client3);

        // Create test rooms
        room1 = new Room("101", RoomType.SINGLE, 1, new BigDecimal("100.00"), "USD", RoomStatus.AVAILABLE);
        room1.setDescription("Cozy single room");
        entityManager.persistAndFlush(room1);

        room2 = new Room("102", RoomType.DOUBLE, 2, new BigDecimal("150.00"), "USD", RoomStatus.AVAILABLE);
        room2.setDescription("Comfortable double room");
        entityManager.persistAndFlush(room2);

        // Create test reservations
        reservation1 = new Reservation(LocalDate.of(2024, 1, 15), LocalDate.of(2024, 1, 18), 
                                     2, new BigDecimal("300.00"), "USD", ReservationStatus.CONFIRMED, client1, room1);
        entityManager.persistAndFlush(reservation1);

        reservation2 = new Reservation(LocalDate.of(2024, 1, 20), LocalDate.of(2024, 1, 22), 
                                     2, new BigDecimal("300.00"), "USD", ReservationStatus.CONFIRMED, client2, room2);
        entityManager.persistAndFlush(reservation2);

        // Create test installations
        spaInstallation = new Installation("Spa Center", InstallationType.SPA_ROOM, 50, new BigDecimal("1000.00"), "USD");
        spaInstallation.setDescription("Luxury spa with multiple treatment rooms");
        entityManager.persistAndFlush(spaInstallation);

        conferenceInstallation = new Installation("Conference Hall", InstallationType.CONFERENCE_ROOM, 200, new BigDecimal("2000.00"), "USD");
        conferenceInstallation.setDescription("Large conference room with AV equipment");
        entityManager.persistAndFlush(conferenceInstallation);

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
                            20, new BigDecimal("25.00"), "USD", spaInstallation);
        yogaEvent.setDescription("Beginner-friendly yoga class");
        entityManager.persistAndFlush(yogaEvent);

        // Create test event bookings using the correct constructor
        spaBooking = new EventBooking(spaEvent, client1, 
                                    LocalDateTime.of(2024, 1, 15, 10, 0), 1, 
                                    LocalDateTime.of(2024, 1, 10, 0, 0), 1, new BigDecimal("150.00"), 
                                    EventBookingStatus.CONFIRMED);
        spaBooking.setReservation(reservation1); // Set reservation separately
        entityManager.persistAndFlush(spaBooking);

        conferenceBooking = new EventBooking(conferenceEvent, client2,
                                           LocalDateTime.of(2024, 1, 20, 9, 0), 8,
                                           LocalDateTime.of(2024, 1, 15, 0, 0), 25, new BigDecimal("800.00"),
                                           EventBookingStatus.CONFIRMED);
        conferenceBooking.setReservation(reservation2); // Set reservation separately
        entityManager.persistAndFlush(conferenceBooking);

        yogaBooking = new EventBooking(yogaEvent, client3,
                                     LocalDateTime.of(2024, 1, 18, 7, 0), 1,
                                     LocalDateTime.of(2024, 1, 12, 0, 0), 15, new BigDecimal("25.00"),
                                     EventBookingStatus.CONFIRMED);
        // No reservation for yoga booking
        entityManager.persistAndFlush(yogaBooking);

        cancelledBooking = new EventBooking(spaEvent, client1,
                                          LocalDateTime.of(2024, 1, 25, 14, 0), 1,
                                          LocalDateTime.of(2024, 1, 20, 0, 0), 1, new BigDecimal("150.00"),
                                          EventBookingStatus.CANCELLED);
        // No reservation for cancelled booking
        entityManager.persistAndFlush(cancelledBooking);

        expensiveBooking = new EventBooking(conferenceEvent, client2,
                                          LocalDateTime.of(2024, 2, 1, 9, 0), 8,
                                          LocalDateTime.of(2024, 1, 25, 0, 0), 50, new BigDecimal("800.00"),
                                          EventBookingStatus.CONFIRMED);
        // No reservation for expensive booking
        entityManager.persistAndFlush(expensiveBooking);
    }

    // ==================== CLIENT-BASED QUERIES ====================

    @Test
    void testFindByClient() {
        List<EventBooking> bookings = eventBookingRepository.findByClient(client1);
        assertThat(bookings).hasSize(2);
        assertThat(bookings).extracting("client.email")
                .containsExactlyInAnyOrder("john.doe@email.com", "john.doe@email.com");
    }

    @Test
    void testFindByClientId() {
        List<EventBooking> bookings = eventBookingRepository.findByClientId(client2.getId());
        assertThat(bookings).hasSize(2);
        assertThat(bookings).extracting("client.email")
                .containsExactlyInAnyOrder("jane.smith@email.com", "jane.smith@email.com");
    }

    // ==================== EVENT-BASED QUERIES ====================

    @Test
    void testFindByEvent() {
        List<EventBooking> bookings = eventBookingRepository.findByEvent(spaEvent);
        assertThat(bookings).hasSize(2);
        assertThat(bookings).extracting("event.name")
                .containsExactlyInAnyOrder("Relaxing Massage", "Relaxing Massage");
    }

    @Test
    void testFindByEventId() {
        List<EventBooking> bookings = eventBookingRepository.findByEventId(conferenceEvent.getId());
        assertThat(bookings).hasSize(2);
        assertThat(bookings).extracting("event.name")
                .containsExactlyInAnyOrder("Business Meeting", "Business Meeting");
    }

    // ==================== RESERVATION-BASED QUERIES ====================

    @Test
    void testFindByReservation() {
        List<EventBooking> bookings = eventBookingRepository.findByReservation(reservation1);
        assertThat(bookings).hasSize(1);
        assertThat(bookings.get(0).getReservation().getId()).isEqualTo(reservation1.getId());
    }

    @Test
    void testFindByReservationId() {
        List<EventBooking> bookings = eventBookingRepository.findByReservationId(reservation2.getId());
        assertThat(bookings).hasSize(1);
        assertThat(bookings.get(0).getReservation().getId()).isEqualTo(reservation2.getId());
    }

    // ==================== STATUS-BASED QUERIES ====================

    @Test
    void testFindByStatus() {
        List<EventBooking> confirmedBookings = eventBookingRepository.findByStatus(EventBookingStatus.CONFIRMED);
        assertThat(confirmedBookings).hasSize(4);
        assertThat(confirmedBookings).extracting("status")
                .containsOnly(EventBookingStatus.CONFIRMED);

        List<EventBooking> cancelledBookings = eventBookingRepository.findByStatus(EventBookingStatus.CANCELLED);
        assertThat(cancelledBookings).hasSize(1);
        assertThat(cancelledBookings).extracting("status")
                .containsOnly(EventBookingStatus.CANCELLED);
    }

    // ==================== DATE/TIME-BASED QUERIES ====================

    @Test
    void testFindByEventDateTimeBetween() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 20, 0, 0);
        List<EventBooking> bookings = eventBookingRepository.findByEventDateTimeBetween(start, end);
        assertThat(bookings).hasSize(2);
        assertThat(bookings).extracting("event.name")
                .containsExactlyInAnyOrder("Relaxing Massage", "Morning Yoga");
    }

    @Test
    void testFindByBookingDateBetween() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 10, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 15, 0, 0);
        List<EventBooking> bookings = eventBookingRepository.findByBookingDateBetween(start, end);
        assertThat(bookings).hasSize(3);
        assertThat(bookings).extracting("bookingDate")
                .containsExactlyInAnyOrder(LocalDateTime.of(2024, 1, 10, 0, 0), LocalDateTime.of(2024, 1, 15, 0, 0), LocalDateTime.of(2024, 1, 12, 0, 0));
    }

    // ==================== PRICE-BASED QUERIES ====================

    @Test
    void testFindByTotalPriceBetween() {
        BigDecimal minPrice = new BigDecimal("100.00");
        BigDecimal maxPrice = new BigDecimal("200.00");
        List<EventBooking> bookings = eventBookingRepository.findByTotalPriceBetween(minPrice, maxPrice);
        assertThat(bookings).hasSize(2);
        assertThat(bookings).extracting("totalPrice")
                .containsExactlyInAnyOrder(new BigDecimal("150.00"), new BigDecimal("150.00"));
    }

    @Test
    void testFindByTotalPriceGreaterThanEqual() {
        BigDecimal minPrice = new BigDecimal("500.00");
        List<EventBooking> bookings = eventBookingRepository.findByTotalPriceGreaterThanEqual(minPrice);
        assertThat(bookings).hasSize(2);
        assertThat(bookings).extracting("totalPrice")
                .containsExactlyInAnyOrder(new BigDecimal("800.00"), new BigDecimal("800.00"));
    }

    @Test
    void testFindByTotalPriceLessThanEqual() {
        BigDecimal maxPrice = new BigDecimal("100.00");
        List<EventBooking> bookings = eventBookingRepository.findByTotalPriceLessThanEqual(maxPrice);
        assertThat(bookings).hasSize(1);
        assertThat(bookings.get(0).getTotalPrice()).isEqualTo(new BigDecimal("25.00"));
    }

    // ==================== PARTICIPANT-BASED QUERIES ====================

    @Test
    void testFindByNumberOfParticipantsBetween() {
        List<EventBooking> bookings = eventBookingRepository.findByNumberOfParticipantsBetween(10, 30);
        assertThat(bookings).hasSize(2);
        assertThat(bookings).extracting("numberOfParticipants")
                .containsExactlyInAnyOrder(15, 25);
    }

    @Test
    void testFindByNumberOfParticipantsGreaterThanEqual() {
        List<EventBooking> bookings = eventBookingRepository.findByNumberOfParticipantsGreaterThanEqual(20);
        assertThat(bookings).hasSize(2);
        assertThat(bookings).extracting("numberOfParticipants")
                .containsExactlyInAnyOrder(25, 50);
    }

    @Test
    void testFindByNumberOfParticipantsLessThanEqual() {
        List<EventBooking> bookings = eventBookingRepository.findByNumberOfParticipantsLessThanEqual(10);
        assertThat(bookings).hasSize(2);
        assertThat(bookings).extracting("numberOfParticipants")
                .containsExactlyInAnyOrder(1, 1);
    }

    // ==================== DURATION-BASED QUERIES ====================

    @Test
    void testFindByDurationHoursBetween() {
        List<EventBooking> bookings = eventBookingRepository.findByDurationHoursBetween(5, 10);
        assertThat(bookings).hasSize(2);
        assertThat(bookings).extracting("durationHours")
                .containsExactlyInAnyOrder(8, 8);
    }

    @Test
    void testFindByDurationHoursGreaterThanEqual() {
        List<EventBooking> bookings = eventBookingRepository.findByDurationHoursGreaterThanEqual(5);
        assertThat(bookings).hasSize(2);
        assertThat(bookings).extracting("durationHours")
                .containsExactlyInAnyOrder(8, 8);
    }

    @Test
    void testFindByDurationHoursLessThanEqual() {
        List<EventBooking> bookings = eventBookingRepository.findByDurationHoursLessThanEqual(5);
        assertThat(bookings).hasSize(3);
        assertThat(bookings).extracting("durationHours")
                .containsExactlyInAnyOrder(1, 1, 1);
    }

    // ==================== COMBINED CRITERIA QUERIES ====================

    @Test
    void testFindByEventIdAndStatus() {
        List<EventBooking> bookings = eventBookingRepository.findByEventIdAndStatus(spaEvent.getId(), EventBookingStatus.CONFIRMED);
        assertThat(bookings).hasSize(1);
        assertThat(bookings.get(0).getEvent().getId()).isEqualTo(spaEvent.getId());
        assertThat(bookings.get(0).getStatus()).isEqualTo(EventBookingStatus.CONFIRMED);
    }

    @Test
    void testFindByClientIdAndStatus() {
        List<EventBooking> bookings = eventBookingRepository.findByClientIdAndStatus(client1.getId(), EventBookingStatus.CONFIRMED);
        assertThat(bookings).hasSize(1);
        assertThat(bookings.get(0).getClient().getId()).isEqualTo(client1.getId());
        assertThat(bookings.get(0).getStatus()).isEqualTo(EventBookingStatus.CONFIRMED);
    }

    // ==================== ADVANCED SEARCH WITH MULTIPLE CRITERIA ====================

    @Test
    void testFindByCriteria() {
        List<EventBooking> bookings = eventBookingRepository.findByCriteria(
                spaEvent.getId(), client1.getId(), EventBookingStatus.CONFIRMED,
                new BigDecimal("100.00"), new BigDecimal("200.00"),
                1, 2, 1, 2);
        assertThat(bookings).hasSize(1);
        assertThat(bookings.get(0).getEvent().getId()).isEqualTo(spaEvent.getId());
        assertThat(bookings.get(0).getClient().getId()).isEqualTo(client1.getId());
    }

    @Test
    void testFindByCriteriaWithNulls() {
        List<EventBooking> bookings = eventBookingRepository.findByCriteria(
                null, null, EventBookingStatus.CONFIRMED,
                null, null, null, null, null, null);
        assertThat(bookings).hasSize(4);
        assertThat(bookings).extracting("status").containsOnly(EventBookingStatus.CONFIRMED);
    }

    @Test
    void testCountConfirmedParticipantsByEventId_SumsConfirmedParticipants() {
        int total = eventBookingRepository.countConfirmedParticipantsByEventId(conferenceEvent.getId());
        // conferenceBooking (25) + expensiveBooking (50) = 75
        assertThat(total).isEqualTo(75);
    }

    @Test
    void testCountConfirmedParticipantsByEventId_ExcludesCancelledBookings() {
        int total = eventBookingRepository.countConfirmedParticipantsByEventId(spaEvent.getId());
        // spaBooking (1 confirmed) + cancelledBooking (1 cancelled) = 1
        assertThat(total).isEqualTo(1);
    }

    @Test
    void testCountConfirmedParticipantsByEventId_WithNoBookings_ReturnsZero() {
        Event unusedEvent = new Event("Empty Event", EventType.FITNESS,
                                      LocalDateTime.of(2024, 3, 1, 10, 0),
                                      LocalDateTime.of(2024, 3, 1, 12, 0),
                                      30, new BigDecimal("50.00"), "USD", conferenceInstallation);
        entityManager.persistAndFlush(unusedEvent);

        int total = eventBookingRepository.countConfirmedParticipantsByEventId(unusedEvent.getId());
        assertThat(total).isEqualTo(0);
    }

    // ==================== PAGINATION TESTS ====================

    @Test
    void testFindByStatusWithPagination() {
        Pageable pageable = PageRequest.of(0, 2);
        Page<EventBooking> page = eventBookingRepository.findByStatus(EventBookingStatus.CONFIRMED, pageable);
        
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getNumber()).isEqualTo(0);
        assertThat(page.getSize()).isEqualTo(2);
    }

    @Test
    void testFindByClientIdWithPagination() {
        Pageable pageable = PageRequest.of(0, 1);
        Page<EventBooking> page = eventBookingRepository.findByClientId(client1.getId(), pageable);
        
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    // ==================== STATISTICS QUERIES ====================

    @Test
    void testCountByStatus() {
        long confirmedCount = eventBookingRepository.countByStatus(EventBookingStatus.CONFIRMED);
        assertThat(confirmedCount).isEqualTo(4);

        long cancelledCount = eventBookingRepository.countByStatus(EventBookingStatus.CANCELLED);
        assertThat(cancelledCount).isEqualTo(1);
    }

    @Test
    void testGetTotalRevenueByStatus() {
        Optional<BigDecimal> confirmedRevenue = eventBookingRepository.getTotalRevenueByStatus(EventBookingStatus.CONFIRMED);
        assertThat(confirmedRevenue).isPresent();
        // 150 + 800 + 25 + 800 = 1775
        assertThat(confirmedRevenue.get()).isEqualByComparingTo(new BigDecimal("1775.00"));

        Optional<BigDecimal> cancelledRevenue = eventBookingRepository.getTotalRevenueByStatus(EventBookingStatus.CANCELLED);
        assertThat(cancelledRevenue).isPresent();
        assertThat(cancelledRevenue.get()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    void testCalculateTotalRevenueBetweenDates() {
        // Query January 2024: 150 (spa) + 800 (conference) + 25 (yoga) = 975.00 (excluding February expensiveBooking 800)
        LocalDateTime janStart = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime janEnd = LocalDateTime.of(2024, 1, 31, 23, 59, 59);

        BigDecimal janRevenue = eventBookingRepository.calculateTotalRevenueBetweenDates(
            janStart, janEnd, EventBookingStatus.CONFIRMED);
        assertThat(janRevenue).isEqualByComparingTo(new BigDecimal("975.00"));

        // Query Jan 1 to Feb 28 (including February expensiveBooking 800): 975 + 800 = 1775.00
        LocalDateTime febEnd = LocalDateTime.of(2024, 2, 29, 23, 59, 59);
        BigDecimal totalPeriodRevenue = eventBookingRepository.calculateTotalRevenueBetweenDates(
            janStart, febEnd, EventBookingStatus.CONFIRMED);
        assertThat(totalPeriodRevenue).isEqualByComparingTo(new BigDecimal("1775.00"));

        // Date range in 2025 should yield 0
        BigDecimal zeroRevenue = eventBookingRepository.calculateTotalRevenueBetweenDates(
            LocalDateTime.of(2025, 1, 1, 0, 0), LocalDateTime.of(2025, 1, 31, 23, 59, 59), EventBookingStatus.CONFIRMED);
        assertThat(zeroRevenue).isEqualByComparingTo(BigDecimal.ZERO);
    }
}