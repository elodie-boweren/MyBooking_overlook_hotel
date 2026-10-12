package com.MyBooking.feedback.domain;

import com.MyBooking.auth.domain.Role;
import com.MyBooking.auth.domain.User;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FeedbackReplyTest {

    private Validator validator;
    private FeedbackReply feedbackReply;
    private Feedback feedback;
    private User adminUser;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        // Create mock admin user
        adminUser = new User();
        adminUser.setFirstName("Admin");
        adminUser.setLastName("Manager");
        adminUser.setEmail("admin@hotel.com");
        adminUser.setPassword("admin123");
        adminUser.setPhone("+1234567890");
        adminUser.setAddress("Hotel Office");
        adminUser.setBirthDate(LocalDate.of(1985, 1, 1));
        adminUser.setRole(Role.ADMIN);
        
        // Create mock client user
        User clientUser = new User();
        clientUser.setFirstName("John");
        clientUser.setLastName("Doe");
        clientUser.setEmail("john.doe@example.com");
        clientUser.setPassword("password123");
        clientUser.setPhone("+1234567890");
        clientUser.setAddress("123 Main St");
        clientUser.setBirthDate(LocalDate.of(1990, 1, 1));
        clientUser.setRole(Role.CLIENT);
        
        // Create mock room
        Room room = new Room();
        room.setNumber("101");
        room.setRoomType(RoomType.DELUXE);
        room.setCapacity(2);
        room.setPrice(new BigDecimal("150.00"));
        room.setCurrency("EUR");
        room.setStatus(RoomStatus.AVAILABLE);
        
        // Create mock reservation
        Reservation reservation = new Reservation();
        reservation.setClient(clientUser);
        reservation.setRoom(room);
        reservation.setCheckIn(LocalDate.now().minusDays(1));
        reservation.setCheckOut(LocalDate.now());
        reservation.setNumberOfGuests(2);
        reservation.setTotalPrice(new BigDecimal("300.00"));
        reservation.setCurrency("EUR");
        reservation.setStatus(ReservationStatus.CONFIRMED);
        
        // Create mock feedback
        feedback = new Feedback();
        feedback.setReservation(reservation);
        feedback.setUser(clientUser);
        feedback.setRating(3);
        feedback.setComment("Room was okay but service could be better.");
        
        // Create feedback reply
        feedbackReply = new FeedbackReply();
        feedbackReply.setFeedback(feedback);
        feedbackReply.setAdminUser(adminUser);
        feedbackReply.setMessage("Thank you for your feedback. We'll work on improving our service.");
    }

    @Test
    void testValidFeedbackReply() {
        Set<ConstraintViolation<FeedbackReply>> violations = validator.validate(feedbackReply);
        assertTrue(violations.isEmpty(), "Valid feedback reply should have no violations");
    }

    @Test
    void testFeedbackValidation() {
        // Test null feedback
        feedbackReply.setFeedback(null);
        Set<ConstraintViolation<FeedbackReply>> violations = validator.validate(feedbackReply);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Feedback is required")));
    }

    @Test
    void testAdminUserValidation() {
        // Test null admin user
        feedbackReply.setAdminUser(null);
        Set<ConstraintViolation<FeedbackReply>> violations = validator.validate(feedbackReply);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Admin user is required")));
    }

    @Test
    void testMessageValidation() {
        // Test null message
        feedbackReply.setMessage(null);
        Set<ConstraintViolation<FeedbackReply>> violations = validator.validate(feedbackReply);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Message is required")));

        // Test blank message
        feedbackReply.setMessage("");
        violations = validator.validate(feedbackReply);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Message is required")));

        // Test message too long
        feedbackReply.setMessage("A".repeat(2001));
        violations = validator.validate(feedbackReply);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Message cannot exceed 2000 characters")));
    }

    @Test
    void testConstructor() {
        FeedbackReply newReply = new FeedbackReply(feedback, adminUser, "We appreciate your feedback!");
        
        assertEquals(feedback, newReply.getFeedback());
        assertEquals(adminUser, newReply.getAdminUser());
        assertEquals("We appreciate your feedback!", newReply.getMessage());
    }

    @Test
    void testMessageLengthBoundaries() {
        // Test minimum valid message (1 character)
        feedbackReply.setMessage("A");
        Set<ConstraintViolation<FeedbackReply>> violations = validator.validate(feedbackReply);
        assertTrue(violations.isEmpty(), "Single character message should be valid");

        // Test maximum valid message (1000 characters)
        feedbackReply.setMessage("A".repeat(1000));
        violations = validator.validate(feedbackReply);
        assertTrue(violations.isEmpty(), "1000 character message should be valid");
    }
}