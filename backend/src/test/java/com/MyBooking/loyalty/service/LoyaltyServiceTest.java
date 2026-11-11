package com.MyBooking.loyalty.service;

import com.MyBooking.auth.domain.User;
import com.MyBooking.auth.domain.Role;
import com.MyBooking.auth.repository.UserRepository;
import com.MyBooking.common.exception.BusinessRuleException;
import com.MyBooking.common.exception.NotFoundException;
import com.MyBooking.loyalty.domain.LoyaltyAccount;
import com.MyBooking.loyalty.domain.LoyaltyTransaction;
import com.MyBooking.loyalty.domain.LoyaltyTxType;
import com.MyBooking.loyalty.repository.LoyaltyAccountRepository;
import com.MyBooking.loyalty.repository.LoyaltyTransactionRepository;
import com.MyBooking.reservation.domain.Reservation;
import com.MyBooking.reservation.domain.ReservationStatus;
import com.MyBooking.reservation.repository.ReservationRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoyaltyServiceTest {

    @Mock
    private LoyaltyAccountRepository loyaltyAccountRepository;

    @Mock
    private LoyaltyTransactionRepository loyaltyTransactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private LoyaltyService loyaltyService;

    private User testUser;
    private LoyaltyAccount testAccount;
    private LoyaltyTransaction testTransaction;
    private Reservation testReservation;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setRole(Role.CLIENT);

        // Setup test loyalty account
        testAccount = new LoyaltyAccount();
        testAccount.setId(1L);
        testAccount.setUser(testUser);
        testAccount.setBalance(1000);

        // Setup test transaction
        testTransaction = new LoyaltyTransaction();
        testTransaction.setId(1L);
        testTransaction.setAccount(testAccount);
        testTransaction.setType(LoyaltyTxType.EARN);
        testTransaction.setPoints(100);
        testTransaction.setCreatedAt(LocalDateTime.now());

        // Setup test reservation
        testReservation = new Reservation();
        testReservation.setId(1L);
        testReservation.setClient(testUser);
        testReservation.setTotalPrice(new BigDecimal("100.00"));
        testReservation.setStatus(ReservationStatus.CONFIRMED);
    }

    // ========== CORE ACCOUNT MANAGEMENT TESTS ==========

    @Test
    void createLoyaltyAccount_WithValidUser_ShouldCreateAccount() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(loyaltyAccountRepository.existsByUserId(1L)).thenReturn(false);
        when(loyaltyAccountRepository.save(any(LoyaltyAccount.class))).thenReturn(testAccount);

        // When
        LoyaltyAccount result = loyaltyService.createLoyaltyAccount(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(testUser);
        assertThat(result.getBalance()).isEqualTo(1000); // The mock returns testAccount with balance 1000

        verify(userRepository).findById(1L);
        verify(loyaltyAccountRepository).existsByUserId(1L);
        verify(loyaltyAccountRepository).save(any(LoyaltyAccount.class));
    }

    @Test
    void createLoyaltyAccount_WithNonExistentUser_ShouldThrowNotFoundException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> loyaltyService.createLoyaltyAccount(999L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("User not found with ID: 999");

        verify(userRepository).findById(999L);
        verify(loyaltyAccountRepository, never()).save(any());
    }

    @Test
    void createLoyaltyAccount_WithExistingAccount_ShouldThrowBusinessRuleException() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(loyaltyAccountRepository.existsByUserId(1L)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> loyaltyService.createLoyaltyAccount(1L))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Loyalty account already exists for user ID: 1");

        verify(userRepository).findById(1L);
        verify(loyaltyAccountRepository).existsByUserId(1L);
        verify(loyaltyAccountRepository, never()).save(any());
    }

    @Test
    void getLoyaltyAccountByUserId_WithValidId_ShouldReturnAccount() {
        // Given
        when(loyaltyAccountRepository.findByUserId(1L)).thenReturn(Optional.of(testAccount));

        // When
        LoyaltyAccount result = loyaltyService.getLoyaltyAccountByUserId(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getBalance()).isEqualTo(1000);

        verify(loyaltyAccountRepository).findByUserId(1L);
    }

    @Test
    void getLoyaltyAccountByUserId_WithNonExistentId_ShouldThrowNotFoundException() {
        // Given
        when(loyaltyAccountRepository.findByUserId(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> loyaltyService.getLoyaltyAccountByUserId(999L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Loyalty account not found for user ID: 999");

        verify(loyaltyAccountRepository).findByUserId(999L);
    }

    @Test
    void updateLoyaltyAccount_WithValidAccount_ShouldUpdateAccount() {
        // Given
        testAccount.setBalance(1500);
        when(loyaltyAccountRepository.existsById(1L)).thenReturn(true);
        when(loyaltyAccountRepository.save(testAccount)).thenReturn(testAccount);

        // When
        LoyaltyAccount result = loyaltyService.updateLoyaltyAccount(testAccount);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getBalance()).isEqualTo(1500);

        verify(loyaltyAccountRepository).existsById(1L);
        verify(loyaltyAccountRepository).save(testAccount);
    }

    @Test
    void updateLoyaltyAccount_WithNonExistentAccount_ShouldThrowNotFoundException() {
        // Given
        testAccount.setId(999L);
        when(loyaltyAccountRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> loyaltyService.updateLoyaltyAccount(testAccount))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Loyalty account not found with ID: 999");

        verify(loyaltyAccountRepository).existsById(999L);
        verify(loyaltyAccountRepository, never()).save(any());
    }

    @Test
    void deleteLoyaltyAccount_WithValidAccount_ShouldDeleteAccount() {
        // Given
        when(loyaltyAccountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(loyaltyTransactionRepository.existsByAccountId(1L)).thenReturn(false);
        doNothing().when(loyaltyAccountRepository).delete(testAccount);

        // When
        loyaltyService.deleteLoyaltyAccount(1L);

        // Then
        verify(loyaltyAccountRepository).findById(1L);
        verify(loyaltyTransactionRepository).existsByAccountId(1L);
        verify(loyaltyAccountRepository).delete(testAccount);
    }

    @Test
    void deleteLoyaltyAccount_WithExistingTransactions_ShouldThrowBusinessRuleException() {
        // Given
        when(loyaltyAccountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(loyaltyTransactionRepository.existsByAccountId(1L)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> loyaltyService.deleteLoyaltyAccount(1L))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Cannot delete loyalty account with existing transactions");

        verify(loyaltyAccountRepository).findById(1L);
        verify(loyaltyTransactionRepository).existsByAccountId(1L);
        verify(loyaltyAccountRepository, never()).delete(any());
    }

    // ========== POINTS OPERATIONS TESTS ==========

    @Test
    void earnPoints_WithValidData_ShouldEarnPoints() {
        // Given
        BigDecimal amount = new BigDecimal("50.00");
        String reason = "Test earning";
        LoyaltyTransaction savedTransaction = new LoyaltyTransaction();
        savedTransaction.setId(1L);

        when(loyaltyAccountRepository.findByUserId(1L)).thenReturn(Optional.of(testAccount));
        when(loyaltyTransactionRepository.save(any(LoyaltyTransaction.class))).thenReturn(savedTransaction);
        when(loyaltyAccountRepository.save(any(LoyaltyAccount.class))).thenReturn(testAccount);

        // When
        LoyaltyTransaction result = loyaltyService.earnPoints(1L, amount, reason);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);

        verify(loyaltyAccountRepository).findByUserId(1L);
        verify(loyaltyTransactionRepository).save(any(LoyaltyTransaction.class));
        verify(loyaltyAccountRepository).save(any(LoyaltyAccount.class));
    }

    @Test
    void earnPoints_WithInvalidAmount_ShouldThrowBusinessRuleException() {
        // Given
        BigDecimal invalidAmount = new BigDecimal("-10.00");

        // When & Then
        assertThatThrownBy(() -> loyaltyService.earnPoints(1L, invalidAmount, "Test"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Amount must be positive");

        verify(loyaltyAccountRepository, never()).findByUserId(any());
        verify(loyaltyTransactionRepository, never()).save(any());
    }

    @Test
    void redeemPoints_WithValidData_ShouldRedeemPoints() {
        // Given
        Integer points = 100;
        String reason = "Test redemption";
        LoyaltyTransaction savedTransaction = new LoyaltyTransaction();
        savedTransaction.setId(1L);

        when(loyaltyAccountRepository.findByUserId(1L)).thenReturn(Optional.of(testAccount));
        when(loyaltyTransactionRepository.save(any(LoyaltyTransaction.class))).thenReturn(savedTransaction);
        when(loyaltyAccountRepository.save(any(LoyaltyAccount.class))).thenReturn(testAccount);

        // When
        LoyaltyTransaction result = loyaltyService.redeemPoints(1L, points, reason);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);

        verify(loyaltyAccountRepository).findByUserId(1L);
        verify(loyaltyTransactionRepository).save(any(LoyaltyTransaction.class));
        verify(loyaltyAccountRepository).save(any(LoyaltyAccount.class));
    }

    @Test
    void redeemPoints_WithInsufficientPoints_ShouldThrowBusinessRuleException() {
        // Given
        Integer points = 2000; // More than available balance
        testAccount.setBalance(1000);

        when(loyaltyAccountRepository.findByUserId(1L)).thenReturn(Optional.of(testAccount));

        // When & Then
        assertThatThrownBy(() -> loyaltyService.redeemPoints(1L, points, "Test"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Insufficient points");

        verify(loyaltyAccountRepository).findByUserId(1L);
        verify(loyaltyTransactionRepository, never()).save(any());
    }

    @Test
    void redeemPoints_WithInvalidPoints_ShouldThrowBusinessRuleException() {
        // Given
        Integer invalidPoints = -50;

        // When & Then
        assertThatThrownBy(() -> loyaltyService.redeemPoints(1L, invalidPoints, "Test"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Points must be positive");

        verify(loyaltyAccountRepository, never()).findByUserId(any());
        verify(loyaltyTransactionRepository, never()).save(any());
    }

    @Test
    void getAvailablePoints_WithValidUser_ShouldReturnBalance() {
        // Given
        when(loyaltyAccountRepository.findByUserId(1L)).thenReturn(Optional.of(testAccount));

        // When
        Integer result = loyaltyService.getAvailablePoints(1L);

        // Then
        assertThat(result).isEqualTo(1000);

        verify(loyaltyAccountRepository).findByUserId(1L);
    }

    // ========== POINT CALCULATION TESTS ==========

    @Test
    void calculatePointsFromAmount_WithValidAmount_ShouldCalculateCorrectly() {
        // Given
        BigDecimal amount = new BigDecimal("50.25");

        // When
        Integer result = loyaltyService.calculatePointsFromAmount(amount);

        // Then
        assertThat(result).isEqualTo(50); // 1 point per $1, rounded down
    }

    @Test
    void calculatePointsFromAmount_WithZeroAmount_ShouldReturnZero() {
        // Given
        BigDecimal amount = BigDecimal.ZERO;

        // When
        Integer result = loyaltyService.calculatePointsFromAmount(amount);

        // Then
        assertThat(result).isEqualTo(0);
    }

    @Test
    void calculatePointsDiscount_WithValidPoints_ShouldCalculateCorrectly() {
        // Given
        Integer points = 1000;

        when(loyaltyAccountRepository.findByUserId(1L)).thenReturn(Optional.of(testAccount));

        // When
        BigDecimal result = loyaltyService.calculatePointsDiscount(1L, points);

        // Then
        assertThat(result).isEqualTo(new BigDecimal("10.00")); // 1000 points = $10.00
    }

    @Test
    void calculatePointsDiscountAmount_WithValidPoints_ShouldCalculateCorrectly() {
        // Given
        Integer points = 500;

        // When
        BigDecimal result = loyaltyService.calculatePointsDiscountAmount(points);

        // Then
        assertThat(result).isEqualTo(new BigDecimal("5.00")); // 500 points = $5.00
    }

    @Test
    void calculateMaxRedeemablePoints_WithValidData_ShouldCalculateCorrectly() {
        // Given
        BigDecimal maxAmount = new BigDecimal("50.00");
        testAccount.setBalance(8000); // User has 8000 points

        when(loyaltyAccountRepository.findByUserId(1L)).thenReturn(Optional.of(testAccount));

        // When
        Integer result = loyaltyService.calculateMaxRedeemablePoints(1L, maxAmount);

        // Then
        // Max should be min of: available points (8000), amount-based (5000), business rule (10000)
        assertThat(result).isEqualTo(5000);
    }

    // ========== RESERVATION INTEGRATION TESTS ==========

    @Test
    void validateReservationPointsRedemption_WithValidData_ShouldPass() {
        // Given
        Integer points = 100;
        BigDecimal reservationTotal = new BigDecimal("10.00");

        when(loyaltyAccountRepository.findByUserId(1L)).thenReturn(Optional.of(testAccount));

        // When & Then - Should not throw exception
        assertThatCode(() -> loyaltyService.validateReservationPointsRedemption(1L, points, reservationTotal))
            .doesNotThrowAnyException();

        verify(loyaltyAccountRepository).findByUserId(1L);
    }

    @Test
    void validateReservationPointsRedemption_WithExcessivePoints_ShouldThrowBusinessRuleException() {
        // Given
        Integer points = 2000; // More than reservation total
        BigDecimal reservationTotal = new BigDecimal("10.00");

        when(loyaltyAccountRepository.findByUserId(1L)).thenReturn(Optional.of(testAccount));

        // When & Then
        assertThatThrownBy(() -> loyaltyService.validateReservationPointsRedemption(1L, points, reservationTotal))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Insufficient points. Available: 1000, Requested: 2000");

        verify(loyaltyAccountRepository).findByUserId(1L);
    }

    @Test
    void validateReservationPointsRedemption_WithMinimumPoints_ShouldThrowBusinessRuleException() {
        // Given
        Integer points = 50; // Less than minimum 100
        BigDecimal reservationTotal = new BigDecimal("10.00");

        when(loyaltyAccountRepository.findByUserId(1L)).thenReturn(Optional.of(testAccount));

        // When & Then
        assertThatThrownBy(() -> loyaltyService.validateReservationPointsRedemption(1L, points, reservationTotal))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Minimum redemption is 100 points");

        verify(loyaltyAccountRepository).findByUserId(1L);
    }

    @Test
    void validateReservationPointsRedemption_WithMaximumPoints_ShouldThrowBusinessRuleException() {
        // Given
        Integer points = 15000; // More than maximum 10000
        BigDecimal reservationTotal = new BigDecimal("200.00");

        when(loyaltyAccountRepository.findByUserId(1L)).thenReturn(Optional.of(testAccount));

        // When & Then
        assertThatThrownBy(() -> loyaltyService.validateReservationPointsRedemption(1L, points, reservationTotal))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Insufficient points. Available: 1000, Requested: 15000");

        verify(loyaltyAccountRepository).findByUserId(1L);
    }

    // ========== TRANSACTION HISTORY TESTS ==========

    @Test
    void getTransactionHistory_WithValidUser_ShouldReturnTransactions() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<LoyaltyTransaction> expectedPage = new PageImpl<>(Arrays.asList(testTransaction), pageable, 1);

        when(loyaltyAccountRepository.findByUserId(1L)).thenReturn(Optional.of(testAccount));
        when(loyaltyTransactionRepository.findByAccountOrderByCreatedAtDesc(testAccount, pageable))
            .thenReturn(expectedPage);

        // When
        Page<LoyaltyTransaction> result = loyaltyService.getTransactionHistory(1L, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getType()).isEqualTo(LoyaltyTxType.EARN);

        verify(loyaltyAccountRepository).findByUserId(1L);
        verify(loyaltyTransactionRepository).findByAccountOrderByCreatedAtDesc(testAccount, pageable);
    }

    @Test
    void getTransactionsByType_WithValidData_ShouldReturnFilteredTransactions() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<LoyaltyTransaction> expectedPage = new PageImpl<>(Arrays.asList(testTransaction), pageable, 1);

        when(loyaltyAccountRepository.findByUserId(1L)).thenReturn(Optional.of(testAccount));
        when(loyaltyTransactionRepository.findByAccountAndType(testAccount, LoyaltyTxType.EARN, pageable))
            .thenReturn(expectedPage);

        // When
        Page<LoyaltyTransaction> result = loyaltyService.getTransactionsByType(1L, LoyaltyTxType.EARN, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getType()).isEqualTo(LoyaltyTxType.EARN);

        verify(loyaltyAccountRepository).findByUserId(1L);
        verify(loyaltyTransactionRepository).findByAccountAndType(testAccount, LoyaltyTxType.EARN, pageable);
    }

    // ========== INTEGRATION METHODS TESTS ==========

    @Test
    void processReservationPoints_WithValidReservation_ShouldEarnPoints() {
        // Given
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        when(loyaltyAccountRepository.findByUserId(1L)).thenReturn(Optional.of(testAccount));
        when(loyaltyTransactionRepository.save(any(LoyaltyTransaction.class))).thenReturn(testTransaction);
        when(loyaltyAccountRepository.save(any(LoyaltyAccount.class))).thenReturn(testAccount);

        // When
        LoyaltyTransaction result = loyaltyService.processReservationPoints(1L);

        // Then
        assertThat(result).isNotNull();

        verify(reservationRepository, times(2)).findById(1L); // Called twice: once in processReservationPoints, once in earnPoints
        verify(loyaltyAccountRepository).findByUserId(1L);
        verify(loyaltyTransactionRepository).save(any(LoyaltyTransaction.class));
        verify(loyaltyAccountRepository).save(any(LoyaltyAccount.class));
    }

    @Test
    void processReservationPoints_WithNonExistentReservation_ShouldThrowNotFoundException() {
        // Given
        when(reservationRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> loyaltyService.processReservationPoints(999L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Reservation not found with ID: 999");

        verify(reservationRepository).findById(999L);
        verify(loyaltyAccountRepository, never()).findByUserId(any());
    }

    @Test
    void refundReservationPoints_WithValidData_ShouldRefundPoints() {
        // Given
        Integer pointsUsed = 500;
        testAccount.setBalance(1000);
        LoyaltyTransaction refundTx = new LoyaltyTransaction(testAccount, LoyaltyTxType.REFUND, 500, testReservation);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        when(loyaltyAccountRepository.findByUserId(1L)).thenReturn(Optional.of(testAccount));
        when(loyaltyTransactionRepository.save(any(LoyaltyTransaction.class))).thenReturn(refundTx);
        when(loyaltyAccountRepository.save(any(LoyaltyAccount.class))).thenReturn(testAccount);

        // When
        LoyaltyTransaction result = loyaltyService.refundReservationPoints(1L, pointsUsed);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(LoyaltyTxType.REFUND);
        assertThat(result.getPoints()).isEqualTo(500);
        assertThat(testAccount.getBalance()).isEqualTo(1500);

        verify(reservationRepository, times(1)).findById(1L);
        verify(loyaltyAccountRepository).findByUserId(1L);
        verify(loyaltyTransactionRepository).save(any(LoyaltyTransaction.class));
        verify(loyaltyAccountRepository).save(any(LoyaltyAccount.class));
    }

    @Test
    void refundReservationPoints_WithZeroPoints_ShouldReturnNull() {
        // Given
        Integer pointsUsed = 0;

        // When
        LoyaltyTransaction result = loyaltyService.refundReservationPoints(1L, pointsUsed);

        // Then
        assertThat(result).isNull();

        verify(reservationRepository, never()).findById(any());
        verify(loyaltyAccountRepository, never()).findByUserId(any());
    }

    @Test
    void clawbackReservationPoints_WithEarnedPoints_ShouldDeductPointsWithClawbackType() {
        // Given
        testAccount.setBalance(1000);
        LoyaltyTransaction earnTx = new LoyaltyTransaction(testAccount, LoyaltyTxType.EARN, 200, testReservation);
        LoyaltyTransaction clawbackTx = new LoyaltyTransaction(testAccount, LoyaltyTxType.CLAWBACK, 200, testReservation);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        when(loyaltyTransactionRepository.existsByTypeAndReservationId(LoyaltyTxType.CLAWBACK, 1L)).thenReturn(false);
        when(loyaltyTransactionRepository.findByTypeAndReservationId(LoyaltyTxType.EARN, 1L))
                .thenReturn(Collections.singletonList(earnTx));
        when(loyaltyAccountRepository.findByUserId(1L)).thenReturn(Optional.of(testAccount));
        when(loyaltyTransactionRepository.save(any(LoyaltyTransaction.class))).thenReturn(clawbackTx);
        when(loyaltyAccountRepository.save(any(LoyaltyAccount.class))).thenReturn(testAccount);

        // When
        LoyaltyTransaction result = loyaltyService.clawbackReservationPoints(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(LoyaltyTxType.CLAWBACK);
        assertThat(result.getPoints()).isEqualTo(200);
        assertThat(testAccount.getBalance()).isEqualTo(800);

        verify(loyaltyTransactionRepository).save(any(LoyaltyTransaction.class));
        verify(loyaltyAccountRepository).save(testAccount);
    }

    @Test
    void clawbackReservationPoints_WhenBalanceLowerThanEarned_ShouldClampToZero() {
        // Given - user spent points elsewhere and only has 50 points left, but earned 200
        testAccount.setBalance(50);
        LoyaltyTransaction earnTx = new LoyaltyTransaction(testAccount, LoyaltyTxType.EARN, 200, testReservation);
        LoyaltyTransaction clawbackTx = new LoyaltyTransaction(testAccount, LoyaltyTxType.CLAWBACK, 200, testReservation);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        when(loyaltyTransactionRepository.existsByTypeAndReservationId(LoyaltyTxType.CLAWBACK, 1L)).thenReturn(false);
        when(loyaltyTransactionRepository.findByTypeAndReservationId(LoyaltyTxType.EARN, 1L))
                .thenReturn(Collections.singletonList(earnTx));
        when(loyaltyAccountRepository.findByUserId(1L)).thenReturn(Optional.of(testAccount));
        when(loyaltyTransactionRepository.save(any(LoyaltyTransaction.class))).thenReturn(clawbackTx);
        when(loyaltyAccountRepository.save(any(LoyaltyAccount.class))).thenReturn(testAccount);

        // When
        LoyaltyTransaction result = loyaltyService.clawbackReservationPoints(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(testAccount.getBalance()).isEqualTo(0);
        verify(loyaltyAccountRepository).save(testAccount);
    }

    @Test
    void clawbackReservationPoints_WithoutEarnedPoints_ShouldReturnNull() {
        // Given
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        when(loyaltyTransactionRepository.existsByTypeAndReservationId(LoyaltyTxType.CLAWBACK, 1L)).thenReturn(false);
        when(loyaltyTransactionRepository.findByTypeAndReservationId(LoyaltyTxType.EARN, 1L))
                .thenReturn(Collections.emptyList());

        // When
        LoyaltyTransaction result = loyaltyService.clawbackReservationPoints(1L);

        // Then
        assertThat(result).isNull();
        verify(loyaltyTransactionRepository, never()).save(any());
        verify(loyaltyAccountRepository, never()).save(any());
    }

    @Test
    void clawbackReservationPoints_AlreadyClawedBack_ShouldReturnNull() {
        // Given
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        when(loyaltyTransactionRepository.existsByTypeAndReservationId(LoyaltyTxType.CLAWBACK, 1L)).thenReturn(true);

        // When
        LoyaltyTransaction result = loyaltyService.clawbackReservationPoints(1L);

        // Then
        assertThat(result).isNull();
        verify(loyaltyTransactionRepository, never()).findByTypeAndReservationId(any(), any());
        verify(loyaltyTransactionRepository, never()).save(any());
    }

    // ========== STATISTICS TESTS ==========

    @Test
    void getLoyaltyStatistics_ShouldReturnCorrectStatistics() {
        // Given
        when(loyaltyAccountRepository.count()).thenReturn(10L);
        when(loyaltyTransactionRepository.count()).thenReturn(50L);
        when(loyaltyAccountRepository.getTotalBalance()).thenReturn(5000L);
        when(loyaltyAccountRepository.getAverageBalance()).thenReturn(500.0);
        when(loyaltyTransactionRepository.getTotalPointsByType(LoyaltyTxType.EARN)).thenReturn(10000L);
        when(loyaltyTransactionRepository.getTotalPointsByType(LoyaltyTxType.REDEEM)).thenReturn(5000L);

        // When
        LoyaltyService.LoyaltyStatistics result = loyaltyService.getLoyaltyStatistics();

        // Then
        assertThat(result.getTotalAccounts()).isEqualTo(10);
        assertThat(result.getTotalTransactions()).isEqualTo(50);
        assertThat(result.getTotalBalance()).isEqualTo(5000);
        assertThat(result.getAverageBalance()).isEqualTo(500.0);
        assertThat(result.getTotalEarnedPoints()).isEqualTo(10000);
        assertThat(result.getTotalRedeemedPoints()).isEqualTo(5000);

        verify(loyaltyAccountRepository).count();
        verify(loyaltyTransactionRepository).count();
        verify(loyaltyAccountRepository).getTotalBalance();
        verify(loyaltyAccountRepository).getAverageBalance();
        verify(loyaltyTransactionRepository).getTotalPointsByType(LoyaltyTxType.EARN);
        verify(loyaltyTransactionRepository).getTotalPointsByType(LoyaltyTxType.REDEEM);
    }
}