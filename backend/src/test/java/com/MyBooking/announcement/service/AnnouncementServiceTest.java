package com.MyBooking.announcement.service;

import com.MyBooking.announcement.domain.*;
import com.MyBooking.announcement.repository.AnnouncementRepository;
import com.MyBooking.announcement.repository.AnnouncementReplyRepository;
import com.MyBooking.auth.domain.Role;
import com.MyBooking.auth.domain.User;
import com.MyBooking.auth.repository.UserRepository;
import com.MyBooking.common.exception.BusinessRuleException;
import com.MyBooking.common.exception.NotFoundException;
import com.MyBooking.employee.domain.Department;
import com.MyBooking.employee.domain.Employee;
import com.MyBooking.employee.domain.EmployeeStatus;
import com.MyBooking.employee.repository.EmployeeRepository;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private AnnouncementReplyRepository announcementReplyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private AnnouncementService announcementService;

    private User testUser;
    private User testUser2;
    private Announcement testAnnouncement;
    private Announcement testArchivedAnnouncement;
    private AnnouncementReply testReply;

    @BeforeEach
    void setUp() {
        // Create test users
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("admin@hotel.com");
        testUser.setFirstName("Admin");
        testUser.setLastName("User");

        testUser2 = new User();
        testUser2.setId(2L);
        testUser2.setEmail("employee@hotel.com");
        testUser2.setFirstName("Employee");
        testUser2.setLastName("User");

        // Create test announcement
        testAnnouncement = new Announcement();
        testAnnouncement.setId(1L);
        testAnnouncement.setTitle("Test Announcement");
        testAnnouncement.setContent("This is a test announcement");
        testAnnouncement.setCreatedBy(testUser);
        testAnnouncement.setPriority(AnnouncementPriority.HIGH);
        testAnnouncement.setStatus(AnnouncementStatus.PUBLISHED);
        testAnnouncement.setCreatedAt(LocalDateTime.now().minusDays(1));
        testAnnouncement.setUpdatedAt(LocalDateTime.now().minusDays(1));

        // Create test archived announcement
        testArchivedAnnouncement = new Announcement();
        testArchivedAnnouncement.setId(2L);
        testArchivedAnnouncement.setTitle("Archived Announcement");
        testArchivedAnnouncement.setContent("This is an archived announcement");
        testArchivedAnnouncement.setCreatedBy(testUser);
        testArchivedAnnouncement.setPriority(AnnouncementPriority.MEDIUM);
        testArchivedAnnouncement.setStatus(AnnouncementStatus.ARCHIVED);
        testArchivedAnnouncement.setCreatedAt(LocalDateTime.now().minusDays(2));
        testArchivedAnnouncement.setUpdatedAt(LocalDateTime.now().minusDays(2));

        // Create test reply
        testReply = new AnnouncementReply();
        testReply.setId(1L);
        testReply.setAnnouncement(testAnnouncement);
        testReply.setUser(testUser2);
        testReply.setMessage("This is a test reply");
        testReply.setCreatedAt(LocalDateTime.now());
        testReply.setUpdatedAt(LocalDateTime.now());
    }

    // ==================== ANNOUNCEMENT MANAGEMENT TESTS ====================

    @Test
    void createAnnouncement_WithValidData_ShouldCreatePublishedAnnouncement() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(announcementRepository.save(any(Announcement.class))).thenAnswer(invocation -> {
            Announcement announcement = invocation.getArgument(0);
            announcement.setId(1L);
            return announcement;
        });

        // When
        Announcement result = announcementService.createAnnouncement(
            1L, "Test Title", "Test Content", AnnouncementPriority.HIGH);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test Title");
        assertThat(result.getContent()).isEqualTo("Test Content");
        assertThat(result.getPriority()).isEqualTo(AnnouncementPriority.HIGH);
        assertThat(result.getStatus()).isEqualTo(AnnouncementStatus.PUBLISHED);
        assertThat(result.getCreatedBy()).isEqualTo(testUser);

        verify(userRepository).findById(1L);
        verify(announcementRepository).save(any(Announcement.class));
    }

    @Test
    void createAnnouncement_WithInvalidUser_ShouldThrowNotFoundException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> announcementService.createAnnouncement(
            999L, "Test Title", "Test Content", AnnouncementPriority.HIGH))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("User not found with ID: 999");

        verify(userRepository).findById(999L);
        verify(announcementRepository, never()).save(any());
    }

    @Test
    void updateAnnouncement_WithValidData_ShouldUpdateAnnouncement() {
        // Given
        when(announcementRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testAnnouncement));
        when(announcementRepository.save(any(Announcement.class))).thenReturn(testAnnouncement);

        // When
        Announcement result = announcementService.updateAnnouncement(
            1L, "Updated Title", "Updated Content", AnnouncementPriority.MEDIUM);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Updated Title");
        assertThat(result.getContent()).isEqualTo("Updated Content");
        assertThat(result.getPriority()).isEqualTo(AnnouncementPriority.MEDIUM);

        verify(announcementRepository).findByIdForUpdate(1L);
        verify(announcementRepository).save(testAnnouncement);
    }

    @Test
    void updateAnnouncement_WithArchivedAnnouncement_ShouldThrowBusinessRuleException() {
        // Given
        when(announcementRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(testArchivedAnnouncement));

        // When & Then
        assertThatThrownBy(() -> announcementService.updateAnnouncement(
            2L, "Updated Title", "Updated Content", AnnouncementPriority.MEDIUM))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Cannot update archived announcement");

        verify(announcementRepository).findByIdForUpdate(2L);
        verify(announcementRepository, never()).save(any());
    }

    @Test
    void updateAnnouncement_WithNonExistentAnnouncement_ShouldThrowNotFoundException() {
        // Given
        when(announcementRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> announcementService.updateAnnouncement(
            999L, "Updated Title", "Updated Content", AnnouncementPriority.MEDIUM))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Announcement not found with ID: 999");

        verify(announcementRepository).findByIdForUpdate(999L);
        verify(announcementRepository, never()).save(any());
    }

    @Test
    void archiveAnnouncement_WithPublishedAnnouncement_ShouldArchiveAnnouncement() {
        // Given
        when(announcementRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testAnnouncement));
        when(announcementRepository.save(any(Announcement.class))).thenReturn(testAnnouncement);

        // When
        Announcement result = announcementService.archiveAnnouncement(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(AnnouncementStatus.ARCHIVED);

        verify(announcementRepository).findByIdForUpdate(1L);
        verify(announcementRepository).save(testAnnouncement);
    }

    @Test
    void archiveAnnouncement_WithAlreadyArchivedAnnouncement_ShouldThrowBusinessRuleException() {
        // Given
        when(announcementRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(testArchivedAnnouncement));

        // When & Then
        assertThatThrownBy(() -> announcementService.archiveAnnouncement(2L))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Announcement is already archived");

        verify(announcementRepository).findByIdForUpdate(2L);
        verify(announcementRepository, never()).save(any());
    }

    @Test
    void unarchiveAnnouncement_WithArchivedAnnouncement_ShouldUnarchiveAnnouncement() {
        // Given
        when(announcementRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(testArchivedAnnouncement));
        when(announcementRepository.save(any(Announcement.class))).thenReturn(testArchivedAnnouncement);

        // When
        Announcement result = announcementService.unarchiveAnnouncement(2L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(AnnouncementStatus.PUBLISHED);

        verify(announcementRepository).findByIdForUpdate(2L);
        verify(announcementRepository).save(testArchivedAnnouncement);
    }

    @Test
    void unarchiveAnnouncement_WithPublishedAnnouncement_ShouldThrowBusinessRuleException() {
        // Given
        when(announcementRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testAnnouncement));

        // When & Then
        assertThatThrownBy(() -> announcementService.unarchiveAnnouncement(1L))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Only archived announcements can be unarchived");

        verify(announcementRepository).findByIdForUpdate(1L);
        verify(announcementRepository, never()).save(any());
    }

    @Test
    void deleteAnnouncement_WithNoReplies_ShouldDeleteAnnouncement() {
        // Given
        when(announcementRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testAnnouncement));
        when(announcementReplyRepository.existsByAnnouncement(testAnnouncement)).thenReturn(false);

        // When
        announcementService.deleteAnnouncement(1L);

        // Then
        verify(announcementRepository).findByIdForUpdate(1L);
        verify(announcementReplyRepository).existsByAnnouncement(testAnnouncement);
        verify(announcementRepository).delete(testAnnouncement);
    }

    @Test
    void deleteAnnouncement_WithReplies_ShouldThrowBusinessRuleException() {
        // Given
        when(announcementRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testAnnouncement));
        when(announcementReplyRepository.existsByAnnouncement(testAnnouncement)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> announcementService.deleteAnnouncement(1L))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Cannot delete announcement with replies");

        verify(announcementRepository).findByIdForUpdate(1L);
        verify(announcementReplyRepository).existsByAnnouncement(testAnnouncement);
        verify(announcementRepository, never()).delete(any());
    }

    @Test
    void getAnnouncementById_WithValidId_ShouldReturnAnnouncement() {
        // Given
        when(announcementRepository.findById(1L)).thenReturn(Optional.of(testAnnouncement));

        // When
        Announcement result = announcementService.getAnnouncementById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Test Announcement");

        verify(announcementRepository).findById(1L);
    }

    @Test
    void getAnnouncementById_WithInvalidId_ShouldThrowNotFoundException() {
        // Given
        when(announcementRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> announcementService.getAnnouncementById(999L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Announcement not found with ID: 999");

        verify(announcementRepository).findById(999L);
    }

    // ==================== REPLY MANAGEMENT TESTS ====================

    @Test
    void addReply_WithValidData_ShouldAddReply() {
        // Given
        when(announcementRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testAnnouncement));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser2));
        when(announcementReplyRepository.save(any(AnnouncementReply.class))).thenAnswer(invocation -> {
            AnnouncementReply reply = invocation.getArgument(0);
            reply.setId(1L);
            return reply;
        });

        // When
        AnnouncementReply result = announcementService.addReply(1L, 2L, "Test reply message");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMessage()).isEqualTo("Test reply message");
        assertThat(result.getAnnouncement()).isEqualTo(testAnnouncement);
        assertThat(result.getUser()).isEqualTo(testUser2);

        verify(announcementRepository).findByIdForUpdate(1L);
        verify(userRepository).findById(2L);
        verify(announcementReplyRepository).save(any(AnnouncementReply.class));
    }

    @Test
    void addReply_WithArchivedAnnouncement_ShouldThrowBusinessRuleException() {
        // Given
        when(announcementRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(testArchivedAnnouncement));

        // When & Then
        assertThatThrownBy(() -> announcementService.addReply(2L, 2L, "Test reply message"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Cannot reply to non-published announcement");

        verify(announcementRepository).findByIdForUpdate(2L);
        verify(userRepository, never()).findById(any());
        verify(announcementReplyRepository, never()).save(any());
    }

    @Test
    void addReply_WithInvalidUser_ShouldThrowNotFoundException() {
        // Given
        when(announcementRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testAnnouncement));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> announcementService.addReply(1L, 999L, "Test reply message"))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("User not found with ID: 999");

        verify(announcementRepository).findByIdForUpdate(1L);
        verify(userRepository).findById(999L);
        verify(announcementReplyRepository, never()).save(any());
    }

    @Test
    void getAnnouncementReplies_WithValidAnnouncement_ShouldReturnReplies() {
        // Given
        when(announcementRepository.findById(1L)).thenReturn(Optional.of(testAnnouncement));
        when(announcementReplyRepository.findByAnnouncementOrderByCreatedAtAsc(testAnnouncement))
            .thenReturn(Arrays.asList(testReply));

        // When
        List<AnnouncementReply> result = announcementService.getAnnouncementReplies(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testReply);

        verify(announcementRepository).findById(1L);
        verify(announcementReplyRepository).findByAnnouncementOrderByCreatedAtAsc(testAnnouncement);
    }

    @Test
    void deleteReply_WithValidReplyAndAuthor_ShouldDeleteReply() {
        // Given
        when(announcementReplyRepository.findById(1L)).thenReturn(Optional.of(testReply));

        // When
        announcementService.deleteReply(1L, 2L);

        // Then
        verify(announcementReplyRepository).findById(1L);
        verify(announcementReplyRepository).delete(testReply);
    }

    @Test
    void deleteReply_WithNonAuthor_ShouldThrowBusinessRuleException() {
        // Given
        when(announcementReplyRepository.findById(1L)).thenReturn(Optional.of(testReply));

        // When & Then
        assertThatThrownBy(() -> announcementService.deleteReply(1L, 1L))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("You can only delete your own replies");

        verify(announcementReplyRepository).findById(1L);
        verify(announcementReplyRepository, never()).delete(any());
    }

    @Test
    void deleteReply_WithInvalidReplyId_ShouldThrowNotFoundException() {
        // Given
        when(announcementReplyRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> announcementService.deleteReply(999L, 2L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Reply not found with ID: 999");

        verify(announcementReplyRepository).findById(999L);
        verify(announcementReplyRepository, never()).delete(any());
    }

    // ==================== SEARCH AND FILTERING TESTS ====================

    @Test
    void getPublishedAnnouncements_ShouldReturnPublishedAnnouncements() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Announcement> expectedPage = new PageImpl<>(Arrays.asList(testAnnouncement));
        when(announcementRepository.findByStatus(AnnouncementStatus.PUBLISHED, pageable))
            .thenReturn(expectedPage);

        // When
        Page<Announcement> result = announcementService.getPublishedAnnouncements(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(testAnnouncement);

        verify(announcementRepository).findByStatus(AnnouncementStatus.PUBLISHED, pageable);
    }

    @Test
    void getArchivedAnnouncements_ShouldReturnArchivedAnnouncements() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Announcement> expectedPage = new PageImpl<>(Arrays.asList(testArchivedAnnouncement));
        when(announcementRepository.findByStatus(AnnouncementStatus.ARCHIVED, pageable))
            .thenReturn(expectedPage);

        // When
        Page<Announcement> result = announcementService.getArchivedAnnouncements(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(testArchivedAnnouncement);

        verify(announcementRepository).findByStatus(AnnouncementStatus.ARCHIVED, pageable);
    }

    @Test
    void getAnnouncementsByPriority_ShouldReturnAnnouncementsByPriority() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Announcement> expectedPage = new PageImpl<>(Arrays.asList(testAnnouncement));
        when(announcementRepository.findByPriority(AnnouncementPriority.HIGH, pageable))
            .thenReturn(expectedPage);

        // When
        Page<Announcement> result = announcementService.getAnnouncementsByPriority(
            AnnouncementPriority.HIGH, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(testAnnouncement);

        verify(announcementRepository).findByPriority(AnnouncementPriority.HIGH, pageable);
    }

    @Test
    void getAnnouncementsByCreator_ShouldReturnAnnouncementsByCreator() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Announcement> expectedPage = new PageImpl<>(Arrays.asList(testAnnouncement));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(announcementRepository.findByCreatedBy(testUser, pageable)).thenReturn(expectedPage);

        // When
        Page<Announcement> result = announcementService.getAnnouncementsByCreator(1L, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(testAnnouncement);

        verify(userRepository).findById(1L);
        verify(announcementRepository).findByCreatedBy(testUser, pageable);
    }

    @Test
    void searchAnnouncements_ShouldReturnMatchingAnnouncements() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Announcement> searchResults = new PageImpl<>(Arrays.asList(testAnnouncement));

        when(announcementRepository.searchByTitleOrContent("test", pageable))
            .thenReturn(searchResults);

        // When
        Page<Announcement> result = announcementService.searchAnnouncements("test", pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(testAnnouncement);

        verify(announcementRepository).searchByTitleOrContent("test", pageable);
    }

    @Test
    void getHighPriorityAnnouncements_ShouldReturnHighPriorityPublishedAnnouncements() {
        // Given
        when(announcementRepository.findByPriorityAndStatus(
            AnnouncementPriority.HIGH, AnnouncementStatus.PUBLISHED))
            .thenReturn(Arrays.asList(testAnnouncement));

        // When
        List<Announcement> result = announcementService.getHighPriorityAnnouncements();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testAnnouncement);

        verify(announcementRepository).findByPriorityAndStatus(
            AnnouncementPriority.HIGH, AnnouncementStatus.PUBLISHED);
    }

    @Test
    void getRecentAnnouncements_ShouldReturnRecentAnnouncements() {
        // Given
        when(announcementRepository.findRecentAnnouncements(any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(testAnnouncement));

        // When
        List<Announcement> result = announcementService.getRecentAnnouncements();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testAnnouncement);

        verify(announcementRepository).findRecentAnnouncements(any(LocalDateTime.class));
    }

    @Test
    void getAnnouncementsByDateRange_ShouldReturnAnnouncementsInDateRange() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(10);
        LocalDateTime endDate = LocalDateTime.now();
        when(announcementRepository.findByCreatedAtBetween(startDate, endDate))
            .thenReturn(Arrays.asList(testAnnouncement));

        // When
        List<Announcement> result = announcementService.getAnnouncementsByDateRange(startDate, endDate);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testAnnouncement);

        verify(announcementRepository).findByCreatedAtBetween(startDate, endDate);
    }

    @Test
    void getMostRepliedAnnouncements_ShouldReturnAnnouncementsSortedByReplyCount() {
        // Given
        when(announcementRepository.findMostRepliedAnnouncements(any()))
            .thenReturn(Arrays.asList(testAnnouncement, testArchivedAnnouncement));

        // When
        List<Announcement> result = announcementService.getMostRepliedAnnouncements(2);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(testAnnouncement);
        assertThat(result.get(1)).isEqualTo(testArchivedAnnouncement);

        verify(announcementRepository).findMostRepliedAnnouncements(any());
    }

    // ==================== STATISTICS TESTS ====================

    @Test
    void getAnnouncementStatistics_ShouldReturnCorrectStatistics() {
        // Given
        when(announcementRepository.count()).thenReturn(10L);
        when(announcementRepository.countByStatus(AnnouncementStatus.PUBLISHED)).thenReturn(7L);
        when(announcementRepository.countByStatus(AnnouncementStatus.ARCHIVED)).thenReturn(3L);
        when(announcementRepository.countByPriority(AnnouncementPriority.HIGH)).thenReturn(2L);
        when(announcementRepository.countByPriority(AnnouncementPriority.MEDIUM)).thenReturn(5L);
        when(announcementRepository.countByPriority(AnnouncementPriority.LOW)).thenReturn(3L);
        when(announcementReplyRepository.count()).thenReturn(25L);

        // When
        var result = announcementService.getAnnouncementStatistics();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalAnnouncements()).isEqualTo(10L);
        assertThat(result.getPublishedAnnouncements()).isEqualTo(7L);
        assertThat(result.getArchivedAnnouncements()).isEqualTo(3L);
        assertThat(result.getHighPriorityAnnouncements()).isEqualTo(2L);
        assertThat(result.getMediumPriorityAnnouncements()).isEqualTo(5L);
        assertThat(result.getLowPriorityAnnouncements()).isEqualTo(3L);
        assertThat(result.getTotalReplies()).isEqualTo(25L);

        verify(announcementRepository).count();
        verify(announcementRepository).countByStatus(AnnouncementStatus.PUBLISHED);
        verify(announcementRepository).countByStatus(AnnouncementStatus.ARCHIVED);
        verify(announcementRepository).countByPriority(AnnouncementPriority.HIGH);
        verify(announcementRepository).countByPriority(AnnouncementPriority.MEDIUM);
        verify(announcementRepository).countByPriority(AnnouncementPriority.LOW);
        verify(announcementReplyRepository).count();
    }

    @Test
    void getPublishedAnnouncementsOrderedByPriority_ShouldReturnOrderedAnnouncements() {
        // Given
        when(announcementRepository.findPublishedAnnouncementsOrderByPriorityAndDate())
            .thenReturn(Arrays.asList(testAnnouncement));

        // When
        List<Announcement> result = announcementService.getPublishedAnnouncementsOrderedByPriority();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testAnnouncement);

        verify(announcementRepository).findPublishedAnnouncementsOrderByPriorityAndDate();
    }

    @Test
    void getAnnouncementsByCreatorOrderedByDate_ShouldReturnOrderedAnnouncements() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(announcementRepository.findByCreatedByOrderByCreatedAtDesc(testUser))
            .thenReturn(Arrays.asList(testAnnouncement));

        // When
        List<Announcement> result = announcementService.getAnnouncementsByCreatorOrderedByDate(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testAnnouncement);

        verify(userRepository).findById(1L);
        verify(announcementRepository).findByCreatedByOrderByCreatedAtDesc(testUser);
    }

    @Test
    void createAnnouncement_WithAudienceScoping_ShouldPersistTargetDepartmentAndRole() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(announcementRepository.save(any(Announcement.class))).thenAnswer(inv -> inv.getArgument(0));

        Announcement result = announcementService.createAnnouncement(
                1L, "Housekeeping Update", "Deep clean floor 3",
                AnnouncementPriority.HIGH, Department.HOUSEKEEPING, Role.EMPLOYEE);

        assertThat(result.getTargetDepartment()).isEqualTo(Department.HOUSEKEEPING);
        assertThat(result.getTargetRole()).isEqualTo(Role.EMPLOYEE);
        assertThat(result.getStatus()).isEqualTo(AnnouncementStatus.PUBLISHED);
    }

    @Test
    void getVisibleAnnouncementsForUser_Employee_ShouldScopeByDepartmentAndRole() {
        testUser2.setRole(Role.EMPLOYEE);
        Employee emp = new Employee(testUser2, EmployeeStatus.ACTIVE, "Housekeeper", Department.HOUSEKEEPING);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Announcement> expectedPage = new PageImpl<>(Arrays.asList(testAnnouncement));

        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser2));
        when(employeeRepository.findByUserId(2L)).thenReturn(Optional.of(emp));
        when(announcementRepository.findVisibleAnnouncementsForAudience(
                Department.HOUSEKEEPING, Role.EMPLOYEE, AnnouncementStatus.PUBLISHED, pageable))
                .thenReturn(expectedPage);

        Page<Announcement> result = announcementService.getVisibleAnnouncementsForUser(2L, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(announcementRepository).findVisibleAnnouncementsForAudience(
                Department.HOUSEKEEPING, Role.EMPLOYEE, AnnouncementStatus.PUBLISHED, pageable);
    }

    @Test
    void getVisibleAnnouncementsForUser_Admin_ShouldReturnAllPublishedAnnouncements() {
        testUser.setRole(Role.ADMIN);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Announcement> expectedPage = new PageImpl<>(Arrays.asList(testAnnouncement));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(announcementRepository.findByStatus(AnnouncementStatus.PUBLISHED, pageable))
                .thenReturn(expectedPage);

        Page<Announcement> result = announcementService.getVisibleAnnouncementsForUser(1L, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(announcementRepository).findByStatus(AnnouncementStatus.PUBLISHED, pageable);
        verify(employeeRepository, never()).findByUserId(anyLong());
    }
}
