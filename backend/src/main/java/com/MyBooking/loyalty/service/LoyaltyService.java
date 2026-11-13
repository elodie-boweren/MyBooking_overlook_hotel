//Loyalty Account Management: Create, update, get account by user
//Points Operations: Earn points (from reservations), redeem points (for rewards)
//Transaction History: Track all point transactions with audit trail
//Business Rules: Point calculation, redemption limits, expiration policies

package com.MyBooking.loyalty.service;

import com.MyBooking.auth.domain.User;
import com.MyBooking.auth.repository.UserRepository;
import com.MyBooking.common.exception.BusinessRuleException;
import com.MyBooking.common.exception.NotFoundException;
import com.MyBooking.loyalty.domain.LoyaltyAccount;
import com.MyBooking.loyalty.domain.LoyaltyTransaction;
import com.MyBooking.loyalty.domain.LoyaltyTxType;
import com.MyBooking.loyalty.repository.LoyaltyAccountRepository;
import com.MyBooking.loyalty.repository.LoyaltyTransactionRepository;
import com.MyBooking.reservation.domain.Reservation;
import com.MyBooking.reservation.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class LoyaltyService {

    @Autowired
    private LoyaltyAccountRepository loyaltyAccountRepository;

    @Autowired
    private LoyaltyTransactionRepository loyaltyTransactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    // ========== CORE ACCOUNT MANAGEMENT ==========

    /**
     * Create a new loyalty account for a user
     */
    public LoyaltyAccount createLoyaltyAccount(Long userId) {
        // Validate user exists
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));

        // Check if account already exists
        if (loyaltyAccountRepository.existsByUserId(userId)) {
            throw new BusinessRuleException("Loyalty account already exists for user ID: " + userId);
        }

        LoyaltyAccount account = new LoyaltyAccount(user);
        return loyaltyAccountRepository.save(account);
    }

    /**
     * Get loyalty account by user ID
     */
    @Transactional(readOnly = true)
    public LoyaltyAccount getLoyaltyAccountByUserId(Long userId) {
        return loyaltyAccountRepository.findByUserId(userId)
            .orElseThrow(() -> new NotFoundException("Loyalty account not found for user ID: " + userId));
    }

    /**
     * Get loyalty account by user ID (optional)
     */
    @Transactional(readOnly = true)
    public Optional<LoyaltyAccount> getLoyaltyAccountByUserIdOptional(Long userId) {
        return loyaltyAccountRepository.findByUserId(userId);
    }

    /**
     * Update loyalty account
     */
    public LoyaltyAccount updateLoyaltyAccount(LoyaltyAccount account) {
        if (account.getId() == null) {
            throw new BusinessRuleException("Account ID is required for update");
        }

        if (!loyaltyAccountRepository.existsById(account.getId())) {
            throw new NotFoundException("Loyalty account not found with ID: " + account.getId());
        }

        return loyaltyAccountRepository.save(account);
    }

    /**
     * Delete loyalty account
     */
    public void deleteLoyaltyAccount(Long accountId) {
        LoyaltyAccount account = loyaltyAccountRepository.findById(accountId)
            .orElseThrow(() -> new NotFoundException("Loyalty account not found with ID: " + accountId));

        // Check if account has transactions
        if (loyaltyTransactionRepository.existsByAccountId(accountId)) {
            throw new BusinessRuleException("Cannot delete loyalty account with existing transactions");
        }

        loyaltyAccountRepository.delete(account);
    }

    // ========== POINTS OPERATIONS ==========

    /**
     * Earn points for a user (typically from reservation)
     */
    public LoyaltyTransaction earnPoints(Long userId, BigDecimal amount, String reason) {
        return earnPoints(userId, amount, reason, null);
    }

    /**
     * Earn points for a user with reservation reference
     */
    public LoyaltyTransaction earnPoints(Long userId, BigDecimal amount, String reason, Long reservationId) {
        // Validate inputs
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Amount must be positive");
        }

        // Get or create loyalty account with pessimistic write lock
        LoyaltyAccount account = loyaltyAccountRepository.findByUserIdForUpdate(userId)
            .orElseGet(() -> getOrCreateLoyaltyAccount(userId));

        // Calculate points (1 point per $1 spent)
        Integer points = calculatePointsFromAmount(amount);

        // Create transaction
        LoyaltyTransaction transaction = new LoyaltyTransaction(account, LoyaltyTxType.EARN, points);
        transaction.setReservation(reservationId != null ? 
            reservationRepository.findById(reservationId).orElse(null) : null);

        // Save transaction
        LoyaltyTransaction savedTransaction = loyaltyTransactionRepository.save(transaction);

        // Update account balance
        account.setBalance(account.getBalance() + points);
        loyaltyAccountRepository.save(account);

        return savedTransaction;
    }

    /**
     * Redeem points for a user
     */
    public LoyaltyTransaction redeemPoints(Long userId, Integer points, String reason) {
        // Validate inputs
        if (points == null || points <= 0) {
            throw new BusinessRuleException("Points must be positive");
        }

        // Get loyalty account with pessimistic write lock
        LoyaltyAccount account = loyaltyAccountRepository.findByUserIdForUpdate(userId)
            .orElseThrow(() -> new NotFoundException("Loyalty account not found for user ID: " + userId));

        // Validate redemption
        validateRedemptionRequest(account, points);

        // Create transaction
        LoyaltyTransaction transaction = new LoyaltyTransaction(account, LoyaltyTxType.REDEEM, points);
        LoyaltyTransaction savedTransaction = loyaltyTransactionRepository.save(transaction);

        // Update account balance
        account.setBalance(account.getBalance() - points);
        loyaltyAccountRepository.save(account);

        return savedTransaction;
    }

    /**
     * Get available points for a user
     */
    @Transactional(readOnly = true)
    public Integer getAvailablePoints(Long userId) {
        LoyaltyAccount account = getLoyaltyAccountByUserId(userId);
        return account.getBalance();
    }

    // ========== TRANSACTION HISTORY ==========

    /**
     * Get transaction history for a user
     */
    @Transactional(readOnly = true)
    public Page<LoyaltyTransaction> getTransactionHistory(Long userId, Pageable pageable) {
        LoyaltyAccount account = getLoyaltyAccountByUserId(userId);
        return loyaltyTransactionRepository.findByAccountOrderByCreatedAtDesc(account, pageable);
    }

    /**
     * Get transactions by type for a user
     */
    @Transactional(readOnly = true)
    public Page<LoyaltyTransaction> getTransactionsByType(Long userId, LoyaltyTxType type, Pageable pageable) {
        LoyaltyAccount account = getLoyaltyAccountByUserId(userId);
        return loyaltyTransactionRepository.findByAccountAndType(account, type, pageable);
    }

    /**
     * Get recent transactions for a user
     */
    @Transactional(readOnly = true)
    public List<LoyaltyTransaction> getRecentTransactions(Long userId, int days) {
        LoyaltyAccount account = getLoyaltyAccountByUserId(userId);
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return loyaltyTransactionRepository.findByAccountIdAndCreatedAtAfterOrderByCreatedAtDesc(account.getId(), since);
    }

    // ========== BUSINESS OPERATIONS ==========

    /**
     * Calculate points from monetary amount (1 point per $1)
     */
    @Transactional(readOnly = true)
    public Integer calculatePointsFromAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return amount.setScale(0, RoundingMode.DOWN).intValue();
    }

    /**
     * Calculate discount amount from points (1 point = $0.01)
     */
    @Transactional(readOnly = true)
    public BigDecimal calculatePointsDiscount(Long userId, Integer points) {
        if (points == null || points <= 0) {
            return BigDecimal.ZERO;
        }
        
        // Validate user has enough points
        validateRedemptionRequest(userId, points);
        
        // Convert points to dollar amount (1 point = $0.01)
        return BigDecimal.valueOf(points).multiply(new BigDecimal("0.01"));
    }

    /**
     * Calculate discount amount from points without validation (for price calculation)
     */
    @Transactional(readOnly = true)
    public BigDecimal calculatePointsDiscountAmount(Integer points) {
        if (points == null || points <= 0) {
            return BigDecimal.ZERO;
        }
        
        // Convert points to dollar amount (1 point = $0.01)
        return BigDecimal.valueOf(points).multiply(new BigDecimal("0.01"));
    }

    /**
     * Calculate maximum points that can be redeemed for a given amount
     */
    @Transactional(readOnly = true)
    public Integer calculateMaxRedeemablePoints(Long userId, BigDecimal maxAmount) {
        if (maxAmount == null || maxAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        
        Integer availablePoints = getAvailablePoints(userId);
        Integer maxPointsByAmount = maxAmount.multiply(new BigDecimal("100")).intValue(); // $1 = 100 points
        Integer maxPointsByBusinessRule = 10000; // Business rule: max 10,000 points per transaction
        
        return Math.min(Math.min(availablePoints, maxPointsByAmount), maxPointsByBusinessRule);
    }

    /**
     * Validate points redemption for reservation (with reservation-specific rules)
     */
    @Transactional(readOnly = true)
    public void validateReservationPointsRedemption(Long userId, Integer points, BigDecimal reservationTotal) {
        if (points == null || points <= 0) {
            throw new BusinessRuleException("Points must be positive");
        }

        LoyaltyAccount account = getLoyaltyAccountByUserId(userId);
        
        // Check if user has enough points
        if (account.getBalance() < points) {
            throw new BusinessRuleException("Insufficient points. Available: " + 
                account.getBalance() + ", Requested: " + points);
        }

        // Business rule: minimum redemption of 100 points
        if (points < 100) {
            throw new BusinessRuleException("Minimum redemption is 100 points");
        }

        // Business rule: maximum redemption of 10,000 points per transaction
        if (points > 10000) {
            throw new BusinessRuleException("Maximum redemption is 10,000 points per transaction");
        }

        // Business rule: points discount cannot exceed reservation total
        BigDecimal pointsDiscount = calculatePointsDiscountAmount(points);
        if (pointsDiscount.compareTo(reservationTotal) > 0) {
            throw new BusinessRuleException("Points discount (" + pointsDiscount + 
                ") cannot exceed reservation total (" + reservationTotal + ")");
        }
    }

    /**
     * Validate redemption request
     */
    @Transactional(readOnly = true)
    public void validateRedemptionRequest(Long userId, Integer points) {
        LoyaltyAccount account = getLoyaltyAccountByUserId(userId);
        validateRedemptionRequest(account, points);
    }

    /**
     * Validate redemption request (private helper)
     */
    private void validateRedemptionRequest(LoyaltyAccount account, Integer points) {
        if (points <= 0) {
            throw new BusinessRuleException("Points must be positive");
        }

        if (account.getBalance() < points) {
            throw new BusinessRuleException("Insufficient points. Available: " + 
                account.getBalance() + ", Requested: " + points);
        }

        // Business rule: minimum redemption of 100 points
        if (points < 100) {
            throw new BusinessRuleException("Minimum redemption is 100 points");
        }

        // Business rule: maximum redemption of 10,000 points per transaction
        if (points > 10000) {
            throw new BusinessRuleException("Maximum redemption is 10,000 points per transaction");
        }
    }

    /**
     * Get or create loyalty account for user
     */
    private LoyaltyAccount getOrCreateLoyaltyAccount(Long userId) {
        return loyaltyAccountRepository.findByUserId(userId)
            .orElseGet(() -> {
                userRepository.findByIdForUpdate(userId)
                    .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));
                return loyaltyAccountRepository.findByUserId(userId)
                    .orElseGet(() -> createLoyaltyAccount(userId));
            });
    }

    // ========== ADMIN OPERATIONS ==========

    /**
     * Get all loyalty accounts with pagination
     */
    @Transactional(readOnly = true)
    public Page<LoyaltyAccount> getAllLoyaltyAccounts(Pageable pageable) {
        return loyaltyAccountRepository.findAll(pageable);
    }

    /**
     * Get accounts with high balance
     */
    @Transactional(readOnly = true)
    public Page<LoyaltyAccount> getAccountsWithHighBalance(Integer threshold, Pageable pageable) {
        return loyaltyAccountRepository.findAccountsWithHighBalance(threshold, pageable);
    }

    /**
     * Get accounts with zero balance
     */
    @Transactional(readOnly = true)
    public Page<LoyaltyAccount> getAccountsWithZeroBalance(Pageable pageable) {
        return loyaltyAccountRepository.findAccountsWithZeroBalance(pageable);
    }

    /**
     * Get all transactions with pagination
     */
    @Transactional(readOnly = true)
    public Page<LoyaltyTransaction> getAllTransactions(Pageable pageable) {
        return loyaltyTransactionRepository.findAll(pageable);
    }

    /**
     * Get high-value transactions
     */
    @Transactional(readOnly = true)
    public Page<LoyaltyTransaction> getHighValueTransactions(Integer threshold, Pageable pageable) {
        return loyaltyTransactionRepository.findHighValueTransactions(threshold, pageable);
    }

    /**
     * Get transactions by type
     */
    @Transactional(readOnly = true)
    public Page<LoyaltyTransaction> getTransactionsByType(LoyaltyTxType type, Pageable pageable) {
        return loyaltyTransactionRepository.findByTypeOrderByCreatedAtDesc(type, pageable);
    }

    // ========== STATISTICS & ANALYTICS ==========

    /**
     * Get loyalty program statistics
     */
    @Transactional(readOnly = true)
    public LoyaltyStatistics getLoyaltyStatistics() {
        long totalAccounts = loyaltyAccountRepository.count();
        long totalTransactions = loyaltyTransactionRepository.count();
        
        Long totalBalance = loyaltyAccountRepository.getTotalBalance();
        Double averageBalance = loyaltyAccountRepository.getAverageBalance();
        
        Long totalEarnedPoints = loyaltyTransactionRepository.getTotalPointsByType(LoyaltyTxType.EARN);
        Long totalRedeemedPoints = loyaltyTransactionRepository.getTotalPointsByType(LoyaltyTxType.REDEEM);
        
        return new LoyaltyStatistics(
            totalAccounts,
            totalTransactions,
            totalBalance != null ? totalBalance : 0L,
            averageBalance != null ? averageBalance : 0.0,
            totalEarnedPoints != null ? totalEarnedPoints : 0L,
            totalRedeemedPoints != null ? totalRedeemedPoints : 0L
        );
    }

    /**
     * Get balance statistics by user role
     */
    @Transactional(readOnly = true)
    public List<Object[]> getBalanceStatisticsByUserRole() {
        return loyaltyAccountRepository.getBalanceStatisticsByUserRole();
    }

    /**
     * Get points statistics by transaction type
     */
    @Transactional(readOnly = true)
    public List<Object[]> getPointsStatisticsByType() {
        return loyaltyTransactionRepository.getPointsStatisticsByType();
    }

    // ========== INTEGRATION METHODS ==========

    /**
     * Process points for a confirmed reservation
     */
    @Transactional(noRollbackFor = RuntimeException.class)
    public LoyaltyTransaction processReservationPoints(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new NotFoundException("Reservation not found with ID: " + reservationId));

        if (reservation.getClient() == null) {
            throw new BusinessRuleException("Reservation has no associated client");
        }

        return earnPoints(
            reservation.getClient().getId(),
            reservation.getTotalPrice(),
            "Points earned from reservation #" + reservationId,
            reservationId
        );
    }

    /**
     * Refund redeemed points for a cancelled reservation (1:1 points restoration).
     * Enforces INV-LOY-002 and INV-LOY-003 by creating a LoyaltyTransaction of type REFUND.
     */
    public LoyaltyTransaction refundReservationPoints(Long reservationId, Integer pointsUsed) {
        if (pointsUsed == null || pointsUsed <= 0) {
            return null; // No points to refund
        }

        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new NotFoundException("Reservation not found with ID: " + reservationId));

        if (reservation.getClient() == null) {
            throw new BusinessRuleException("Reservation has no associated client");
        }

        LoyaltyAccount account = loyaltyAccountRepository.findByUserIdForUpdate(reservation.getClient().getId())
            .orElseGet(() -> getOrCreateLoyaltyAccount(reservation.getClient().getId()));

        // Create REFUND transaction directly restoring points 1:1
        LoyaltyTransaction transaction = new LoyaltyTransaction(account, LoyaltyTxType.REFUND, pointsUsed, reservation);
        LoyaltyTransaction savedTransaction = loyaltyTransactionRepository.save(transaction);

        // Update account balance
        account.setBalance(account.getBalance() + pointsUsed);
        loyaltyAccountRepository.save(account);

        return savedTransaction;
    }

    /**
     * Claw back points earned from a reservation when it is cancelled prior to check-in.
     * Enforces INV-LOY-001 (non-negative balance) and INV-LOY-002 (symmetrical cancellation clawback).
     */
    public LoyaltyTransaction clawbackReservationPoints(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new NotFoundException("Reservation not found with ID: " + reservationId));

        if (reservation.getClient() == null) {
            throw new BusinessRuleException("Reservation has no associated client");
        }

        // Acquire pessimistic write lock on the user's loyalty account BEFORE idempotency check
        // to serialize concurrent clawbacks and prevent double-deduction (TOCTOU).
        LoyaltyAccount account = loyaltyAccountRepository.findByUserIdForUpdate(reservation.getClient().getId())
            .orElseGet(() -> getOrCreateLoyaltyAccount(reservation.getClient().getId()));

        // Check if clawback has already been executed (idempotency guard under lock)
        if (loyaltyTransactionRepository.existsByTypeAndReservationId(LoyaltyTxType.CLAWBACK, reservationId)) {
            return null;
        }

        // Find all EARN transactions for this reservation
        List<LoyaltyTransaction> earnTransactions = loyaltyTransactionRepository
            .findByTypeAndReservationId(LoyaltyTxType.EARN, reservationId);

        int totalEarnedPoints = earnTransactions.stream()
            .mapToInt(LoyaltyTransaction::getPoints)
            .sum();

        if (totalEarnedPoints <= 0) {
            return null; // No earned points to claw back
        }

        // Create CLAWBACK transaction
        LoyaltyTransaction transaction = new LoyaltyTransaction(account, LoyaltyTxType.CLAWBACK, totalEarnedPoints, reservation);
        LoyaltyTransaction savedTransaction = loyaltyTransactionRepository.save(transaction);

        // Deduct points from balance, guaranteeing balance >= 0 (INV-LOY-001)
        int updatedBalance = Math.max(0, account.getBalance() - totalEarnedPoints);
        account.setBalance(updatedBalance);
        loyaltyAccountRepository.save(account);

        return savedTransaction;
    }

    // ========== INNER CLASSES ==========

    /**
     * Loyalty program statistics data class
     */
    public static class LoyaltyStatistics {
        private final long totalAccounts;
        private final long totalTransactions;
        private final long totalBalance;
        private final double averageBalance;
        private final long totalEarnedPoints;
        private final long totalRedeemedPoints;

        public LoyaltyStatistics(long totalAccounts, long totalTransactions, long totalBalance,
                               double averageBalance, long totalEarnedPoints, long totalRedeemedPoints) {
            this.totalAccounts = totalAccounts;
            this.totalTransactions = totalTransactions;
            this.totalBalance = totalBalance;
            this.averageBalance = averageBalance;
            this.totalEarnedPoints = totalEarnedPoints;
            this.totalRedeemedPoints = totalRedeemedPoints;
        }

        // Getters
        public long getTotalAccounts() { return totalAccounts; }
        public long getTotalTransactions() { return totalTransactions; }
        public long getTotalBalance() { return totalBalance; }
        public double getAverageBalance() { return averageBalance; }
        public long getTotalEarnedPoints() { return totalEarnedPoints; }
        public long getTotalRedeemedPoints() { return totalRedeemedPoints; }
    }

    // ==================== DTO-AWARE METHODS ====================

    // Loyalty Account DTO methods
    @Transactional
    public com.MyBooking.loyalty.dto.LoyaltyAccountResponseDto createLoyaltyAccountAsDto(Long userId) {
        LoyaltyAccount account = createLoyaltyAccount(userId);
        return convertToLoyaltyAccountResponseDto(account);
    }

    @Transactional(readOnly = true)
    public com.MyBooking.loyalty.dto.LoyaltyAccountResponseDto getLoyaltyAccountByUserIdAsDto(Long userId) {
        LoyaltyAccount account = getLoyaltyAccountByUserId(userId);
        return convertToLoyaltyAccountResponseDto(account);
    }

    @Transactional(readOnly = true)
    public com.MyBooking.loyalty.dto.LoyaltyAccountResponseDto getLoyaltyAccountByIdAsDto(Long accountId) {
        LoyaltyAccount account = loyaltyAccountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Loyalty account not found with ID: " + accountId));
        return convertToLoyaltyAccountResponseDto(account);
    }

    @Transactional
    public void deleteLoyaltyAccountAsDto(Long accountId) {
        deleteLoyaltyAccount(accountId);
    }

    @Transactional(readOnly = true)
    public Page<com.MyBooking.loyalty.dto.LoyaltyAccountResponseDto> getAllLoyaltyAccountsAsDto(Pageable pageable) {
        Page<LoyaltyAccount> accounts = getAllLoyaltyAccounts(pageable);
        return accounts.map(this::convertToLoyaltyAccountResponseDto);
    }

    @Transactional(readOnly = true)
    public Page<com.MyBooking.loyalty.dto.LoyaltyAccountResponseDto> getAccountsWithHighBalanceAsDto(Integer threshold, Pageable pageable) {
        Page<LoyaltyAccount> accounts = getAccountsWithHighBalance(threshold, pageable);
        return accounts.map(this::convertToLoyaltyAccountResponseDto);
    }

    @Transactional(readOnly = true)
    public Page<com.MyBooking.loyalty.dto.LoyaltyAccountResponseDto> getAccountsWithZeroBalanceAsDto(Pageable pageable) {
        Page<LoyaltyAccount> accounts = getAccountsWithZeroBalance(pageable);
        return accounts.map(this::convertToLoyaltyAccountResponseDto);
    }

    // Loyalty Transaction DTO methods
    @Transactional
    public com.MyBooking.loyalty.dto.LoyaltyTransactionResponseDto earnPointsAsDto(Long userId, BigDecimal amount, String reason, Long reservationId) {
        LoyaltyTransaction transaction = earnPoints(userId, amount, reason, reservationId);
        return convertToLoyaltyTransactionResponseDto(transaction);
    }

    @Transactional
    public com.MyBooking.loyalty.dto.LoyaltyTransactionResponseDto redeemPointsAsDto(Long userId, Integer points, String reason) {
        LoyaltyTransaction transaction = redeemPoints(userId, points, reason);
        return convertToLoyaltyTransactionResponseDto(transaction);
    }

    @Transactional(readOnly = true)
    public Page<com.MyBooking.loyalty.dto.LoyaltyTransactionResponseDto> getTransactionsByUserIdAsDto(Long userId, Pageable pageable) {
        Page<LoyaltyTransaction> transactions = getTransactionHistory(userId, pageable);
        return transactions.map(this::convertToLoyaltyTransactionResponseDto);
    }

    @Transactional(readOnly = true)
    public Page<com.MyBooking.loyalty.dto.LoyaltyTransactionResponseDto> getAllTransactionsAsDto(Pageable pageable) {
        Page<LoyaltyTransaction> transactions = getAllTransactions(pageable);
        return transactions.map(this::convertToLoyaltyTransactionResponseDto);
    }

    @Transactional(readOnly = true)
    public Page<com.MyBooking.loyalty.dto.LoyaltyTransactionResponseDto> getHighValueTransactionsAsDto(Integer threshold, Pageable pageable) {
        Page<LoyaltyTransaction> transactions = getHighValueTransactions(threshold, pageable);
        return transactions.map(this::convertToLoyaltyTransactionResponseDto);
    }

    @Transactional(readOnly = true)
    public Page<com.MyBooking.loyalty.dto.LoyaltyTransactionResponseDto> getTransactionsByTypeAsDto(LoyaltyTxType type, Pageable pageable) {
        Page<LoyaltyTransaction> transactions = getTransactionsByType(type, pageable);
        return transactions.map(this::convertToLoyaltyTransactionResponseDto);
    }

    // Statistics DTO methods
    @Transactional(readOnly = true)
    public com.MyBooking.loyalty.dto.LoyaltyStatisticsDto getLoyaltyStatisticsAsDto() {
        LoyaltyStatistics stats = getLoyaltyStatistics();
        return convertToLoyaltyStatisticsDto(stats);
    }

    @Transactional(readOnly = true)
    public com.MyBooking.loyalty.dto.PointsCalculationDto calculatePointsAsDto(BigDecimal amount) {
        Integer calculatedPoints = calculatePointsFromAmount(amount);
        BigDecimal calculatedDiscountAmount = calculatePointsDiscountAmount(calculatedPoints);
        Integer maxRedeemablePoints = 10000; // Business rule: max 10,000 points per transaction
        
        return new com.MyBooking.loyalty.dto.PointsCalculationDto(
            calculatedPoints, amount, calculatedDiscountAmount, maxRedeemablePoints, "STANDARD");
    }

    // Utility method to get user ID by email (legacy helper - use UserPrincipal.getId() instead)
    @Deprecated(forRemoval = true)
    @Transactional(readOnly = true)
    public Long getUserIdByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + email));
        return user.getId();
    }

    // Conversion methods
    private com.MyBooking.loyalty.dto.LoyaltyAccountResponseDto convertToLoyaltyAccountResponseDto(LoyaltyAccount account) {
        com.MyBooking.loyalty.dto.LoyaltyAccountResponseDto dto = new com.MyBooking.loyalty.dto.LoyaltyAccountResponseDto();
        dto.setId(account.getId());
        dto.setUserId(account.getUser().getId());
        dto.setUserName(account.getUser().getFirstName() + " " + account.getUser().getLastName());
        dto.setUserEmail(account.getUser().getEmail());
        dto.setBalance(account.getBalance());
        dto.setCreatedAt(account.getCreatedAt());
        dto.setUpdatedAt(account.getUpdatedAt());
        return dto;
    }

    private com.MyBooking.loyalty.dto.LoyaltyTransactionResponseDto convertToLoyaltyTransactionResponseDto(LoyaltyTransaction transaction) {
        com.MyBooking.loyalty.dto.LoyaltyTransactionResponseDto dto = new com.MyBooking.loyalty.dto.LoyaltyTransactionResponseDto();
        dto.setId(transaction.getId());
        dto.setAccountId(transaction.getAccount().getId());
        dto.setUserId(transaction.getAccount().getUser().getId());
        dto.setUserName(transaction.getAccount().getUser().getFirstName() + " " + transaction.getAccount().getUser().getLastName());
        dto.setUserEmail(transaction.getAccount().getUser().getEmail());
        dto.setType(transaction.getType());
        dto.setPoints(transaction.getPoints());
        dto.setReservationId(transaction.getReservation() != null ? transaction.getReservation().getId() : null);
        dto.setCreatedAt(transaction.getCreatedAt());
        return dto;
    }

    private com.MyBooking.loyalty.dto.LoyaltyStatisticsDto convertToLoyaltyStatisticsDto(LoyaltyStatistics stats) {
        return new com.MyBooking.loyalty.dto.LoyaltyStatisticsDto(
            stats.getTotalAccounts(),
            stats.getTotalTransactions(),
            stats.getTotalBalance(),
            stats.getAverageBalance(),
            stats.getTotalEarnedPoints(),
            stats.getTotalRedeemedPoints()
        );
    }
}