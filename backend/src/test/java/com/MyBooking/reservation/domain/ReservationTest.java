package com.MyBooking.reservation.domain;

import com.MyBooking.auth.domain.Role;
import com.MyBooking.auth.domain.User;
import com.MyBooking.room.domain.Room;
import com.MyBooking.room.domain.RoomStatus;
import com.MyBooking.room.domain.RoomType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {

    private Validator validator;
    private Reservation reservation;
    private User client;
    private Room room;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        // Create test client
        client = new User();
        client.setId(1L);
        client.setFirstName("John");
        client.setLastName("Doe");
        client.setEmail("john.doe@example.com");
        client.setRole(Role.CLIENT);
        
        // Create test room
        room = new Room();
        room.setId(1L);
        room.setNumber("101");
        room.setRoomType(RoomType.SINGLE);
        room.setCapacity(2);
        room.setPrice(new BigDecimal("150.00"));
        room.setCurrency("EUR");
        room.setStatus(RoomStatus.AVAILABLE);
        
        // Create test reservation
        reservation = new Reservation();
        reservation.setCheckIn(LocalDate.now().plusDays(1));
        reservation.setCheckOut(LocalDate.now().plusDays(3));
        reservation.setNumberOfGuests(2);
        reservation.setTotalPrice(new BigDecimal("300.00"));
        reservation.setCurrency("EUR");
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setClient(client);
        reservation.setRoom(room);
    }

    @Test
    void testValidReservation() {
        Set<ConstraintViolation<Reservation>> violations = validator.validate(reservation);
        assertTrue(violations.isEmpty(), "Valid reservation should have no violations");
    }

    @Test
    void testCheckInValidation() {
        // Test null check-in
        reservation.setCheckIn(null);
        Set<ConstraintViolation<Reservation>> violations = validator.validate(reservation);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Check-in date is required")));
    }

    @Test
    void testCheckOutValidation() {
        // Test null check-out
        reservation.setCheckOut(null);
        Set<ConstraintViolation<Reservation>> violations = validator.validate(reservation);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Check-out date is required")));
    }

    @Test
    void testNumberOfGuestsValidation() {
        // Test null number of guests
        reservation.setNumberOfGuests(null);
        Set<ConstraintViolation<Reservation>> violations = validator.validate(reservation);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Number of guests is required")));

        // Test too few guests
        reservation.setNumberOfGuests(0);
        violations = validator.validate(reservation);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Number of guests must be at least 1")));

        // Test too many guests
        reservation.setNumberOfGuests(11);
        violations = validator.validate(reservation);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("must not exceed 10")));
    }

    @Test
    void testTotalPriceValidation() {
        // Test null total price
        reservation.setTotalPrice(null);
        Set<ConstraintViolation<Reservation>> violations = validator.validate(reservation);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Total price is required")));

        // Test zero total price (valid when 100% covered by loyalty points)
        reservation.setTotalPrice(BigDecimal.ZERO);
        violations = validator.validate(reservation);
        assertTrue(violations.isEmpty());

        // Test negative total price
        reservation.setTotalPrice(new BigDecimal("-0.01"));
        violations = validator.validate(reservation);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Total price must be greater than or equal to 0")));
    }

    @Test
    void testCurrencyValidation() {
        // Test null currency
        reservation.setCurrency(null);
        Set<ConstraintViolation<Reservation>> violations = validator.validate(reservation);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Currency is required")));

        // Test invalid currency length
        reservation.setCurrency("EU");
        violations = validator.validate(reservation);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Currency must be exactly 3 characters")));
    }

    @Test
    void testStatusValidation() {
        // Test null status
        reservation.setStatus(null);
        Set<ConstraintViolation<Reservation>> violations = validator.validate(reservation);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Status is required")));
    }

    @Test
    void testClientValidation() {
        // Test null client
        reservation.setClient(null);
        Set<ConstraintViolation<Reservation>> violations = validator.validate(reservation);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Client is required")));
    }

    @Test
    void testRoomValidation() {
        // Test null room
        reservation.setRoom(null);
        Set<ConstraintViolation<Reservation>> violations = validator.validate(reservation);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Room is required")));
    }

    @Test
    void testUtilityMethods() {
        // Test status checking methods
        reservation.setStatus(ReservationStatus.CONFIRMED);
        assertTrue(reservation.isConfirmed());
        assertFalse(reservation.isCancelled());

        reservation.setStatus(ReservationStatus.CANCELLED);
        assertFalse(reservation.isConfirmed());
        assertTrue(reservation.isCancelled());
    }

    @Test
    void testGetFormattedPrice() {
        reservation.setCurrency("EUR");
        reservation.setTotalPrice(new BigDecimal("300.00"));
        assertEquals("EUR 300.00", reservation.getFormattedPrice());

        reservation.setCurrency("USD");
        reservation.setTotalPrice(new BigDecimal("250.50"));
        assertEquals("USD 250.50", reservation.getFormattedPrice());
    }

    @Test
    void testConstructor() {
        Reservation newReservation = new Reservation(
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(3),
            2,
            new BigDecimal("300.00"),
            "EUR",
            ReservationStatus.CONFIRMED,
            client,
            room
        );
        
        assertEquals(LocalDate.now().plusDays(1), newReservation.getCheckIn());
        assertEquals(LocalDate.now().plusDays(3), newReservation.getCheckOut());
        assertEquals(2, newReservation.getNumberOfGuests());
        assertEquals(new BigDecimal("300.00"), newReservation.getTotalPrice());
        assertEquals("EUR", newReservation.getCurrency());
        assertEquals(ReservationStatus.CONFIRMED, newReservation.getStatus());
        assertEquals(client, newReservation.getClient());
        assertEquals(room, newReservation.getRoom());
    }

    @Test
    void testReservationStatusEnumValues() {
        // Test all reservation status enum values
        assertEquals(2, ReservationStatus.values().length);
        assertTrue(Set.of("CONFIRMED", "CANCELLED")
                .containsAll(Set.of(ReservationStatus.values()).stream()
                        .map(Enum::name)
                        .collect(java.util.stream.Collectors.toSet())));
    }
}