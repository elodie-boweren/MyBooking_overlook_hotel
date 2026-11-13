package com.MyBooking.announcement.service;

import com.MyBooking.auth.domain.Role;
import com.MyBooking.auth.domain.User;
import com.MyBooking.auth.repository.UserRepository;
import com.MyBooking.common.exception.BusinessRuleException;
import com.MyBooking.common.exception.NotFoundException;
import com.MyBooking.announcement.domain.*;
import com.MyBooking.announcement.dto.*;
import com.MyBooking.announcement.repository.*;
import com.MyBooking.employee.domain.Department;
import com.MyBooking.employee.domain.Employee;
import com.MyBooking.employee.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AnnouncementService {

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private AnnouncementReplyRepository announcementReplyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    // ==================== ANNOUNCEMENT MANAGEMENT ====================

    /**
     * Create and publish a new announcement
     */
    public Announcement createAnnouncement(Long createdByUserId, String title, String content, 
                                         AnnouncementPriority priority) {
        return createAnnouncement(createdByUserId, title, content, priority, Department.ALL, null);
    }

    /**
     * Create and publish a new announcement with audience scoping
     */
    public Announcement createAnnouncement(Long createdByUserId, String title, String content,
                                         AnnouncementPriority priority, Department targetDepartment, Role targetRole) {
        User createdBy = userRepository.findById(createdByUserId)
            .orElseThrow(() -> new NotFoundException("User not found with ID: " + createdByUserId));

        Department effectiveDepartment = targetDepartment != null ? targetDepartment : Department.ALL;
        Announcement announcement = new Announcement(
                title, content, createdBy, priority, AnnouncementStatus.PUBLISHED, effectiveDepartment, targetRole);

        return announcementRepository.save(announcement);
    }

    /**
     * Update an existing announcement
     */
    public Announcement updateAnnouncement(Long announcementId, String title, String content, 
                                         AnnouncementPriority priority) {
        Announcement announcement = announcementRepository.findByIdForUpdate(announcementId)
            .orElseThrow(() -> new NotFoundException("Announcement not found with ID: " + announcementId));
        
        // Check if announcement is archived
        if (announcement.getStatus() == AnnouncementStatus.ARCHIVED) {
            throw new BusinessRuleException("Cannot update archived announcement");
        }

        // Update fields
        if (title != null) {
            announcement.setTitle(title);
        }
        if (content != null) {
            announcement.setContent(content);
        }
        if (priority != null) {
            announcement.setPriority(priority);
        }

        return announcementRepository.save(announcement);
    }

    /**
     * Archive an announcement
     */
    public Announcement archiveAnnouncement(Long announcementId) {
        Announcement announcement = announcementRepository.findByIdForUpdate(announcementId)
            .orElseThrow(() -> new NotFoundException("Announcement not found with ID: " + announcementId));
        
        // Check if announcement is already archived
        if (announcement.getStatus() == AnnouncementStatus.ARCHIVED) {
            throw new BusinessRuleException("Announcement is already archived");
        }

        announcement.setStatus(AnnouncementStatus.ARCHIVED);
        return announcementRepository.save(announcement);
    }

    /**
     * Unarchive an announcement (restore to published)
     */
    public Announcement unarchiveAnnouncement(Long announcementId) {
        Announcement announcement = announcementRepository.findByIdForUpdate(announcementId)
            .orElseThrow(() -> new NotFoundException("Announcement not found with ID: " + announcementId));
        
        // Check if announcement is not archived
        if (announcement.getStatus() != AnnouncementStatus.ARCHIVED) {
            throw new BusinessRuleException("Only archived announcements can be unarchived");
        }

        announcement.setStatus(AnnouncementStatus.PUBLISHED);
        return announcementRepository.save(announcement);
    }

    /**
     * Delete an announcement
     */
    public void deleteAnnouncement(Long announcementId) {
        Announcement announcement = announcementRepository.findByIdForUpdate(announcementId)
            .orElseThrow(() -> new NotFoundException("Announcement not found with ID: " + announcementId));
        
        // Check if announcement has replies
        if (announcementReplyRepository.existsByAnnouncement(announcement)) {
            throw new BusinessRuleException("Cannot delete announcement with replies");
        }

        announcementRepository.delete(announcement);
    }

    /**
     * Get announcement by ID
     */
    @Transactional(readOnly = true)
    public Announcement getAnnouncementById(Long announcementId) {
        return announcementRepository.findById(announcementId)
            .orElseThrow(() -> new NotFoundException("Announcement not found with ID: " + announcementId));
    }

    // ==================== REPLY MANAGEMENT ====================

    /**
     * Add a reply to an announcement
     */
    public AnnouncementReply addReply(Long announcementId, Long userId, String message) {
        // Lock announcement row to serialize against concurrent archive/delete
        Announcement announcement = announcementRepository.findByIdForUpdate(announcementId)
            .orElseThrow(() -> new NotFoundException("Announcement not found with ID: " + announcementId));
        
        if (announcement.getStatus() != AnnouncementStatus.PUBLISHED) {
            throw new BusinessRuleException("Cannot reply to non-published announcement");
        }

        // Validate user exists
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));

        // Create reply
        AnnouncementReply reply = new AnnouncementReply(announcement, user, message);
        return announcementReplyRepository.save(reply);
    }

    /**
     * Get all replies for an announcement
     */
    @Transactional(readOnly = true)
    public List<AnnouncementReply> getAnnouncementReplies(Long announcementId) {
        Announcement announcement = getAnnouncementById(announcementId);
        return announcementReplyRepository.findByAnnouncementOrderByCreatedAtAsc(announcement);
    }

    /**
     * Delete a reply (only by the user who created it)
     */
    public void deleteReply(Long replyId, Long userId) {
        AnnouncementReply reply = announcementReplyRepository.findById(replyId)
            .orElseThrow(() -> new NotFoundException("Reply not found with ID: " + replyId));

        // Check if user is the author of the reply
        if (!reply.getUser().getId().equals(userId)) {
            throw new BusinessRuleException("You can only delete your own replies");
        }

        announcementReplyRepository.delete(reply);
    }

    // ==================== SEARCH AND FILTERING ====================

    /**
     * Get all published announcements
     */
    @Transactional(readOnly = true)
    public Page<Announcement> getPublishedAnnouncements(Pageable pageable) {
        return announcementRepository.findByStatus(AnnouncementStatus.PUBLISHED, pageable);
    }

    /**
     * Get all archived announcements
     */
    @Transactional(readOnly = true)
    public Page<Announcement> getArchivedAnnouncements(Pageable pageable) {
        return announcementRepository.findByStatus(AnnouncementStatus.ARCHIVED, pageable);
    }

    /**
     * Get announcements by priority
     */
    @Transactional(readOnly = true)
    public Page<Announcement> getAnnouncementsByPriority(AnnouncementPriority priority, Pageable pageable) {
        return announcementRepository.findByPriority(priority, pageable);
    }

    /**
     * Get announcements by status
     */
    @Transactional(readOnly = true)
    public Page<Announcement> getAnnouncementsByStatus(AnnouncementStatus status, Pageable pageable) {
        return announcementRepository.findByStatus(status, pageable);
    }

    /**
     * Get announcements created by a specific user
     */
    @Transactional(readOnly = true)
    public Page<Announcement> getAnnouncementsByCreator(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));
        
        return announcementRepository.findByCreatedBy(user, pageable);
    }

    /**
     * Search announcements by title or content
     */
    @Transactional(readOnly = true)
    public Page<Announcement> searchAnnouncements(String searchTerm, Pageable pageable) {
        return announcementRepository.searchByTitleOrContent(searchTerm, pageable);
    }

    /**
     * Get announcements with high priority
     */
    @Transactional(readOnly = true)
    public List<Announcement> getHighPriorityAnnouncements() {
        return announcementRepository.findByPriorityAndStatus(
            AnnouncementPriority.HIGH, AnnouncementStatus.PUBLISHED);
    }

    /**
     * Get recent announcements (last 7 days)
     */
    @Transactional(readOnly = true)
    public List<Announcement> getRecentAnnouncements() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        return announcementRepository.findRecentAnnouncements(weekAgo);
    }

    /**
     * Get announcements by date range
     */
    @Transactional(readOnly = true)
    public List<Announcement> getAnnouncementsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return announcementRepository.findByCreatedAtBetween(startDate, endDate);
    }

    /**
     * Get most replied announcements
     */
    @Transactional(readOnly = true)
    public List<Announcement> getMostRepliedAnnouncements(int limit) {
        return announcementRepository.findMostRepliedAnnouncements(PageRequest.of(0, limit));
    }

    // ==================== STATISTICS AND ANALYTICS ====================

    @Transactional(readOnly = true)
    public long getTotalAnnouncementsCount() {
        return announcementRepository.count();
    }

    /**
     * Get announcement statistics
     */
    @Transactional(readOnly = true)
    public AnnouncementStatistics getAnnouncementStatistics() {
        long totalAnnouncements = announcementRepository.count();
        long publishedAnnouncements = announcementRepository.countByStatus(AnnouncementStatus.PUBLISHED);
        long archivedAnnouncements = announcementRepository.countByStatus(AnnouncementStatus.ARCHIVED);
        
        long highPriorityAnnouncements = announcementRepository.countByPriority(AnnouncementPriority.HIGH);
        long mediumPriorityAnnouncements = announcementRepository.countByPriority(AnnouncementPriority.MEDIUM);
        long lowPriorityAnnouncements = announcementRepository.countByPriority(AnnouncementPriority.LOW);
        
        long totalReplies = announcementReplyRepository.count();
        
        return new AnnouncementStatistics(
            totalAnnouncements, publishedAnnouncements, archivedAnnouncements,
            highPriorityAnnouncements, mediumPriorityAnnouncements, lowPriorityAnnouncements,
            totalReplies
        );
    }

    /**
     * Get published announcements ordered by priority and date
     */
    @Transactional(readOnly = true)
    public List<Announcement> getPublishedAnnouncementsOrderedByPriority() {
        return announcementRepository.findPublishedAnnouncementsOrderByPriorityAndDate();
    }

    /**
     * Get announcements created by user ordered by date
     */
    @Transactional(readOnly = true)
    public List<Announcement> getAnnouncementsByCreatorOrderedByDate(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));
        
        return announcementRepository.findByCreatedByOrderByCreatedAtDesc(user);
    }

    // ==================== AUDIENCE SCOPING & DTO WRAPPERS ====================

    /**
     * Get published announcements visible to a specific user based on role and department.
     */
    @Transactional(readOnly = true)
    public Page<Announcement> getVisibleAnnouncementsForUser(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));

        if (user.getRole() == Role.ADMIN) {
            return announcementRepository.findByStatus(AnnouncementStatus.PUBLISHED, pageable);
        }

        Department department = resolveUserDepartment(userId);
        return announcementRepository.findVisibleAnnouncementsForAudience(
                department, user.getRole(), AnnouncementStatus.PUBLISHED, pageable);
    }

    public AnnouncementResponseDto createAnnouncementAsDto(Long createdByUserId, AnnouncementCreateRequestDto request) {
        Announcement created = createAnnouncement(
                createdByUserId,
                request.getTitle(),
                request.getContent(),
                request.getPriority(),
                request.getTargetDepartment(),
                request.getTargetRole()
        );
        return toAnnouncementDto(created);
    }

    @Transactional(readOnly = true)
    public Page<AnnouncementResponseDto> getVisibleAnnouncementsForUserAsDto(Long userId, Pageable pageable) {
        return getVisibleAnnouncementsForUser(userId, pageable).map(this::toAnnouncementDto);
    }

    @Transactional(readOnly = true)
    public AnnouncementResponseDto getAnnouncementByIdForUserAsDto(Long announcementId, Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));
        Announcement announcement = getAnnouncementById(announcementId);

        assertUserCanAccessAnnouncement(user, announcement);
        return toAnnouncementDto(announcement);
    }

    public AnnouncementResponseDto updateAnnouncementAsDto(Long announcementId, AnnouncementUpdateRequestDto request) {
        Announcement updated = updateAnnouncement(
                announcementId, request.getTitle(), request.getContent(), request.getPriority());
        return toAnnouncementDto(updated);
    }

    public AnnouncementResponseDto archiveAnnouncementAsDto(Long announcementId) {
        Announcement archived = archiveAnnouncement(announcementId);
        return toAnnouncementDto(archived);
    }

    public AnnouncementReplyResponseDto addReplyAsDto(Long announcementId, Long userId, AnnouncementReplyRequestDto request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));
        Announcement announcement = getAnnouncementById(announcementId);
        assertUserCanAccessAnnouncement(user, announcement);

        AnnouncementReply reply = addReply(announcementId, userId, request.getMessage());
        return toReplyDto(reply);
    }

    @Transactional(readOnly = true)
    public List<AnnouncementReplyResponseDto> getRepliesAsDto(Long announcementId, Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));
        Announcement announcement = getAnnouncementById(announcementId);
        assertUserCanAccessAnnouncement(user, announcement);

        return announcementReplyRepository.findByAnnouncementOrderByCreatedAtAsc(announcement)
                .stream()
                .map(this::toReplyDto)
                .toList();
    }

    private Department resolveUserDepartment(Long userId) {
        if (employeeRepository == null) {
            return Department.ALL;
        }
        return employeeRepository.findByUserId(userId)
                .map(Employee::getDepartment)
                .orElse(Department.ALL);
    }

    private void assertUserCanAccessAnnouncement(User user, Announcement announcement) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        if (announcement.getStatus() != AnnouncementStatus.PUBLISHED) {
            throw new AccessDeniedException("Announcement is not accessible");
        }
        Department userDepartment = resolveUserDepartment(user.getId());
        Department targetDepartment = announcement.getTargetDepartment();
        boolean departmentMatches = targetDepartment == null
                || targetDepartment == Department.ALL
                || targetDepartment == userDepartment;
        boolean roleMatches = announcement.getTargetRole() == null
                || announcement.getTargetRole() == user.getRole();

        if (!departmentMatches || !roleMatches) {
            throw new AccessDeniedException("You do not have access to this announcement");
        }
    }

    private AnnouncementResponseDto toAnnouncementDto(Announcement announcement) {
        User creator = announcement.getCreatedBy();
        String creatorName = creator != null
                ? (creator.getFirstName() + " " + creator.getLastName()).trim()
                : "";
        return new AnnouncementResponseDto(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getContent(),
                creator != null ? creator.getId() : null,
                creatorName,
                announcement.getPriority(),
                announcement.getStatus(),
                announcement.getTargetDepartment(),
                announcement.getTargetRole(),
                announcement.getCreatedAt(),
                announcement.getUpdatedAt()
        );
    }

    private AnnouncementReplyResponseDto toReplyDto(AnnouncementReply reply) {
        User author = reply.getUser();
        String authorName = author != null
                ? (author.getFirstName() + " " + author.getLastName()).trim()
                : "";
        return new AnnouncementReplyResponseDto(
                reply.getId(),
                reply.getAnnouncement() != null ? reply.getAnnouncement().getId() : null,
                author != null ? author.getId() : null,
                authorName,
                reply.getMessage(),
                reply.getCreatedAt()
        );
    }
}

// Statistics DTO
class AnnouncementStatistics {
    private final long totalAnnouncements;
    private final long publishedAnnouncements;
    private final long archivedAnnouncements;
    private final long highPriorityAnnouncements;
    private final long mediumPriorityAnnouncements;
    private final long lowPriorityAnnouncements;
    private final long totalReplies;

    public AnnouncementStatistics(long totalAnnouncements, long publishedAnnouncements, 
                                long archivedAnnouncements, long highPriorityAnnouncements, 
                                long mediumPriorityAnnouncements, long lowPriorityAnnouncements,
                                long totalReplies) {
        this.totalAnnouncements = totalAnnouncements;
        this.publishedAnnouncements = publishedAnnouncements;
        this.archivedAnnouncements = archivedAnnouncements;
        this.highPriorityAnnouncements = highPriorityAnnouncements;
        this.mediumPriorityAnnouncements = mediumPriorityAnnouncements;
        this.lowPriorityAnnouncements = lowPriorityAnnouncements;
        this.totalReplies = totalReplies;
    }

    // Getters
    public long getTotalAnnouncements() { return totalAnnouncements; }
    public long getPublishedAnnouncements() { return publishedAnnouncements; }
    public long getArchivedAnnouncements() { return archivedAnnouncements; }
    public long getHighPriorityAnnouncements() { return highPriorityAnnouncements; }
    public long getMediumPriorityAnnouncements() { return mediumPriorityAnnouncements; }
    public long getLowPriorityAnnouncements() { return lowPriorityAnnouncements; }
    public long getTotalReplies() { return totalReplies; }
}