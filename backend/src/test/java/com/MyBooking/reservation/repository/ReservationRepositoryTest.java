package com.MyBooking.reservation.repository;

import com.MyBooking.auth.domain.User;
import com.MyBooking.auth.domain.Role;
import com.MyBooking.reservation.domain.Reservation;
import com.MyBooking.reservation.domain.ReservationStatus;
import com.MyBooking.room.domain.Room;
import com.MyBooking.room.domain.RoomType;
import com.MyBooking.room.domain.RoomStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import com.MyBooking.hotel_management.HotelManagementApplication;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("repository-test")
@ContextConfiguration(classes = HotelManagementApplication.class)
@EntityScan({"com.MyBooking.reservation.domain", "com.MyBooking.room.domain", "com.MyBooking.auth.domain"})
@EnableJpaRepositories("com.MyBooking.reservation.repository")
class ReservationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ReservationRepository reservationRepository;

    // Test entities
    private User client1;
    private User client2;
    private User client3;
    private Room room1;
    private Room room2;
    private Room room3;
    private Reservation reservation1;
    private Reservation reservation2;
    private Reservation reservation3;
    private Reservation reservation4;
    private Reservation reservation5;

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

        room3 = new Room("201", RoomType.DELUXE, 2, new BigDecimal("250.00"), "USD", RoomStatus.AVAILABLE);
        room3.setDescription("Luxurious deluxe room");
        entityManager.persistAndFlush(room3);

        // Create test reservations
        reservation1 = new Reservation(
            LocalDate.of(2024, 1, 15), LocalDate.of(2024, 1, 20), 1,
            new BigDecimal("500.00"), "USD", ReservationStatus.CONFIRMED, client1, room1
        );
        entityManager.persistAndFlush(reservation1);

        reservation2 = new Reservation(
            LocalDate.of(2024, 1, 18), LocalDate.of(2024, 1, 25), 2,
            new BigDecimal("1050.00"), "USD", ReservationStatus.CONFIRMED, client2, room2
        );
        entityManager.persistAndFlush(reservation2);

        reservation3 = new Reservation(
            LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 5), 1,
            new BigDecimal("400.00"), "USD", ReservationStatus.CANCELLED, client1, room1
        );
        entityManager.persistAndFlush(reservation3);

        reservation4 = new Reservation(
            LocalDate.of(2024, 2, 10), LocalDate.of(2024, 2, 15), 2,
            new BigDecimal("1250.00"), "USD", ReservationStatus.CONFIRMED, client3, room3
        );
        entityManager.persistAndFlush(reservation4);

        reservation5 = new Reservation(
            LocalDate.of(2024, 1, 25), LocalDate.of(2024, 1, 30), 1,
            new BigDecimal("500.00"), "EUR", ReservationStatus.CONFIRMED, client1, room1
        );
        entityManager.persistAndFlush(reservation5);
    }

    // ==================== CLIENT-BASED QUERY TESTS ====================

    @Test
    void testFindByClientId() {
        // When
        List<Reservation> client1Reservations = reservationRepository.findByClientId(client1.getId());

        // Then
        assertThat(client1Reservations).hasSize(3); // reservation1, reservation3, reservation5
        assertThat(client1Reservations).extracting(Reservation::getClient).allMatch(client -> client.getId().equals(client1.getId()));
    }

    @Test
    void testFindByClientIdWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 2);

        // When
        Page<Reservation> page = reservationRepository.findByClientId(client1.getId(), pageable);

        // Then
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    void testFindByClientIdAndStatus() {
        // When
        List<Reservation> confirmedReservations = reservationRepository.findByClientIdAndStatus(client1.getId(), ReservationStatus.CONFIRMED);

        // Then
        assertThat(confirmedReservations).hasSize(2); // reservation1, reservation5
        assertThat(confirmedReservations).extracting(Reservation::getStatus).allMatch(status -> status == ReservationStatus.CONFIRMED);
    }

    @Test
    void testCountByClientId() {
        // When & Then
        assertThat(reservationRepository.countByClientId(client1.getId())).isEqualTo(3);
        assertThat(reservationRepository.countByClientId(client2.getId())).isEqualTo(1);
        assertThat(reservationRepository.countByClientId(client3.getId())).isEqualTo(1);
    }

    @Test
    void testCountByClientIdAndStatus() {
        // When & Then
        assertThat(reservationRepository.countByClientIdAndStatus(client1.getId(), ReservationStatus.CONFIRMED)).isEqualTo(2);
        assertThat(reservationRepository.countByClientIdAndStatus(client1.getId(), ReservationStatus.CANCELLED)).isEqualTo(1);
    }

    // ==================== ROOM-BASED QUERY TESTS ====================

    @Test
    void testFindByRoomId() {
        // When
        List<Reservation> room1Reservations = reservationRepository.findByRoomId(room1.getId());

        // Then
        assertThat(room1Reservations).hasSize(3); // reservation1, reservation3, reservation5
        assertThat(room1Reservations).extracting(Reservation::getRoom).allMatch(room -> room.getId().equals(room1.getId()));
    }

    @Test
    void testFindByRoomIdWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 2);

        // When
        Page<Reservation> page = reservationRepository.findByRoomId(room1.getId(), pageable);

        // Then
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    void testFindByRoomIdAndStatus() {
        // When
        List<Reservation> confirmedReservations = reservationRepository.findByRoomIdAndStatus(room1.getId(), ReservationStatus.CONFIRMED);

        // Then
        assertThat(confirmedReservations).hasSize(2); // reservation1, reservation5
        assertThat(confirmedReservations).extracting(Reservation::getStatus).allMatch(status -> status == ReservationStatus.CONFIRMED);
    }

    @Test
    void testCountByRoomId() {
        // When & Then
        assertThat(reservationRepository.countByRoomId(room1.getId())).isEqualTo(3);
        assertThat(reservationRepository.countByRoomId(room2.getId())).isEqualTo(1);
        assertThat(reservationRepository.countByRoomId(room3.getId())).isEqualTo(1);
    }

    @Test
    void testCountByRoomIdAndStatus() {
        // When & Then
        assertThat(reservationRepository.countByRoomIdAndStatus(room1.getId(), ReservationStatus.CONFIRMED)).isEqualTo(2);
        assertThat(reservationRepository.countByRoomIdAndStatus(room1.getId(), ReservationStatus.CANCELLED)).isEqualTo(1);
    }

    // ==================== STATUS-BASED QUERY TESTS ====================

    @Test
    void testFindByStatus() {
        // When
        List<Reservation> confirmedReservations = reservationRepository.findByStatus(ReservationStatus.CONFIRMED);

        // Then
        assertThat(confirmedReservations).hasSize(4); // reservation1, reservation2, reservation4, reservation5
        assertThat(confirmedReservations).extracting(Reservation::getStatus).allMatch(status -> status == ReservationStatus.CONFIRMED);
    }

    @Test
    void testFindByStatusWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 2);

        // When
        Page<Reservation> page = reservationRepository.findByStatus(ReservationStatus.CONFIRMED, pageable);

        // Then
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    void testCountByStatus() {
        // When & Then
        assertThat(reservationRepository.countByStatus(ReservationStatus.CONFIRMED)).isEqualTo(4);
        assertThat(reservationRepository.countByStatus(ReservationStatus.CANCELLED)).isEqualTo(1);
    }

    // ==================== DATE-BASED QUERY TESTS ====================

    @Test
    void testFindByCheckInBetween() {
        // When
        List<Reservation> reservations = reservationRepository.findByCheckInBetween(
            LocalDate.of(2024, 1, 15), LocalDate.of(2024, 1, 20)
        );

        // Then
        assertThat(reservations).hasSize(2); // reservation1 (Jan 15), reservation2 (Jan 18)
        assertThat(reservations).extracting(Reservation::getCheckIn)
            .allMatch(date -> !date.isBefore(LocalDate.of(2024, 1, 15)) && !date.isAfter(LocalDate.of(2024, 1, 20)));
    }

    @Test
    void testFindByCheckOutBetween() {
        // When
        List<Reservation> reservations = reservationRepository.findByCheckOutBetween(
            LocalDate.of(2024, 1, 20), LocalDate.of(2024, 1, 25)
        );

        // Then
        assertThat(reservations).hasSize(2); // reservation1 (Jan 20), reservation2 (Jan 25)
        assertThat(reservations).extracting(Reservation::getCheckOut)
            .allMatch(date -> !date.isBefore(LocalDate.of(2024, 1, 20)) && !date.isAfter(LocalDate.of(2024, 1, 25)));
    }

    @Test
    void testFindByCheckInGreaterThanEqual() {
        // When
        List<Reservation> reservations = reservationRepository.findByCheckInGreaterThanEqual(LocalDate.of(2024, 2, 1));

        // Then
        assertThat(reservations).hasSize(2); // reservation3 (Feb 1), reservation4 (Feb 10)
        assertThat(reservations).extracting(Reservation::getCheckIn)
            .allMatch(date -> !date.isBefore(LocalDate.of(2024, 2, 1)));
    }

    @Test
    void testFindByCheckOutLessThanEqual() {
        // When
        List<Reservation> reservations = reservationRepository.findByCheckOutLessThanEqual(LocalDate.of(2024, 1, 25));

        // Then
        assertThat(reservations).hasSize(2); // reservation1 (Jan 20), reservation2 (Jan 25)
        assertThat(reservations).extracting(Reservation::getCheckOut)
            .allMatch(date -> !date.isAfter(LocalDate.of(2024, 1, 25)));
    }

    // ==================== AVAILABILITY QUERY TESTS ====================

    @Test
    void testCheckRoomAvailability_NoConflict() {
        // When - Check availability for dates that don't conflict
        List<Reservation> conflicts = reservationRepository.checkRoomAvailability(
            room1.getId(), LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 5)
        );

        // Then
        assertThat(conflicts).isEmpty();
    }

    @Test
    void testCheckRoomAvailability_WithConflict() {
        // When - Check availability for dates that conflict with existing reservation
        List<Reservation> conflicts = reservationRepository.checkRoomAvailability(
            room1.getId(), LocalDate.of(2024, 1, 16), LocalDate.of(2024, 1, 18)
        );

        // Then
        assertThat(conflicts).hasSize(1);
        assertThat(conflicts.get(0).getId()).isEqualTo(reservation1.getId());
    }

    @Test
    void testCheckRoomAvailability_PartialOverlap() {
        // When - Check availability with partial overlap
        List<Reservation> conflicts = reservationRepository.checkRoomAvailability(
            room1.getId(), LocalDate.of(2024, 1, 19), LocalDate.of(2024, 1, 22)
        );

        // Then
        assertThat(conflicts).hasSize(1);
        assertThat(conflicts.get(0).getId()).isEqualTo(reservation1.getId());
    }

    @Test
    void testCheckRoomAvailability_ExactOverlap() {
        // When - Check availability with exact same dates
        List<Reservation> conflicts = reservationRepository.checkRoomAvailability(
            room1.getId(), LocalDate.of(2024, 1, 15), LocalDate.of(2024, 1, 20)
        );

        // Then
        assertThat(conflicts).hasSize(1);
        assertThat(conflicts.get(0).getId()).isEqualTo(reservation1.getId());
    }

    @Test
    void testCheckRoomAvailability_IgnoresCancelledReservations() {
        // When - Check availability for dates that conflict with cancelled reservation
        List<Reservation> conflicts = reservationRepository.checkRoomAvailability(
            room1.getId(), LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 5)
        );

        // Then - Should be empty because reservation3 is cancelled
        assertThat(conflicts).isEmpty();
    }

    @Test
    void testCheckRoomAvailability_BackToBackArrival_Allowed() {
        // When - Check-in on the exact day existing reservation checks out (2024-01-20)
        List<Reservation> conflicts = reservationRepository.checkRoomAvailability(
            room1.getId(), LocalDate.of(2024, 1, 20), LocalDate.of(2024, 1, 25)
        );

        // Then - Should NOT conflict (half-open interval [2024-01-15, 2024-01-20) does not overlap [2024-01-20, 2024-01-25))
        assertThat(conflicts).isEmpty();
    }

    @Test
    void testCheckRoomAvailability_BackToBackDeparture_Allowed() {
        // When - Check-out on the exact day existing reservation checks in (2024-01-15)
        List<Reservation> conflicts = reservationRepository.checkRoomAvailability(
            room1.getId(), LocalDate.of(2024, 1, 10), LocalDate.of(2024, 1, 15)
        );

        // Then - Should NOT conflict
        assertThat(conflicts).isEmpty();
    }

    @Test
    void testCheckRoomAvailability_EnclosingInterval_Conflict() {
        // When - Check requested range that completely wraps the existing reservation
        List<Reservation> conflicts = reservationRepository.checkRoomAvailability(
            room1.getId(), LocalDate.of(2024, 1, 10), LocalDate.of(2024, 1, 25)
        );

        // Then - Must detect conflict with reservation1
        assertThat(conflicts).hasSize(1);
        assertThat(conflicts.get(0).getId()).isEqualTo(reservation1.getId());
    }

    @Test
    void testCheckRoomAvailability_WithExcludeReservationId_NoSelfConflict() {
        // When - Updating reservation1 with identical or overlapping dates, excluding reservation1's ID
        List<Reservation> conflicts = reservationRepository.checkRoomAvailability(
            room1.getId(), LocalDate.of(2024, 1, 15), LocalDate.of(2024, 1, 20), reservation1.getId()
        );

        // Then - Should be empty because reservation1 is excluded from conflict check
        assertThat(conflicts).isEmpty();
    }

    @Test
    void testCheckRoomAvailability_WithExcludeDifferentReservationId_DetectsConflict() {
        // When - Checking same dates excluding an unrelated reservation ID
        List<Reservation> conflicts = reservationRepository.checkRoomAvailability(
            room1.getId(), LocalDate.of(2024, 1, 15), LocalDate.of(2024, 1, 20), 9999L
        );

        // Then - Should detect reservation1 as conflict
        assertThat(conflicts).hasSize(1);
        assertThat(conflicts.get(0).getId()).isEqualTo(reservation1.getId());
    }

    @Test
    void testFindOverlappingReservations() {
        // When
        List<Reservation> overlapping = reservationRepository.findOverlappingReservations(
            LocalDate.of(2024, 1, 16), LocalDate.of(2024, 1, 18)
        );

        // Then
        assertThat(overlapping).hasSize(1);
        assertThat(overlapping.get(0).getId()).isEqualTo(reservation1.getId());
    }

    // ==================== PRICE-BASED QUERY TESTS ====================

    @Test
    void testFindByTotalPriceBetween() {
        // When
        List<Reservation> reservations = reservationRepository.findByTotalPriceBetween(
            new BigDecimal("400.00"), new BigDecimal("600.00")
        );

        // Then
        assertThat(reservations).hasSize(3); // reservation1 (500), reservation3 (400), reservation5 (500)
        assertThat(reservations).extracting(Reservation::getTotalPrice)
            .allMatch(price -> price.compareTo(new BigDecimal("400.00")) >= 0 && 
                              price.compareTo(new BigDecimal("600.00")) <= 0);
    }

    @Test
    void testFindByTotalPriceBetweenWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 2);

        // When
        Page<Reservation> page = reservationRepository.findByTotalPriceBetween(
            new BigDecimal("400.00"), new BigDecimal("600.00"), pageable
        );

        // Then
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    void testFindByCurrency() {
        // When
        List<Reservation> usdReservations = reservationRepository.findByCurrency("USD");

        // Then
        assertThat(usdReservations).hasSize(4); // reservation1, reservation2, reservation3, reservation4
        assertThat(usdReservations).extracting(Reservation::getCurrency).allMatch(currency -> currency.equals("USD"));
    }

    @Test
    void testFindByCurrencyWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 2);

        // When
        Page<Reservation> page = reservationRepository.findByCurrency("USD", pageable);

        // Then
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    void testFindByTotalPriceGreaterThanEqual() {
        // When
        List<Reservation> reservations = reservationRepository.findByTotalPriceGreaterThanEqual(new BigDecimal("1000.00"));

        // Then
        assertThat(reservations).hasSize(2); // reservation2 (1050), reservation4 (1250)
        assertThat(reservations).extracting(Reservation::getTotalPrice)
            .allMatch(price -> price.compareTo(new BigDecimal("1000.00")) >= 0);
    }

    @Test
    void testFindByTotalPriceLessThanEqual() {
        // When
        List<Reservation> reservations = reservationRepository.findByTotalPriceLessThanEqual(new BigDecimal("500.00"));

        // Then
        assertThat(reservations).hasSize(3); // reservation1 (500), reservation3 (400), reservation5 (500)
        assertThat(reservations).extracting(Reservation::getTotalPrice)
            .allMatch(price -> price.compareTo(new BigDecimal("500.00")) <= 0);
    }

    // ==================== GUEST COUNT QUERY TESTS ====================

    @Test
    void testFindByNumberOfGuests() {
        // When
        List<Reservation> singleGuestReservations = reservationRepository.findByNumberOfGuests(1);

        // Then
        assertThat(singleGuestReservations).hasSize(3); // reservation1, reservation3, reservation5
        assertThat(singleGuestReservations).extracting(Reservation::getNumberOfGuests).allMatch(guests -> guests == 1);
    }

    @Test
    void testFindByNumberOfGuestsBetween() {
        // When
        List<Reservation> reservations = reservationRepository.findByNumberOfGuestsBetween(1, 2);

        // Then
        assertThat(reservations).hasSize(5); // All reservations
        assertThat(reservations).extracting(Reservation::getNumberOfGuests)
            .allMatch(guests -> guests >= 1 && guests <= 2);
    }

    @Test
    void testFindByNumberOfGuestsGreaterThanEqual() {
        // When
        List<Reservation> reservations = reservationRepository.findByNumberOfGuestsGreaterThanEqual(2);

        // Then
        assertThat(reservations).hasSize(2); // reservation2, reservation4
        assertThat(reservations).extracting(Reservation::getNumberOfGuests).allMatch(guests -> guests >= 2);
    }

    @Test
    void testFindByNumberOfGuestsLessThanEqual() {
        // When
        List<Reservation> reservations = reservationRepository.findByNumberOfGuestsLessThanEqual(1);

        // Then
        assertThat(reservations).hasSize(3); // reservation1, reservation3, reservation5
        assertThat(reservations).extracting(Reservation::getNumberOfGuests).allMatch(guests -> guests <= 1);
    }

    // ==================== COMBINED CRITERIA QUERY TESTS ====================

    @Test
    void testFindByCriteria_AllFilters() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Reservation> page = reservationRepository.findByCriteria(
            client1.getId(), // clientId
            room1.getId(),   // roomId
            ReservationStatus.CONFIRMED, // status
            new BigDecimal("400.00"), // minPrice
            new BigDecimal("600.00"), // maxPrice
            "USD", // currency
            1, // minGuests
            1, // maxGuests
            LocalDate.of(2024, 1, 1), // checkInFrom
            LocalDate.of(2024, 12, 31), // checkInTo
            pageable
        );

        // Then
        assertThat(page.getContent()).hasSize(1); // Only reservation1 matches all criteria
        assertThat(page.getContent().get(0).getId()).isEqualTo(reservation1.getId());
    }

    @Test
    void testFindByCriteria_PartialFilters() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When - Only filter by status and currency
        Page<Reservation> page = reservationRepository.findByCriteria(
            null, // clientId
            null, // roomId
            ReservationStatus.CONFIRMED, // status
            null, // minPrice
            null, // maxPrice
            "USD", // currency
            null, // minGuests
            null, // maxGuests
            null, // checkInFrom
            null, // checkInTo
            pageable
        );

        // Then
        assertThat(page.getContent()).hasSize(3); // reservation1, reservation2, reservation4 (all confirmed USD)
        assertThat(page.getContent()).extracting(Reservation::getStatus).allMatch(status -> status == ReservationStatus.CONFIRMED);
        assertThat(page.getContent()).extracting(Reservation::getCurrency).allMatch(currency -> currency.equals("USD"));
    }

    // ==================== EXISTENCE CHECK TESTS ====================

    @Test
    void testExistsByClientIdAndRoomIdAndStatus() {
        // When & Then
        assertThat(reservationRepository.existsByClientIdAndRoomIdAndStatus(
            client1.getId(), room1.getId(), ReservationStatus.CONFIRMED)).isTrue();
        assertThat(reservationRepository.existsByClientIdAndRoomIdAndStatus(
            client1.getId(), room1.getId(), ReservationStatus.CANCELLED)).isTrue();
        assertThat(reservationRepository.existsByClientIdAndRoomIdAndStatus(
            client2.getId(), room1.getId(), ReservationStatus.CONFIRMED)).isFalse();
    }

    @Test
    void testExistsByClientIdAndRoomId() {
        // When & Then
        assertThat(reservationRepository.existsByClientIdAndRoomId(client1.getId(), room1.getId())).isTrue();
        assertThat(reservationRepository.existsByClientIdAndRoomId(client2.getId(), room1.getId())).isFalse();
    }

    @Test
    void testExistsByRoomIdAndStatus() {
        // When & Then
        assertThat(reservationRepository.existsByRoomIdAndStatus(room1.getId(), ReservationStatus.CONFIRMED)).isTrue();
        assertThat(reservationRepository.existsByRoomIdAndStatus(room1.getId(), ReservationStatus.CANCELLED)).isTrue();
        assertThat(reservationRepository.existsByRoomIdAndStatus(room2.getId(), ReservationStatus.CANCELLED)).isFalse();
    }

    // ==================== TIME-BASED QUERY TESTS ====================

    @Test
    void testFindByCreatedAtBetween() {
        // Given - Get current time for range
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusHours(1);
        LocalDateTime end = now.plusHours(1);

        // When
        List<Reservation> reservations = reservationRepository.findByCreatedAtBetween(start, end);

        // Then - All test reservations should be in this range (created in setUp)
        assertThat(reservations).hasSize(5);
    }

    @Test
    void testFindByUpdatedAtBetween() {
        // Given - Get current time for range
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusHours(1);
        LocalDateTime end = now.plusHours(1);

        // When
        List<Reservation> reservations = reservationRepository.findByUpdatedAtBetween(start, end);

        // Then - All test reservations should be in this range (updated in setUp)
        assertThat(reservations).hasSize(5);
    }

    @Test
    void testFindByCreatedAtAfter() {
        // Given - Get time before setUp
        LocalDateTime beforeSetUp = LocalDateTime.now().minusHours(1);

        // When
        List<Reservation> reservations = reservationRepository.findByCreatedAtAfter(beforeSetUp);

        // Then - All test reservations should be after this time
        assertThat(reservations).hasSize(5);
    }

    @Test
    void testFindByUpdatedAtAfter() {
        // Given - Get time before setUp
        LocalDateTime beforeSetUp = LocalDateTime.now().minusHours(1);

        // When
        List<Reservation> reservations = reservationRepository.findByUpdatedAtAfter(beforeSetUp);

        // Then - All test reservations should be after this time
        assertThat(reservations).hasSize(5);
    }

    // ==================== STATISTICS QUERY TESTS ====================

    @Test
    void testGetTotalRevenue() {
        // When
        BigDecimal totalRevenue = reservationRepository.getTotalRevenue();

        // Then - Only confirmed reservations count: 500 + 1050 + 1250 + 500 = 3300
        assertThat(totalRevenue).isEqualByComparingTo(new BigDecimal("3300.00"));
    }

    @Test
    void testGetTotalRevenueByCurrency() {
        // When
        BigDecimal usdRevenue = reservationRepository.getTotalRevenueByCurrency("USD");
        BigDecimal eurRevenue = reservationRepository.getTotalRevenueByCurrency("EUR");

        // Then
        assertThat(usdRevenue).isEqualByComparingTo(new BigDecimal("2800.00")); // 500 + 1050 + 1250
        assertThat(eurRevenue).isEqualByComparingTo(new BigDecimal("500.00")); // 500
    }

    @Test
    void testGetAverageReservationPrice() {
        // When
        BigDecimal averagePrice = reservationRepository.getAverageReservationPrice();

        // Then - Average of confirmed reservations: 3300 / 4 = 825
        assertThat(averagePrice).isEqualByComparingTo(new BigDecimal("825.00"));
    }

    @Test
    void testGetAverageReservationPriceByCurrency() {
        // When
        BigDecimal usdAverage = reservationRepository.getAverageReservationPriceByCurrency("USD");
        BigDecimal eurAverage = reservationRepository.getAverageReservationPriceByCurrency("EUR");

        // Then
        assertThat(usdAverage).isEqualByComparingTo(new BigDecimal("933.333333333333")); // 2800 / 3 ≈ 933.33
        assertThat(eurAverage).isEqualByComparingTo(new BigDecimal("500.00")); // 500 / 1 = 500
    }

    @Test
    void testFindOverlappingReservationsInDateRange() {
        // Query January 2024: Should find confirmed reservations 1, 2, 5 (excluding cancelled 3 and February 4)
        List<Reservation> janReservations = reservationRepository.findOverlappingReservationsInDateRange(
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), ReservationStatus.CONFIRMED
        );
        assertThat(janReservations).hasSize(3);
        assertThat(janReservations).extracting(Reservation::getId)
            .containsExactlyInAnyOrder(reservation1.getId(), reservation2.getId(), reservation5.getId());

        // Query February 2024: Should find only confirmed reservation 4
        List<Reservation> febReservations = reservationRepository.findOverlappingReservationsInDateRange(
            LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29), ReservationStatus.CONFIRMED
        );
        assertThat(febReservations).hasSize(1);
        assertThat(febReservations.get(0).getId()).isEqualTo(reservation4.getId());
    }
}