package com.MyBooking.auth.domain;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NotificationPreferenceTest {

    private Validator validator;
    private NotificationPreference notificationPreference;
    private User user;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        // Create a mock user
        user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        user.setPassword("password123");
        user.setPhone("+1234567890");
        user.setAddress("123 Main St");
        user.setBirthDate(LocalDate.of(1990, 1, 1));
        user.setRole(Role.CLIENT);
        
        notificationPreference = new NotificationPreference();
        notificationPreference.setUser(user);
        notificationPreference.setNotificationType("BOOKING_CONFIRMATION");
        notificationPreference.setEmailEnabled(true);
        notificationPreference.setSmsEnabled(false);
    }

    @Test
    void testValidNotificationPreference() {
        Set<ConstraintViolation<NotificationPreference>> violations = validator.validate(notificationPreference);
        assertTrue(violations.isEmpty(), "Valid notification preference should have no violations");
    }

    @Test
    void testUserValidation() {
        // Test null user
        notificationPreference.setUser(null);
        Set<ConstraintViolation<NotificationPreference>> violations = validator.validate(notificationPreference);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("User is required")));
    }

    @Test
    void testNotificationTypeValidation() {
        // Test null notification type
        notificationPreference.setNotificationType(null);
        Set<ConstraintViolation<NotificationPreference>> violations = validator.validate(notificationPreference);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Notification type is required")));
    }

    @Test
    void testEmailEnabledValidation() {
        // Test null email enabled
        notificationPreference.setEmailEnabled(null);
        Set<ConstraintViolation<NotificationPreference>> violations = validator.validate(notificationPreference);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Email enabled is required")));
    }

    @Test
    void testSmsEnabledValidation() {
        // Test null SMS enabled
        notificationPreference.setSmsEnabled(null);
        Set<ConstraintViolation<NotificationPreference>> violations = validator.validate(notificationPreference);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("SMS enabled is required")));
    }

    @Test
    void testConstructor() {
        NotificationPreference newPreference = new NotificationPreference(user, "BOOKING_CONFIRMATION", false, true, true);
        
        assertEquals(user, newPreference.getUser());
        assertEquals("BOOKING_CONFIRMATION", newPreference.getNotificationType());
        assertEquals(false, newPreference.getEmailEnabled());
        assertEquals(true, newPreference.getSmsEnabled());
        assertEquals(true, newPreference.getPushEnabled());
    }

    @Test
    void testUtilityMethods() {
        // Test isEmailEnabled
        notificationPreference.setEmailEnabled(true);
        assertTrue(notificationPreference.isEmailEnabled());
        
        notificationPreference.setEmailEnabled(false);
        assertFalse(notificationPreference.isEmailEnabled());
        
        notificationPreference.setEmailEnabled(null);
        assertFalse(notificationPreference.isEmailEnabled());
        
        // Test isSmsEnabled
        notificationPreference.setSmsEnabled(true);
        assertTrue(notificationPreference.isSmsEnabled());
        
        notificationPreference.setSmsEnabled(false);
        assertFalse(notificationPreference.isSmsEnabled());
        
        notificationPreference.setSmsEnabled(null);
        assertFalse(notificationPreference.isSmsEnabled());
    }

    @Test
    void testDefaultValues() {
        NotificationPreference defaultPreference = new NotificationPreference();
        defaultPreference.setUser(user);
        
        assertEquals(true, defaultPreference.getEmailEnabled());
        assertEquals(false, defaultPreference.getSmsEnabled());
    }
}