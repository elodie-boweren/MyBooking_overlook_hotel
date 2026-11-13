package com.MyBooking.loyalty.controller;

import com.MyBooking.auth.domain.Role;
import com.MyBooking.common.security.UserPrincipal;
import com.MyBooking.loyalty.dto.*;
import com.MyBooking.loyalty.service.LoyaltyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Controller for loyalty management (Client access)
 */
@RestController
@RequestMapping("/api/loyalty")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    @Autowired
    public LoyaltyController(LoyaltyService loyaltyService) {
        this.loyaltyService = loyaltyService;
    }

    // ==================== CLIENT LOYALTY ENDPOINTS ====================

    /**
     * CLIENT: Get my loyalty account
     * Requires CLIENT role.
     */
    @GetMapping("/account")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<LoyaltyAccountResponseDto> getMyLoyaltyAccount(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal.getId();
        LoyaltyAccountResponseDto response = loyaltyService.getLoyaltyAccountByUserIdAsDto(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * CLIENT: Create my loyalty account
     * Requires CLIENT role.
     */
    @PostMapping("/account")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<LoyaltyAccountResponseDto> createMyLoyaltyAccount(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal.getId();
        LoyaltyAccountResponseDto response = loyaltyService.createLoyaltyAccountAsDto(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * CLIENT: Get my transaction history
     * Requires CLIENT role.
     */
    @GetMapping("/transactions")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Page<LoyaltyTransactionResponseDto>> getMyTransactions(
            @AuthenticationPrincipal UserPrincipal principal,
            Pageable pageable) {
        Long userId = principal.getId();
        Page<LoyaltyTransactionResponseDto> transactions = loyaltyService.getTransactionsByUserIdAsDto(userId, pageable);
        return ResponseEntity.ok(transactions);
    }

    /**
     * CLIENT: Redeem points
     * Requires CLIENT role.
     */
    @PostMapping("/redeem")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<LoyaltyTransactionResponseDto> redeemPoints(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RedeemPointsRequestDto request) {
        Long userId = principal.getId();
        // For client operations, ignore the userId in the request and use the authenticated user's ID
        LoyaltyTransactionResponseDto response = loyaltyService.redeemPointsAsDto(
                userId, request.getPoints(), request.getReason());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * CLIENT: Calculate points for an amount
     * Requires CLIENT role.
     */
    @GetMapping("/calculate-points")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<PointsCalculationDto> calculatePoints(
            @RequestParam BigDecimal amount) {
        PointsCalculationDto response = loyaltyService.calculatePointsAsDto(amount);
        return ResponseEntity.ok(response);
    }

    /**
     * ALL ROLES: Get loyalty account by ID
     */
    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasAnyRole('CLIENT', 'EMPLOYEE', 'ADMIN')")
    public ResponseEntity<LoyaltyAccountResponseDto> getLoyaltyAccountById(
            @PathVariable Long accountId,
            @AuthenticationPrincipal UserPrincipal principal) {
        LoyaltyAccountResponseDto response = loyaltyService.getLoyaltyAccountByIdAsDto(accountId);
        if (!Role.ADMIN.name().equals(principal.getRole()) && !Role.EMPLOYEE.name().equals(principal.getRole())
                && !response.getUserId().equals(principal.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Cannot access another user's loyalty account");
        }
        return ResponseEntity.ok(response);
    }

    /**
     * ALL ROLES: Get loyalty account by user ID
     */
    @GetMapping("/account/user/{userId}")
    @PreAuthorize("hasAnyRole('CLIENT', 'EMPLOYEE', 'ADMIN')")
    public ResponseEntity<LoyaltyAccountResponseDto> getLoyaltyAccountByUserId(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (!Role.ADMIN.name().equals(principal.getRole()) && !Role.EMPLOYEE.name().equals(principal.getRole())
                && !userId.equals(principal.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Cannot access another user's loyalty account");
        }
        LoyaltyAccountResponseDto response = loyaltyService.getLoyaltyAccountByUserIdAsDto(userId);
        return ResponseEntity.ok(response);
    }
}
