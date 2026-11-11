package com.MyBooking.loyalty.domain;

import com.MyBooking.auth.domain.Role;
import com.MyBooking.auth.domain.User;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LoyaltyAccountTest {

    private Validator validator;
    private LoyaltyAccount loyaltyAccount;
    private User user;

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
        
        loyaltyAccount = new LoyaltyAccount();
        loyaltyAccount.setUser(user);
        loyaltyAccount.setBalance(100);
    }

    @Test
    void testValidLoyaltyAccount() {
        Set<ConstraintViolation<LoyaltyAccount>> violations = validator.validate(loyaltyAccount);
        assertTrue(violations.isEmpty(), "Valid loyalty account should have no violations");
    }

    @Test
    void testUserValidation() {
        // Test null user
        loyaltyAccount.setUser(null);
        Set<ConstraintViolation<LoyaltyAccount>> violations = validator.validate(loyaltyAccount);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("User is required")));
    }

    @Test
    void testBalanceValidation() {
        // Test null balance
        loyaltyAccount.setBalance(null);
        Set<ConstraintViolation<LoyaltyAccount>> violations = validator.validate(loyaltyAccount);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Balance is required")));

        // Test negative balance
        loyaltyAccount.setBalance(-1);
        violations = validator.validate(loyaltyAccount);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Balance cannot be negative")));
    }

    @Test
    void testValidBalanceRange() {
        // Test zero balance
        loyaltyAccount.setBalance(0);
        Set<ConstraintViolation<LoyaltyAccount>> violations = validator.validate(loyaltyAccount);
        assertTrue(violations.isEmpty(), "Zero balance should be valid");

        // Test positive balance
        loyaltyAccount.setBalance(1000);
        violations = validator.validate(loyaltyAccount);
        assertTrue(violations.isEmpty(), "Positive balance should be valid");
    }

    @Test
    void testConstructor() {
        LoyaltyAccount newAccount = new LoyaltyAccount(user, 250);
        
        assertEquals(user, newAccount.getUser());
        assertEquals(250, newAccount.getBalance());
    }

    @Test
    void testDefaultBalance() {
        LoyaltyAccount defaultAccount = new LoyaltyAccount();
        defaultAccount.setUser(user);
        
        assertEquals(0, defaultAccount.getBalance());
    }

    @Test
    void testBalanceBoundaries() {
        // Test minimum valid balance (0)
        loyaltyAccount.setBalance(0);
        Set<ConstraintViolation<LoyaltyAccount>> violations = validator.validate(loyaltyAccount);
        assertTrue(violations.isEmpty(), "Balance of 0 should be valid");

        // Test large balance
        loyaltyAccount.setBalance(999999);
        violations = validator.validate(loyaltyAccount);
        assertTrue(violations.isEmpty(), "Large balance should be valid");
    }

    @Test
    void testVersionGetterSetter() {
        loyaltyAccount.setVersion(3L);
        assertEquals(3L, loyaltyAccount.getVersion());
    }
}