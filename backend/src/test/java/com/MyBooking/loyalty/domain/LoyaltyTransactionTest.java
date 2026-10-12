package com.MyBooking.loyalty.domain;

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

class LoyaltyTransactionTest {

    private Validator validator;
    private LoyaltyTransaction loyaltyTransaction;
    private LoyaltyAccount loyaltyAccount;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        // Create mock user
        User user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        user.setPassword("password123");
        user.setPhone("+1234567890");
        user.setAddress("123 Main St");
        user.setBirthDate(LocalDate.of(1990, 1, 1));
        user.setRole(Role.CLIENT);
        
        // Create mock loyalty account
        loyaltyAccount = new LoyaltyAccount();
        loyaltyAccount.setUser(user);
        loyaltyAccount.setBalance(100);
        
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
        
        // Create loyalty transaction
        loyaltyTransaction = new LoyaltyTransaction();
        loyaltyTransaction.setAccount(loyaltyAccount);
        loyaltyTransaction.setType(LoyaltyTxType.EARN);
        loyaltyTransaction.setPoints(50);
        loyaltyTransaction.setReservation(reservation);
    }

    @Test
    void testValidLoyaltyTransaction() {
        Set<ConstraintViolation<LoyaltyTransaction>> violations = validator.validate(loyaltyTransaction);
        assertTrue(violations.isEmpty(), "Valid loyalty transaction should have no violations");
    }

    @Test
    void testAccountValidation() {
        // Test null account
        loyaltyTransaction.setAccount(null);
        Set<ConstraintViolation<LoyaltyTransaction>> violations = validator.validate(loyaltyTransaction);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Loyalty account is required")));
    }

    @Test
    void testTypeValidation() {
        // Test null type
        loyaltyTransaction.setType(null);
        Set<ConstraintViolation<LoyaltyTransaction>> violations = validator.validate(loyaltyTransaction);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Transaction type is required")));
    }

    @Test
    void testPointsValidation() {
        // Test null points
        loyaltyTransaction.setPoints(null);
        Set<ConstraintViolation<LoyaltyTransaction>> violations = validator.validate(loyaltyTransaction);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Points amount is required")));

        // Test zero points
        loyaltyTransaction.setPoints(0);
        violations = validator.validate(loyaltyTransaction);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Points must be positive")));

        // Test negative points
        loyaltyTransaction.setPoints(-10);
        violations = validator.validate(loyaltyTransaction);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Points must be positive")));
    }

    @Test
    void testConstructor() {
        LoyaltyTransaction newTransaction = new LoyaltyTransaction(loyaltyAccount, LoyaltyTxType.REDEEM, 25, reservation);
        
        assertEquals(loyaltyAccount, newTransaction.getAccount());
        assertEquals(LoyaltyTxType.REDEEM, newTransaction.getType());
        assertEquals(25, newTransaction.getPoints());
        assertEquals(reservation, newTransaction.getReservation());
    }

    @Test
    void testTransactionTypeEnumValues() {
        // Test all transaction type enum values
        assertEquals(2, LoyaltyTxType.values().length);
        assertTrue(Set.of(LoyaltyTxType.values()).contains(LoyaltyTxType.EARN));
        assertTrue(Set.of(LoyaltyTxType.values()).contains(LoyaltyTxType.REDEEM));
        assertEquals("Earn points", LoyaltyTxType.EARN.getDescription());
        assertEquals("Redeem points", LoyaltyTxType.REDEEM.getDescription());
    }

    @Test
    void testValidPointsRange() {
        // Test minimum valid points (1)
        loyaltyTransaction.setPoints(1);
        Set<ConstraintViolation<LoyaltyTransaction>> violations = validator.validate(loyaltyTransaction);
        assertTrue(violations.isEmpty(), "1 point should be valid");

        // Test large points amount
        loyaltyTransaction.setPoints(10000);
        violations = validator.validate(loyaltyTransaction);
        assertTrue(violations.isEmpty(), "Large points amount should be valid");
    }

    @Test
    void testOptionalReservation() {
        // Test transaction without reservation (manual transaction)
        loyaltyTransaction.setReservation(null);
        Set<ConstraintViolation<LoyaltyTransaction>> violations = validator.validate(loyaltyTransaction);
        assertTrue(violations.isEmpty(), "Transaction without reservation should be valid");
    }
}