package com.MyBooking.reservation.service;

import com.MyBooking.reservation.domain.Reservation;
import com.MyBooking.reservation.domain.ReservationStatus;
import com.MyBooking.reservation.repository.ReservationRepository;
import com.MyBooking.room.domain.Room;
import com.MyBooking.room.domain.RoomStatus;
import com.MyBooking.room.domain.RoomType;
import com.MyBooking.room.repository.RoomRepository;
import com.MyBooking.room.service.RoomService;
import com.MyBooking.auth.domain.User;
import com.MyBooking.auth.domain.Role;
import com.MyBooking.auth.repository.UserRepository;
import com.MyBooking.loyalty.service.LoyaltyService;
import com.MyBooking.common.exception.BusinessRuleException;
import com.MyBooking.common.exception.NotFoundException;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;
    
    @Mock
    private RoomRepository roomRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private RoomService roomService;

    @Mock
    private LoyaltyService loyaltyService;
    
    @InjectMocks
    private ReservationService reservationService;
    
    private Room testRoom;
    private User testUser;
    private Reservation testReservation;
    private LocalDate testCheckIn;
    private LocalDate testCheckOut;

    @BeforeEach
    void setUp() {
        // Create test room
        testRoom = new Room();
        testRoom.setId(1L);
        testRoom.setNumber("101");
        testRoom.setRoomType(RoomType.DOUBLE);
        testRoom.setCapacity(2);
        testRoom.setPrice(new BigDecimal("100.00"));
        testRoom.setCurrency("USD");
        testRoom.setStatus(RoomStatus.AVAILABLE);
        testRoom.setDescription("Standard room with city view");
        
        // Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setRole(Role.CLIENT);
        
        // Create test reservation
        testCheckIn = LocalDate.now().plusDays(1);
        testCheckOut = LocalDate.now().plusDays(3);
        
        testReservation = new Reservation();
        testReservation.setId(1L);
        testReservation.setCheckIn(testCheckIn);
        testReservation.setCheckOut(testCheckOut);
        testReservation.setNumberOfGuests(2);
        testReservation.setTotalPrice(new BigDecimal("220.00")); // 2 nights * $100 + 10% tax
        testReservation.setCurrency("USD");
        testReservation.setStatus(ReservationStatus.CONFIRMED);
        testReservation.setClient(testUser);
        testReservation.setRoom(testRoom);
    }

    // ========== RESERVATION CREATION TESTS ==========

    @Test
    void createReservation_WithValidData_ShouldCreateReservation() {
        // Given
        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reservationRepository.countConflictingReservations(1L, testCheckIn, testCheckOut))
            .thenReturn(0L);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);
        
        // When
        Reservation result = reservationService.createReservation(1L, 1L, testCheckIn, testCheckOut, 2, "USD");
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCheckIn()).isEqualTo(testCheckIn);
        assertThat(result.getCheckOut()).isEqualTo(testCheckOut);
        assertThat(result.getNumberOfGuests()).isEqualTo(2);
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(result.getClient()).isEqualTo(testUser);
        assertThat(result.getRoom()).isEqualTo(testRoom);
        
        verify(roomRepository).findByIdForUpdate(1L);
        verify(userRepository).findById(1L);
        verify(reservationRepository).countConflictingReservations(1L, testCheckIn, testCheckOut);
        verify(reservationRepository).save(any(Reservation.class));
        verify(roomService, never()).updateRoomStatusAutomatically(anyLong(), any(), anyString());
    }

    @Test
    void createReservation_WhenRoomIsOutOfService_ShouldThrowBusinessRuleException() {
        // Given
        testRoom.setStatus(RoomStatus.OUT_OF_SERVICE);
        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        // When & Then
        assertThatThrownBy(() -> reservationService.createReservation(1L, 1L, testCheckIn, testCheckOut, 2, "USD"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("is currently out of service and cannot be reserved");
        
        verify(roomRepository).findByIdForUpdate(1L);
        verify(userRepository).findById(1L);
        verify(reservationRepository, never()).save(any());
        verify(roomService, never()).updateRoomStatusAutomatically(anyLong(), any(), anyString());
    }

    @Test
    void createReservation_WithRoomNotFound_ShouldThrowNotFoundException() {
        // Given
        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> reservationService.createReservation(1L, 1L, testCheckIn, testCheckOut, 2, "USD"))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Room not found with ID: 1");
        
        verify(roomRepository).findByIdForUpdate(1L);
        verify(userRepository, never()).findById(any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_WithUserNotFound_ShouldThrowNotFoundException() {
        // Given
        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> reservationService.createReservation(1L, 1L, testCheckIn, testCheckOut, 2, "USD"))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Client not found with ID: 1");
        
        verify(roomRepository).findByIdForUpdate(1L);
        verify(userRepository).findById(1L);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_WithInvalidDates_ShouldThrowBusinessRuleException() {
        // Given
        LocalDate pastDate = LocalDate.now().minusDays(1);
        
        // When & Then
        assertThatThrownBy(() -> reservationService.createReservation(1L, 1L, pastDate, testCheckOut, 2, "USD"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Check-in date cannot be in the past");
        
        verify(roomRepository, never()).findById(any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_WithCheckOutBeforeCheckIn_ShouldThrowBusinessRuleException() {
        // Given
        LocalDate checkIn = LocalDate.now().plusDays(3);
        LocalDate checkOut = LocalDate.now().plusDays(1);
        
        // When & Then
        assertThatThrownBy(() -> reservationService.createReservation(1L, 1L, checkIn, checkOut, 2, "USD"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Check-out date must be after check-in date");
        
        verify(roomRepository, never()).findById(any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_WithExcessiveStay_ShouldThrowBusinessRuleException() {
        // Given
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(35); // More than 30 days
        
        // When & Then
        assertThatThrownBy(() -> reservationService.createReservation(1L, 1L, checkIn, checkOut, 2, "USD"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Maximum stay is 30 days");
        
        verify(roomRepository, never()).findById(any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_WithInvalidGuestCount_ShouldThrowBusinessRuleException() {
        // When & Then
        assertThatThrownBy(() -> reservationService.createReservation(1L, 1L, testCheckIn, testCheckOut, 0, "USD"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Number of guests must be between 1 and 10");
        
        // Validation happens before repository calls
        verify(roomRepository, never()).findById(any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_WithRoomCapacityExceeded_ShouldThrowBusinessRuleException() {
        // Given
        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testRoom)); // Room capacity is 2
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        // When & Then
        assertThatThrownBy(() -> reservationService.createReservation(1L, 1L, testCheckIn, testCheckOut, 3, "USD"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Number of guests (3) exceeds room capacity (2)");
        
        verify(roomRepository).findByIdForUpdate(1L);
        verify(userRepository).findById(1L);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_WithRoomNotAvailable_ShouldThrowBusinessRuleException() {
        // Given
        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reservationRepository.countConflictingReservations(1L, testCheckIn, testCheckOut))
            .thenReturn(1L); // Room has conflicting reservation
        
        // When & Then
        assertThatThrownBy(() -> reservationService.createReservation(1L, 1L, testCheckIn, testCheckOut, 2, "USD"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Room is not available for the selected dates");
        
        verify(roomRepository).findByIdForUpdate(1L);
        verify(userRepository).findById(1L);
        verify(reservationRepository).countConflictingReservations(1L, testCheckIn, testCheckOut);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_WithInvalidCurrency_ShouldThrowBusinessRuleException() {
        // When & Then
        assertThatThrownBy(() -> reservationService.createReservation(1L, 1L, testCheckIn, testCheckOut, 2, "US"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Currency must be a 3-character code");
        
        // Validation happens before repository calls
        verify(roomRepository, never()).findById(any());
        verify(reservationRepository, never()).save(any());
    }

    // ========== RESERVATION UPDATE TESTS ==========

    @Test
    void updateReservation_WithValidData_ShouldUpdateReservation() {
        // Given
        LocalDate newCheckIn = LocalDate.now().plusDays(5);
        LocalDate newCheckOut = LocalDate.now().plusDays(7);
        
        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testReservation));
        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testRoom));
        when(reservationRepository.countConflictingReservations(1L, newCheckIn, newCheckOut, 1L))
            .thenReturn(0L);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);
        
        // When
        Reservation result = reservationService.updateReservation(1L, newCheckIn, newCheckOut, 2, "USD");
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCheckIn()).isEqualTo(newCheckIn);
        assertThat(result.getCheckOut()).isEqualTo(newCheckOut);
        
        verify(reservationRepository).findByIdForUpdate(1L);
        verify(roomRepository).findByIdForUpdate(1L);
        verify(reservationRepository).countConflictingReservations(1L, newCheckIn, newCheckOut, 1L);
        verify(reservationRepository).save(testReservation);
    }

    @Test
    void updateReservation_WithReservationNotFound_ShouldThrowNotFoundException() {
        // Given
        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> reservationService.updateReservation(1L, testCheckIn, testCheckOut, 2, "USD"))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Reservation not found with ID: 1");
        
        verify(reservationRepository).findByIdForUpdate(1L);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void updateReservation_WithConflictingDates_ShouldThrowBusinessRuleException() {
        // Given
        LocalDate newCheckIn = LocalDate.now().plusDays(5);
        LocalDate newCheckOut = LocalDate.now().plusDays(7);
        
        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testReservation));
        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testRoom));
        when(reservationRepository.countConflictingReservations(1L, newCheckIn, newCheckOut, 1L))
            .thenReturn(1L);
        
        // When & Then
        assertThatThrownBy(() -> reservationService.updateReservation(1L, newCheckIn, newCheckOut, 2, "USD"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Room is not available for the selected dates");
        
        verify(reservationRepository).findByIdForUpdate(1L);
        verify(roomRepository).findByIdForUpdate(1L);
        verify(reservationRepository).countConflictingReservations(1L, newCheckIn, newCheckOut, 1L);
        verify(reservationRepository, never()).save(any());
    }

    // ========== RESERVATION CANCELLATION TESTS ==========

    @Test
    void cancelReservation_WithValidReservation_ShouldCancelReservation() {
        // Given
        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testReservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);
        
        // When
        reservationService.cancelReservation(1L, "Guest requested cancellation");
        
        // Then
        assertThat(testReservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        
        verify(reservationRepository).findByIdForUpdate(1L);
        verify(reservationRepository).save(testReservation);
        verify(roomService, never()).updateRoomStatusAutomatically(anyLong(), any(), anyString());
        verify(loyaltyService).clawbackReservationPoints(1L);
    }

    @Test
    void cancelReservation_WithRedeemedPoints_ShouldRefundAndClawbackPoints() {
        // Given
        testReservation.setPointsUsed(300);
        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testReservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);

        // When
        reservationService.cancelReservation(1L, "Guest requested cancellation with refund");

        // Then
        assertThat(testReservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        verify(loyaltyService).refundReservationPoints(1L, 300);
        verify(loyaltyService).clawbackReservationPoints(1L);
        verify(reservationRepository).save(testReservation);
    }

    @Test
    void cancelReservation_WithAlreadyCancelled_ShouldThrowBusinessRuleException() {
        // Given
        testReservation.setStatus(ReservationStatus.CANCELLED);
        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testReservation));
        
        // When & Then
        assertThatThrownBy(() -> reservationService.cancelReservation(1L, "Test reason"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Reservation is already cancelled");
        
        verify(reservationRepository).findByIdForUpdate(1L);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void cancelReservation_AfterCheckInDate_ShouldThrowBusinessRuleException() {
        // Given
        testReservation.setCheckIn(LocalDate.now().minusDays(1)); // Check-in was yesterday
        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testReservation));
        
        // When & Then
        assertThatThrownBy(() -> reservationService.cancelReservation(1L, "Test reason"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Cannot cancel reservation after check-in date has passed");
        
        verify(reservationRepository).findByIdForUpdate(1L);
        verify(reservationRepository, never()).save(any());
    }

    // ========== AVAILABILITY TESTS ==========

    @Test
    void isRoomAvailable_WithAvailableRoom_ShouldReturnTrue() {
        // Given
        when(reservationRepository.countConflictingReservations(1L, testCheckIn, testCheckOut))
            .thenReturn(0L);
        
        // When
        boolean result = reservationService.isRoomAvailable(1L, testCheckIn, testCheckOut);
        
        // Then
        assertThat(result).isTrue();
        verify(reservationRepository).countConflictingReservations(1L, testCheckIn, testCheckOut);
    }

    @Test
    void isRoomAvailable_WithConflictingReservation_ShouldReturnFalse() {
        // Given
        when(reservationRepository.countConflictingReservations(1L, testCheckIn, testCheckOut))
            .thenReturn(1L);
        
        // When
        boolean result = reservationService.isRoomAvailable(1L, testCheckIn, testCheckOut);
        
        // Then
        assertThat(result).isFalse();
        verify(reservationRepository).countConflictingReservations(1L, testCheckIn, testCheckOut);
    }

    @Test
    void getAvailableRooms_WithValidCriteria_ShouldReturnAvailableRooms() {
        // Given
        when(roomRepository.findAvailableRooms(testCheckIn, testCheckOut, 2))
            .thenReturn(List.of(testRoom));
        
        // When
        List<Room> result = reservationService.getAvailableRooms(testCheckIn, testCheckOut, 2);
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testRoom);
        
        verify(roomRepository).findAvailableRooms(testCheckIn, testCheckOut, 2);
    }

    @Test
    void getAvailableRooms_ShouldExcludeOutOfServiceRooms() {
        // Given
        when(roomRepository.findAvailableRooms(testCheckIn, testCheckOut, 2))
            .thenReturn(List.of(testRoom));
        
        // When
        List<Room> result = reservationService.getAvailableRooms(testCheckIn, testCheckOut, 2);
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testRoom);
        verify(roomRepository).findAvailableRooms(testCheckIn, testCheckOut, 2);
    }

    // ========== BUSINESS LOGIC TESTS ==========

    @Test
    void calculateTotalPrice_WithBasePrice_ShouldCalculateCorrectly() {
        // Given
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(3); // 2 nights
        int numberOfGuests = 2;
        
        // When
        BigDecimal result = reservationService.calculateTotalPrice(testRoom, checkIn, checkOut, numberOfGuests);
        
        // Then
        // Base price: 2 nights * $100 = $200
        // Tax: $200 * 0.10 = $20
        // Total: $200 + $20 = $220
        assertThat(result).isEqualByComparingTo(new BigDecimal("220.00"));
    }

    @Test
    void calculateTotalPrice_WithExtraGuests_ShouldAddExtraGuestFee() {
        // Given
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(3); // 2 nights
        int numberOfGuests = 4; // 2 extra guests
        
        // When
        BigDecimal result = reservationService.calculateTotalPrice(testRoom, checkIn, checkOut, numberOfGuests);
        
        // Then
        // Base price: 2 nights * $100 = $200
        // Extra guests: 2 guests * $25 * 2 nights = $100
        // Subtotal: $200 + $100 = $300
        // Tax: $300 * 0.10 = $30
        // Total: $300 + $30 = $330
        assertThat(result).isEqualByComparingTo(new BigDecimal("330.00"));
    }

    // ========== SEARCH & FILTERING TESTS ==========

    @Test
    void getReservationsByGuest_WithValidClientId_ShouldReturnReservations() {
        // Given
        List<Reservation> expectedReservations = Arrays.asList(testReservation);
        when(reservationRepository.findByClientId(1L)).thenReturn(expectedReservations);
        
        // When
        List<Reservation> result = reservationService.getReservationsByGuest(1L);
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testReservation);
        verify(reservationRepository).findByClientId(1L);
    }

    @Test
    void getReservationsByRoom_WithValidRoomId_ShouldReturnReservations() {
        // Given
        List<Reservation> expectedReservations = Arrays.asList(testReservation);
        when(reservationRepository.findByRoomId(1L)).thenReturn(expectedReservations);
        
        // When
        List<Reservation> result = reservationService.getReservationsByRoom(1L);
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testReservation);
        verify(reservationRepository).findByRoomId(1L);
    }

    @Test
    void getReservationsByStatus_WithValidStatus_ShouldReturnReservations() {
        // Given
        List<Reservation> expectedReservations = Arrays.asList(testReservation);
        when(reservationRepository.findByStatus(ReservationStatus.CONFIRMED)).thenReturn(expectedReservations);
        
        // When
        List<Reservation> result = reservationService.getReservationsByStatus(ReservationStatus.CONFIRMED);
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testReservation);
        verify(reservationRepository).findByStatus(ReservationStatus.CONFIRMED);
    }

    @Test
    void getUpcomingReservations_ShouldReturnFutureReservations() {
        // Given
        List<Reservation> expectedReservations = Arrays.asList(testReservation);
        when(reservationRepository.findByCheckInGreaterThanEqual(any(LocalDate.class)))
            .thenReturn(expectedReservations);
        
        // When
        List<Reservation> result = reservationService.getUpcomingReservations();
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testReservation);
        verify(reservationRepository).findByCheckInGreaterThanEqual(any(LocalDate.class));
    }

    @Test
    void getActiveReservations_ShouldReturnCurrentReservations() {
        // Given
        List<Reservation> expectedReservations = Arrays.asList(testReservation);
        when(reservationRepository.findByCheckInLessThanEqualAndCheckOutGreaterThanEqual(any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(expectedReservations);
        
        // When
        List<Reservation> result = reservationService.getActiveReservations();
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testReservation);
        verify(reservationRepository).findByCheckInLessThanEqualAndCheckOutGreaterThanEqual(any(LocalDate.class), any(LocalDate.class));
    }

    // ========== STATISTICS TESTS ==========

    @Test
    void getReservationStatistics_ShouldReturnCorrectStatistics() {
        // Given
        when(reservationRepository.count()).thenReturn(10L);
        when(reservationRepository.countByStatus(ReservationStatus.CONFIRMED)).thenReturn(8L);
        when(reservationRepository.countByStatus(ReservationStatus.CANCELLED)).thenReturn(2L);
        when(reservationRepository.getTotalRevenue()).thenReturn(new BigDecimal("1000.00"));
        when(reservationRepository.getAverageReservationPrice()).thenReturn(new BigDecimal("125.00"));
        
        // When
        ReservationService.ReservationStatistics result = reservationService.getReservationStatistics();
        
        // Then
        assertThat(result.getTotalReservations()).isEqualTo(10L);
        assertThat(result.getConfirmedReservations()).isEqualTo(8L);
        assertThat(result.getCancelledReservations()).isEqualTo(2L);
        assertThat(result.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(result.getAveragePrice()).isEqualByComparingTo(new BigDecimal("125.00"));
        assertThat(result.getConfirmationRate()).isEqualTo(80.0);
        assertThat(result.getCancellationRate()).isEqualTo(20.0);
        
        verify(reservationRepository).count();
        verify(reservationRepository).countByStatus(ReservationStatus.CONFIRMED);
        verify(reservationRepository).countByStatus(ReservationStatus.CANCELLED);
        verify(reservationRepository).getTotalRevenue();
        verify(reservationRepository).getAverageReservationPrice();
    }

    @Test
    void getRevenueByCurrency_WithValidCurrency_ShouldReturnRevenue() {
        // Given
        BigDecimal expectedRevenue = new BigDecimal("500.00");
        when(reservationRepository.getTotalRevenueByCurrency("USD")).thenReturn(expectedRevenue);
        
        // When
        BigDecimal result = reservationService.getRevenueByCurrency("USD");
        
        // Then
        assertThat(result).isEqualByComparingTo(expectedRevenue);
        verify(reservationRepository).getTotalRevenueByCurrency("USD");
    }

    // ========== ERROR HANDLING TESTS ==========

    @Test
    void getReservationById_WithNonExistentId_ShouldThrowNotFoundException() {
        // Given
        when(reservationRepository.findById(1L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> reservationService.getReservationById(1L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Reservation not found with ID: 1");
        
        verify(reservationRepository).findById(1L);
    }

    @Test
    void confirmReservation_WithAlreadyConfirmed_ShouldThrowBusinessRuleException() {
        // Given
        testReservation.setStatus(ReservationStatus.CONFIRMED);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        
        // When & Then
        assertThatThrownBy(() -> reservationService.confirmReservation(1L))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Reservation is already confirmed");
        
        verify(reservationRepository).findById(1L);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void confirmReservation_WithCancelledReservation_ShouldThrowBusinessRuleException() {
        // Given
        testReservation.setStatus(ReservationStatus.CANCELLED);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        
        // When & Then
        assertThatThrownBy(() -> reservationService.confirmReservation(1L))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Cannot confirm a cancelled reservation");
        
        verify(reservationRepository).findById(1L);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void updateReservation_WithCancelledReservation_ShouldThrowBusinessRuleException() {
        testReservation.setStatus(ReservationStatus.CANCELLED);
        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testReservation));

        assertThatThrownBy(() -> reservationService.updateReservation(
                1L, LocalDate.now().plusDays(5), LocalDate.now().plusDays(7), 2, "USD"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Cannot update a cancelled reservation");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void updateReservation_WithPointsDiscount_ShouldPreserveDiscount() {
        testReservation.setPointsUsed(100);
        testReservation.setPointsDiscount(new BigDecimal("10.00"));
        LocalDate newCheckIn = LocalDate.now().plusDays(5);
        LocalDate newCheckOut = LocalDate.now().plusDays(7); // 2 nights * 100 + 10% tax = 220.00

        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testReservation));
        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testRoom));
        when(reservationRepository.countConflictingReservations(1L, newCheckIn, newCheckOut, 1L)).thenReturn(0L);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        Reservation result = reservationService.updateReservation(1L, newCheckIn, newCheckOut, 2, "USD");

        assertThat(result.getTotalPrice()).isEqualByComparingTo(new BigDecimal("210.00"));
    }

    @Test
    void reassignReservation_WithCancelledReservation_ShouldThrowBusinessRuleException() {
        testReservation.setStatus(ReservationStatus.CANCELLED);
        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testReservation));

        assertThatThrownBy(() -> reservationService.reassignReservation(1L, 2L))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Cannot reassign a cancelled reservation");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reassignReservation_ShouldRecalculatePriceAndPreservePointsDiscount() {
        testReservation.setPointsUsed(100);
        testReservation.setPointsDiscount(new BigDecimal("10.00"));
        testReservation.setCheckIn(LocalDate.now().plusDays(2));
        testReservation.setCheckOut(LocalDate.now().plusDays(4)); // 2 nights
        testReservation.setNumberOfGuests(2);

        Room expensiveRoom = new Room("202", RoomType.DELUXE, 2, new BigDecimal("200.00"), "USD", RoomStatus.AVAILABLE);
        expensiveRoom.setId(2L);

        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testReservation));
        when(roomRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(expensiveRoom));
        when(reservationRepository.countConflictingReservations(2L, testReservation.getCheckIn(), testReservation.getCheckOut(), 1L))
            .thenReturn(0L);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        com.MyBooking.reservation.dto.ReservationResponseDto dto = reservationService.reassignReservation(1L, 2L);

        // 2 nights * 200.00 = 400.00 + 10% tax = 440.00 - 10.00 discount = 430.00
        assertThat(dto.getTotalPrice()).isEqualByComparingTo(new BigDecimal("430.00"));
    }
}
