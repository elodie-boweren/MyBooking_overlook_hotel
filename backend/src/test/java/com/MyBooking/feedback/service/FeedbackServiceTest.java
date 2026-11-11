package com.MyBooking.feedback.service;

import com.MyBooking.feedback.domain.Feedback;
import com.MyBooking.feedback.domain.FeedbackReply;
import com.MyBooking.feedback.repository.FeedbackRepository;
import com.MyBooking.feedback.repository.FeedbackReplyRepository;
import com.MyBooking.auth.domain.Role;
import com.MyBooking.auth.domain.User;
import com.MyBooking.auth.repository.UserRepository;
import com.MyBooking.reservation.domain.Reservation;
import com.MyBooking.reservation.repository.ReservationRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;
    @Mock
    private FeedbackReplyRepository feedbackReplyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private FeedbackService feedbackService;

    private User testClient;
    private User testAdmin;
    private Reservation testReservation;
    private Feedback testFeedback;
    private FeedbackReply testReply;

    @BeforeEach
    void setUp() {
        testClient = new User("John", "Doe", "john.doe@example.com", "password", "1234567890", "Address 1", LocalDate.of(1990, 1, 1), Role.CLIENT);
        testClient.setId(1L);
        
        testAdmin = new User("Admin", "User", "admin@hotel.com", "password", "0987654321", "Admin Office", LocalDate.of(1985, 5, 15), Role.ADMIN);
        testAdmin.setId(2L);
        
        testReservation = new Reservation(LocalDate.now().minusDays(5), LocalDate.now().minusDays(1), 2, 
                java.math.BigDecimal.valueOf(150.00), "USD", com.MyBooking.reservation.domain.ReservationStatus.CONFIRMED, testClient, null);
        testReservation.setId(1L);
        
        testFeedback = new Feedback(testReservation, testClient, 4, "Great stay!");
        testFeedback.setId(1L);
        
        testReply = new FeedbackReply(testFeedback, testAdmin, "Thank you for your feedback!");
        testReply.setId(1L);
    }

    // ==================== FEEDBACK MANAGEMENT TESTS ====================

    @Test
    void createFeedback_WithValidData_ShouldCreateFeedback() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testClient));
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        when(feedbackRepository.existsByUserAndReservation(testClient, testReservation)).thenReturn(false);
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> {
            Feedback feedback = invocation.getArgument(0);
            feedback.setId(1L);
            return feedback;
        });

        // When
        Feedback result = feedbackService.createFeedback(1L, 1L, 4, "Great stay!");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRating()).isEqualTo(4);
        assertThat(result.getComment()).isEqualTo("Great stay!");
        assertThat(result.getUser()).isEqualTo(testClient);
        assertThat(result.getReservation()).isEqualTo(testReservation);

        verify(userRepository).findById(1L);
        verify(reservationRepository).findById(1L);
        verify(feedbackRepository).existsByUserAndReservation(testClient, testReservation);
        verify(feedbackRepository).save(any(Feedback.class));
    }

    @Test
    void createFeedback_WithInvalidUser_ShouldThrowNotFoundException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When / Then
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            feedbackService.createFeedback(999L, 1L, 4, "Great stay!"));

        assertThat(exception.getMessage()).isEqualTo("User not found with ID: 999");
        verify(userRepository).findById(999L);
        verify(feedbackRepository, never()).save(any(Feedback.class));
    }

    @Test
    void createFeedback_WithInvalidRating_ShouldThrowBusinessRuleException() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testClient));
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));

        // When / Then
        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
            feedbackService.createFeedback(1L, 1L, 6, "Great stay!"));

        assertThat(exception.getMessage()).isEqualTo("Rating must be between 1 and 5.");
        verify(feedbackRepository, never()).save(any(Feedback.class));
    }

    @Test
    void createFeedback_WithCancelledReservation_ShouldThrowBusinessRuleException() {
        // Given
        testReservation.setStatus(com.MyBooking.reservation.domain.ReservationStatus.CANCELLED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testClient));
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));

        // When / Then
        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
            feedbackService.createFeedback(1L, 1L, 5, "Cancelled stay"));

        assertThat(exception.getMessage()).contains("Cannot provide feedback for a cancelled reservation");
        verify(feedbackRepository, never()).save(any(Feedback.class));
    }

    @Test
    void createFeedback_WithNonClientUser_ShouldThrowBusinessRuleException() {
        // Given
        User employee = new User("Employee", "User", "employee@hotel.com", "password", "1111111111", "Employee Office", LocalDate.of(1990, 1, 1), Role.EMPLOYEE);
        employee.setId(3L);
        when(userRepository.findById(3L)).thenReturn(Optional.of(employee));
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));

        // When / Then
        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
            feedbackService.createFeedback(3L, 1L, 4, "Great stay!"));

        assertThat(exception.getMessage()).isEqualTo("Only clients can provide feedback.");
        verify(feedbackRepository, never()).save(any(Feedback.class));
    }

    @Test
    void createFeedback_WithExistingFeedback_ShouldThrowBusinessRuleException() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testClient));
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        when(feedbackRepository.existsByUserAndReservation(testClient, testReservation)).thenReturn(true);

        // When / Then
        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
            feedbackService.createFeedback(1L, 1L, 4, "Great stay!"));

        assertThat(exception.getMessage()).isEqualTo("Feedback already exists for this reservation.");
        verify(feedbackRepository, never()).save(any(Feedback.class));
    }

    @Test
    void updateFeedback_WithValidData_ShouldUpdateFeedback() {
        // Given
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(testFeedback));
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(testFeedback);

        // When
        Feedback result = feedbackService.updateFeedback(1L, 1L, 5, "Excellent stay!");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRating()).isEqualTo(5);
        assertThat(result.getComment()).isEqualTo("Excellent stay!");

        verify(feedbackRepository).findById(1L);
        verify(feedbackRepository).save(testFeedback);
    }

    @Test
    void updateFeedback_WithInvalidUser_ShouldThrowBusinessRuleException() {
        // Given
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(testFeedback));

        // When / Then
        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
            feedbackService.updateFeedback(1L, 999L, 5, "Excellent stay!"));

        assertThat(exception.getMessage()).isEqualTo("Only the feedback author can modify their feedback.");
        verify(feedbackRepository, never()).save(any(Feedback.class));
    }

    // ==================== FEEDBACK REPLY TESTS ====================

    @Test
    void addReply_WithValidData_ShouldAddReply() {
        // Given
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(testFeedback));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testAdmin));
        when(feedbackReplyRepository.save(any(FeedbackReply.class))).thenAnswer(invocation -> {
            FeedbackReply reply = invocation.getArgument(0);
            reply.setId(1L);
            return reply;
        });

        // When
        FeedbackReply result = feedbackService.addReply(1L, 2L, "Thank you for your feedback!");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMessage()).isEqualTo("Thank you for your feedback!");
        assertThat(result.getFeedback()).isEqualTo(testFeedback);
        assertThat(result.getAdminUser()).isEqualTo(testAdmin);

        verify(feedbackRepository).findById(1L);
        verify(userRepository).findById(2L);
        verify(feedbackReplyRepository).save(any(FeedbackReply.class));
    }

    @Test
    void addReply_WithNonAdminUser_ShouldThrowBusinessRuleException() {
        // Given
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(testFeedback));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testClient));

        // When / Then
        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
            feedbackService.addReply(1L, 1L, "Thank you for your feedback!"));

        assertThat(exception.getMessage()).isEqualTo("Only admin users can reply to feedback.");
        verify(feedbackReplyRepository, never()).save(any(FeedbackReply.class));
    }

    // ==================== STATISTICS TESTS ====================

    @Test
    void getOverallAverageRating_ShouldReturnAverage() {
        // Given
        when(feedbackRepository.getAverageRatingOverall()).thenReturn(4.2);

        // When
        Double result = feedbackService.getOverallAverageRating();

        // Then
        assertThat(result).isEqualTo(4.2);
        verify(feedbackRepository).getAverageRatingOverall();
    }

    @Test
    void getRatingDistribution_ShouldReturnDistribution() {
        // Given
        List<Object[]> distribution = Arrays.asList(
            new Object[]{1, 5L},
            new Object[]{2, 3L},
            new Object[]{3, 8L},
            new Object[]{4, 15L},
            new Object[]{5, 12L}
        );
        when(feedbackRepository.getRatingDistribution()).thenReturn(distribution);

        // When
        List<Object[]> result = feedbackService.getRatingDistribution();

        // Then
        assertThat(result).hasSize(5);
        assertThat(result).isEqualTo(distribution);
        verify(feedbackRepository).getRatingDistribution();
    }

    // ==================== QUERY TESTS ====================

    @Test
    void getFeedbacksByUser_WithValidUser_ShouldReturnFeedbacks() {
        // Given
        List<Feedback> feedbacks = Arrays.asList(testFeedback);
        Page<Feedback> page = new PageImpl<>(feedbacks);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testClient));
        when(feedbackRepository.findByUser(eq(testClient), any(Pageable.class))).thenReturn(page);

        // When
        Page<Feedback> result = feedbackService.getFeedbacksByUser(1L, PageRequest.of(0, 10));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(testFeedback);

        verify(userRepository).findById(1L);
        verify(feedbackRepository).findByUser(eq(testClient), any(Pageable.class));
    }

    @Test
    void getHighRatedFeedbacks_ShouldReturnHighRatedFeedbacks() {
        // Given
        List<Feedback> highRatedFeedbacks = Arrays.asList(testFeedback);
        when(feedbackRepository.findHighRatedFeedbacks()).thenReturn(highRatedFeedbacks);

        // When
        List<Feedback> result = feedbackService.getHighRatedFeedbacks();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testFeedback);
        verify(feedbackRepository).findHighRatedFeedbacks();
    }
}
