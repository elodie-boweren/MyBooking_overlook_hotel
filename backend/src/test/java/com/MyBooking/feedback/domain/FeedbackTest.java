package com.MyBooking.feedback.domain;

import com.MyBooking.auth.domain.Role;
import com.MyBooking.auth.domain.User;
import com.MyBooking.installation.domain.Installation;
import com.MyBooking.installation.domain.InstallationType;
import com.MyBooking.reservation.domain.Reservation;
import com.MyBooking.reservation.domain.ReservationStatus;
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
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FeedbackTest {

    private Validator validator;
    private Feedback feedback;
    private User user;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        // Create mock user
        user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        user.setPassword("password123");
        user.setPhone("+1234567890");
        user.setAddress("123 Main St");
        user.setBirthDate(LocalDate.of(1990, 1, 1));
        user.setRole(Role.CLIENT);
        
        // Create mock room
        Room room = new Room();
        room.setNumber("101");
        room.setRoomType(RoomType.DELUXE);
        room.setCapacity(2);
        room.setPrice(new BigDecimal("150.00"));
        room.setCurrency("EUR");
        room.setStatus(RoomStatus.AVAILABLE);
        
        // Create mock reservation
        reservation = new Reservation();
        reservation.setClient(user);
        reservation.setRoom(room);
        reservation.setCheckIn(LocalDate.now().minusDays(1));
        reservation.setCheckOut(LocalDate.now());
        reservation.setNumberOfGuests(2);
        reservation.setTotalPrice(new BigDecimal("300.00"));
        reservation.setCurrency("EUR");
        reservation.setStatus(ReservationStatus.CONFIRMED);
        
        // Create feedback
        feedback = new Feedback();
        feedback.setReservation(reservation);
        feedback.setUser(user);
        feedback.setRating(5);
        feedback.setComment("Excellent stay! Great service and clean room.");
    }

    @Test
    void testValidFeedback() {
        Set<ConstraintViolation<Feedback>> violations = validator.validate(feedback);
        assertTrue(violations.isEmpty(), "Valid feedback should have no violations");
    }

    @Test
    void testReservationValidation() {
        // Test null reservation
        feedback.setReservation(null);
        Set<ConstraintViolation<Feedback>> violations = validator.validate(feedback);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Reservation is required")));
    }

    @Test
    void testUserValidation() {
        // Test null user
        feedback.setUser(null);
        Set<ConstraintViolation<Feedback>> violations = validator.validate(feedback);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("User is required")));
    }

    @Test
    void testRatingValidation() {
        // Test null rating
        feedback.setRating(null);
        Set<ConstraintViolation<Feedback>> violations = validator.validate(feedback);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Rating is required")));

        // Test rating too low
        feedback.setRating(0);
        violations = validator.validate(feedback);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Rating must be at least 1")));

        // Test rating too high
        feedback.setRating(6);
        violations = validator.validate(feedback);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Rating must be at most 5")));
    }

    @Test
    void testCommentValidation() {
        // Test comment too long
        feedback.setComment("A".repeat(1001));
        Set<ConstraintViolation<Feedback>> violations = validator.validate(feedback);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Comment cannot exceed 1000 characters")));
    }

    @Test
    void testConstructor() {
        Feedback newFeedback = new Feedback(reservation, user, 4, "Good experience overall");
        
        assertEquals(reservation, newFeedback.getReservation());
        assertEquals(user, newFeedback.getUser());
        assertEquals(4, newFeedback.getRating());
        assertEquals("Good experience overall", newFeedback.getComment());
    }

    @Test
    void testValidRatingRange() {
        // Test all valid ratings
        for (int rating = 1; rating <= 5; rating++) {
            feedback.setRating(rating);
            Set<ConstraintViolation<Feedback>> violations = validator.validate(feedback);
            assertTrue(violations.isEmpty(), "Rating " + rating + " should be valid");
        }
    }
}