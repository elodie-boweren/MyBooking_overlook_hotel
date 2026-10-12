package com.MyBooking.employee.repository;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import com.MyBooking.hotel_management.HotelManagementApplication;
import com.MyBooking.auth.domain.Role;
import com.MyBooking.auth.domain.User;
import com.MyBooking.employee.domain.LeaveRequest;
import com.MyBooking.employee.domain.LeaveRequestStatus;
import com.MyBooking.employee.repository.LeaveRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("repository-test")
@ContextConfiguration(classes = HotelManagementApplication.class)
@EntityScan({"com.MyBooking.employee.domain", "com.MyBooking.auth.domain"})
@EnableJpaRepositories("com.MyBooking.employee.repository")
@Transactional
@Rollback
class LeaveRequestRepositoryTest {

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private TestEntityManager entityManager;

    // Test data
    private User employee1, employee2, employee3, employee4, employee5;
    private LeaveRequest leaveRequest1, leaveRequest2, leaveRequest3, leaveRequest4, leaveRequest5;
    private LeaveRequest leaveRequest6, leaveRequest7, leaveRequest8, leaveRequest9, leaveRequest10;
    private LeaveRequest leaveRequest11, leaveRequest12, leaveRequest13, leaveRequest14, leaveRequest15;
    private LocalDate baseDate;

    @BeforeEach
    void setUp() {
        // Clear any existing data
        leaveRequestRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        // Base date for testing
        baseDate = LocalDate.of(2024, 1, 15);

        // Create test employees
        employee1 = new User("John", "Doe", "john.doe@hotel.com", "password123", "55510101",  
                           "123 Main St", LocalDate.of(1990, 5, 15), Role.EMPLOYEE);
        employee2 = new User("Jane", "Smith", "jane.smith@hotel.com", "password123", "55510102", 
                           "456 Oak Ave", LocalDate.of(1988, 8, 22), Role.EMPLOYEE);
        employee3 = new User("Bob", "Johnson", "bob.johnson@hotel.com", "password123", "55510103",
                           "789 Pine St", LocalDate.of(1992, 3, 10), Role.EMPLOYEE);
        employee4 = new User("Alice", "Brown", "alice.brown@hotel.com", "password123", "55510104",
                           "321 Elm St", LocalDate.of(1985, 12, 5), Role.EMPLOYEE);
        employee5 = new User("Charlie", "Wilson", "charlie.wilson@hotel.com", "password123", "55510105",  
                           "654 Maple Ave", LocalDate.of(1991, 7, 18), Role.EMPLOYEE);

        // Save employees
        employee1 = entityManager.persistAndFlush(employee1);
        employee2 = entityManager.persistAndFlush(employee2);
        employee3 = entityManager.persistAndFlush(employee3);
        employee4 = entityManager.persistAndFlush(employee4);
        employee5 = entityManager.persistAndFlush(employee5);

        // Create test leave requests
        // Employee1: 3 leave requests
        leaveRequest1 = new LeaveRequest(employee1, baseDate, baseDate.plusDays(5), LeaveRequestStatus.PENDING, "Vacation");
        leaveRequest2 = new LeaveRequest(employee1, baseDate.plusDays(10), baseDate.plusDays(15), LeaveRequestStatus.APPROVED, "Sick leave");
        leaveRequest3 = new LeaveRequest(employee1, baseDate.plusDays(20), baseDate.plusDays(25), LeaveRequestStatus.REJECTED, "Personal");

        // Employee2: 3 leave requests
        leaveRequest4 = new LeaveRequest(employee2, baseDate.plusDays(2), baseDate.plusDays(7), LeaveRequestStatus.PENDING, "Family emergency");
        leaveRequest5 = new LeaveRequest(employee2, baseDate.plusDays(12), baseDate.plusDays(17), LeaveRequestStatus.APPROVED, "Medical");
        leaveRequest6 = new LeaveRequest(employee2, baseDate.plusDays(22), baseDate.plusDays(27), LeaveRequestStatus.PENDING, "Training");

        // Employee3: 3 leave requests
        leaveRequest7 = new LeaveRequest(employee3, baseDate.plusDays(1), baseDate.plusDays(6), LeaveRequestStatus.APPROVED, "Holiday");
        leaveRequest8 = new LeaveRequest(employee3, baseDate.plusDays(11), baseDate.plusDays(16), LeaveRequestStatus.REJECTED, "Personal");
        leaveRequest9 = new LeaveRequest(employee3, baseDate.plusDays(21), baseDate.plusDays(26), LeaveRequestStatus.PENDING, "Vacation");

        // Employee4: 3 leave requests
        leaveRequest10 = new LeaveRequest(employee4, baseDate.plusDays(3), baseDate.plusDays(8), LeaveRequestStatus.PENDING, "Sick");
        leaveRequest11 = new LeaveRequest(employee4, baseDate.plusDays(13), baseDate.plusDays(18), LeaveRequestStatus.APPROVED, "Family");
        leaveRequest12 = new LeaveRequest(employee4, baseDate.plusDays(23), baseDate.plusDays(28), LeaveRequestStatus.REJECTED, "Personal");

        // Employee5: 3 leave requests
        leaveRequest13 = new LeaveRequest(employee5, baseDate.plusDays(4), baseDate.plusDays(9), LeaveRequestStatus.APPROVED, "Vacation");
        leaveRequest14 = new LeaveRequest(employee5, baseDate.plusDays(14), baseDate.plusDays(19), LeaveRequestStatus.PENDING, "Medical");
        leaveRequest15 = new LeaveRequest(employee5, baseDate.plusDays(24), baseDate.plusDays(29), LeaveRequestStatus.APPROVED, "Training");

        // Save leave requests
        leaveRequest1 = entityManager.persistAndFlush(leaveRequest1);
        leaveRequest2 = entityManager.persistAndFlush(leaveRequest2);
        leaveRequest3 = entityManager.persistAndFlush(leaveRequest3);
        leaveRequest4 = entityManager.persistAndFlush(leaveRequest4);
        leaveRequest5 = entityManager.persistAndFlush(leaveRequest5);
        leaveRequest6 = entityManager.persistAndFlush(leaveRequest6);
        leaveRequest7 = entityManager.persistAndFlush(leaveRequest7);
        leaveRequest8 = entityManager.persistAndFlush(leaveRequest8);
        leaveRequest9 = entityManager.persistAndFlush(leaveRequest9);
        leaveRequest10 = entityManager.persistAndFlush(leaveRequest10);
        leaveRequest11 = entityManager.persistAndFlush(leaveRequest11);
        leaveRequest12 = entityManager.persistAndFlush(leaveRequest12);
        leaveRequest13 = entityManager.persistAndFlush(leaveRequest13);
        leaveRequest14 = entityManager.persistAndFlush(leaveRequest14);
        leaveRequest15 = entityManager.persistAndFlush(leaveRequest15);

    }

    // ==================== BASIC QUERIES TESTS ====================

    @Test
    void testFindByEmployee() {
        List<LeaveRequest> employee1Requests = leaveRequestRepository.findByEmployee(employee1);
        
        assertThat(employee1Requests).hasSize(3);
        assertThat(employee1Requests).containsExactlyInAnyOrder(leaveRequest1, leaveRequest2, leaveRequest3);
    }

    @Test
    void testFindByEmployeeId() {
        List<LeaveRequest> employee2Requests = leaveRequestRepository.findByEmployeeId(employee2.getId());
        
        assertThat(employee2Requests).hasSize(3);
        assertThat(employee2Requests).containsExactlyInAnyOrder(leaveRequest4, leaveRequest5, leaveRequest6);
    }

    @Test
    void testFindByStatus() {
        List<LeaveRequest> pendingRequests = leaveRequestRepository.findByStatus(LeaveRequestStatus.PENDING);
        
        assertThat(pendingRequests).hasSize(6); // leaveRequest1, leaveRequest4, leaveRequest6, leaveRequest9, leaveRequest10, leaveRequest14
        assertThat(pendingRequests).containsExactlyInAnyOrder(leaveRequest1, leaveRequest4, leaveRequest6, leaveRequest9, leaveRequest10, leaveRequest14);
    }

    @Test
    void testFindByEmployeeAndStatus() {
        List<LeaveRequest> employee1ApprovedRequests = leaveRequestRepository.findByEmployeeAndStatus(employee1, LeaveRequestStatus.APPROVED);
        
        assertThat(employee1ApprovedRequests).hasSize(1);
        assertThat(employee1ApprovedRequests).containsExactly(leaveRequest2);
    }

    @Test
    void testFindByEmployeeIdAndStatus() {
        List<LeaveRequest> employee3RejectedRequests = leaveRequestRepository.findByEmployeeIdAndStatus(employee3.getId(), LeaveRequestStatus.REJECTED);
        
        assertThat(employee3RejectedRequests).hasSize(1);
        assertThat(employee3RejectedRequests).containsExactly(leaveRequest8);
    }

    // ==================== DATE RANGE QUERIES TESTS ====================

    @Test
    void testFindByFromDateAfter() {
        LocalDate cutoff = baseDate.plusDays(20);
        List<LeaveRequest> requestsAfter = leaveRequestRepository.findByFromDateAfter(cutoff);
        
        assertThat(requestsAfter).hasSize(4); // leaveRequest3, leaveRequest6, leaveRequest9, leaveRequest12
        assertThat(requestsAfter).allMatch(request -> request.getFromDate().isAfter(cutoff));
    }

    @Test
    void testFindByToDateBefore() {
        LocalDate cutoff = baseDate.plusDays(10);
        List<LeaveRequest> requestsBefore = leaveRequestRepository.findByToDateBefore(cutoff);
        
        assertThat(requestsBefore).hasSize(5); // leaveRequest1, leaveRequest4, leaveRequest7
        assertThat(requestsBefore).allMatch(request -> request.getToDate().isBefore(cutoff));
    }

    @Test
    void testFindByFromDateBetween() {
        LocalDate startDate = baseDate.plusDays(10);
        LocalDate endDate = baseDate.plusDays(20);
        List<LeaveRequest> requestsBetween = leaveRequestRepository.findByFromDateBetween(startDate, endDate);
        
        assertThat(requestsBetween).hasSize(6); // leaveRequest2, leaveRequest5, leaveRequest8, leaveRequest11, leaveRequest13, leaveRequest15
        assertThat(requestsBetween).allMatch(request -> 
            !request.getFromDate().isBefore(startDate) && !request.getFromDate().isAfter(endDate));
    }

    @Test
    void testFindByToDateBetween() {
        LocalDate startDate = baseDate.plusDays(10);
        LocalDate endDate = baseDate.plusDays(20);
        List<LeaveRequest> requestsBetween = leaveRequestRepository.findByToDateBetween(startDate, endDate);
        
        assertThat(requestsBetween).hasSize(5); // leaveRequest2, leaveRequest5, leaveRequest8, leaveRequest11
        assertThat(requestsBetween).allMatch(request -> 
            !request.getToDate().isBefore(startDate) && !request.getToDate().isAfter(endDate));
    }

    @Test
    void testFindOverlappingLeaveRequests() {
        LocalDate startDate = baseDate.plusDays(16);
        LocalDate endDate = baseDate.plusDays(18);
        List<LeaveRequest> overlappingRequests = leaveRequestRepository.findOverlappingLeaveRequests(startDate, endDate);
        
        assertThat(overlappingRequests).hasSize(4); // leaveRequest2, leaveRequest5, leaveRequest11, leaveRequest13
    }

    @Test
    void testFindOverlappingLeaveRequestsForEmployee() {
        LocalDate startDate = baseDate.plusDays(16);
        LocalDate endDate = baseDate.plusDays(18);
        List<LeaveRequest> overlappingRequests = leaveRequestRepository.findOverlappingLeaveRequestsForEmployee(employee2, startDate, endDate);
        
        assertThat(overlappingRequests).hasSize(1);
        assertThat(overlappingRequests).containsExactly(leaveRequest5);
    }

    @Test
    void testFindOverlappingLeaveRequestsForEmployeeId() {
        LocalDate startDate = baseDate.plusDays(16);
        LocalDate endDate = baseDate.plusDays(18);
        List<LeaveRequest> overlappingRequests = leaveRequestRepository.findOverlappingLeaveRequestsForEmployeeId(employee4.getId(), startDate, endDate);
        
        assertThat(overlappingRequests).hasSize(1);
        assertThat(overlappingRequests).containsExactly(leaveRequest11);
    }

    // ==================== STATUS AND EMPLOYEE COMBINATIONS TESTS ====================

    @Test
    void testFindByEmployeeAndFromDateAfter() {
        LocalDate cutoff = baseDate.plusDays(19);
        List<LeaveRequest> requests = leaveRequestRepository.findByEmployeeAndFromDateAfter(employee1, cutoff);
        
        assertThat(requests).hasSize(1);
        assertThat(requests).containsExactly(leaveRequest3);
    }

    @Test
    void testFindByEmployeeIdAndFromDateAfter() {
        LocalDate cutoff = baseDate.plusDays(20);
        List<LeaveRequest> requests = leaveRequestRepository.findByEmployeeIdAndFromDateAfter(employee2.getId(), cutoff);
        
        assertThat(requests).hasSize(1);
        assertThat(requests).containsExactly(leaveRequest6);
    }

    @Test
    void testFindByEmployeeAndToDateBefore() {
        LocalDate cutoff = baseDate.plusDays(10);
        List<LeaveRequest> requests = leaveRequestRepository.findByEmployeeAndToDateBefore(employee1, cutoff);
        
        assertThat(requests).hasSize(1);
        assertThat(requests).containsExactly(leaveRequest1);
    }

    @Test
    void testFindByEmployeeIdAndToDateBefore() {
        LocalDate cutoff = baseDate.plusDays(10);
        List<LeaveRequest> requests = leaveRequestRepository.findByEmployeeIdAndToDateBefore(employee2.getId(), cutoff);
        
        assertThat(requests).hasSize(1);
        assertThat(requests).containsExactly(leaveRequest4);
    }

    @Test
    void testFindByEmployeeAndStatusAndFromDateAfter() {
        LocalDate cutoff = baseDate.plusDays(20);
        List<LeaveRequest> requests = leaveRequestRepository.findByEmployeeAndStatusAndFromDateAfter(employee5, LeaveRequestStatus.APPROVED, cutoff);
        
        assertThat(requests).hasSize(1);
        assertThat(requests).containsExactly(leaveRequest15);
    }

    @Test
    void testFindByEmployeeIdAndStatusAndFromDateAfter() {
        LocalDate cutoff = baseDate.plusDays(19);
        List<LeaveRequest> requests = leaveRequestRepository.findByEmployeeIdAndStatusAndFromDateAfter(employee1.getId(), LeaveRequestStatus.REJECTED, cutoff);
        
        assertThat(requests).hasSize(1);
        assertThat(requests).containsExactly(leaveRequest3);
    }

    // ==================== EXISTENCE CHECKS TESTS ====================

    @Test
    void testExistsByEmployee() {
        assertThat(leaveRequestRepository.existsByEmployee(employee1)).isTrue();
        assertThat(leaveRequestRepository.existsByEmployee(employee2)).isTrue();
        assertThat(leaveRequestRepository.existsByEmployee(employee3)).isTrue();
    }

    @Test
    void testExistsByEmployeeId() {
        assertThat(leaveRequestRepository.existsByEmployeeId(employee1.getId())).isTrue();
        assertThat(leaveRequestRepository.existsByEmployeeId(employee2.getId())).isTrue();
        assertThat(leaveRequestRepository.existsByEmployeeId(employee3.getId())).isTrue();
    }

    @Test
    void testExistsByEmployeeAndStatus() {
        assertThat(leaveRequestRepository.existsByEmployeeAndStatus(employee1, LeaveRequestStatus.PENDING)).isTrue();
        assertThat(leaveRequestRepository.existsByEmployeeAndStatus(employee1, LeaveRequestStatus.APPROVED)).isTrue();
        assertThat(leaveRequestRepository.existsByEmployeeAndStatus(employee1, LeaveRequestStatus.REJECTED)).isTrue();
    }

    @Test
    void testExistsByEmployeeIdAndStatus() {
        assertThat(leaveRequestRepository.existsByEmployeeIdAndStatus(employee2.getId(), LeaveRequestStatus.PENDING)).isTrue();
        assertThat(leaveRequestRepository.existsByEmployeeIdAndStatus(employee2.getId(), LeaveRequestStatus.APPROVED)).isTrue();
        assertThat(leaveRequestRepository.existsByEmployeeIdAndStatus(employee2.getId(), LeaveRequestStatus.REJECTED)).isFalse();
    }

    @Test
    void testHasOverlappingLeaveRequest() {
        LocalDate startDate = baseDate.plusDays(16);
        LocalDate endDate = baseDate.plusDays(18);
        
        assertThat(leaveRequestRepository.hasOverlappingLeaveRequest(employee2, startDate, endDate)).isTrue();
        assertThat(leaveRequestRepository.hasOverlappingLeaveRequest(employee1, startDate, endDate)).isFalse();
    }

    @Test
    void testHasOverlappingLeaveRequestForEmployeeId() {
        LocalDate startDate = baseDate.plusDays(16);
        LocalDate endDate = baseDate.plusDays(18);
        
        assertThat(leaveRequestRepository.hasOverlappingLeaveRequestForEmployeeId(employee4.getId(), startDate, endDate)).isTrue();
        assertThat(leaveRequestRepository.hasOverlappingLeaveRequestForEmployeeId(employee1.getId(), startDate, endDate)).isFalse();
    }

    // ==================== PAGINATION SUPPORT TESTS ====================

    @Test
    void testFindByEmployeeWithPagination() {
        Page<LeaveRequest> page = leaveRequestRepository.findByEmployee(employee1, PageRequest.of(0, 2));
        
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    void testFindByEmployeeIdWithPagination() {
        Page<LeaveRequest> page = leaveRequestRepository.findByEmployeeId(employee2.getId(), PageRequest.of(0, 2));
        
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    void testFindByStatusWithPagination() {
        Page<LeaveRequest> page = leaveRequestRepository.findByStatus(LeaveRequestStatus.PENDING, PageRequest.of(0, 3));
        
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(6);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    void testFindByEmployeeAndStatusWithPagination() {
        Page<LeaveRequest> page = leaveRequestRepository.findByEmployeeAndStatus(employee3, LeaveRequestStatus.APPROVED, PageRequest.of(0, 1));
        
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getTotalPages()).isEqualTo(1);
    }

    @Test
    void testFindByFromDateAfterWithPagination() {
        LocalDate cutoff = baseDate.plusDays(20);
        Page<LeaveRequest> page = leaveRequestRepository.findByFromDateAfter(cutoff, PageRequest.of(0, 3));
        
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    void testFindByToDateBeforeWithPagination() {
        LocalDate cutoff = baseDate.plusDays(10);
        Page<LeaveRequest> page = leaveRequestRepository.findByToDateBefore(cutoff, PageRequest.of(0, 2));
        
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(3);
    }

    @Test
    void testFindByFromDateBetweenWithPagination() {
        LocalDate startDate = baseDate.plusDays(10);
        LocalDate endDate = baseDate.plusDays(20);
        Page<LeaveRequest> page = leaveRequestRepository.findByFromDateBetween(startDate, endDate, PageRequest.of(0, 2));
        
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(6);
        assertThat(page.getTotalPages()).isEqualTo(3);
    }

    @Test
    void testFindByToDateBetweenWithPagination() {
        LocalDate startDate = baseDate.plusDays(10);
        LocalDate endDate = baseDate.plusDays(20);
        Page<LeaveRequest> page = leaveRequestRepository.findByToDateBetween(startDate, endDate, PageRequest.of(0, 2));
        
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(3);
    }

    // ==================== STATISTICS AND COUNTS TESTS ====================

    @Test
    void testCountByEmployee() {
        long count = leaveRequestRepository.countByEmployee(employee1);
        assertThat(count).isEqualTo(3);
    }

    @Test
    void testCountByEmployeeId() {
        long count = leaveRequestRepository.countByEmployeeId(employee2.getId());
        assertThat(count).isEqualTo(3);
    }

    @Test
    void testCountByStatus() {
        long pendingCount = leaveRequestRepository.countByStatus(LeaveRequestStatus.PENDING);
        long approvedCount = leaveRequestRepository.countByStatus(LeaveRequestStatus.APPROVED);
        long rejectedCount = leaveRequestRepository.countByStatus(LeaveRequestStatus.REJECTED);
        
        assertThat(pendingCount).isEqualTo(6);
        assertThat(approvedCount).isEqualTo(6);
        assertThat(rejectedCount).isEqualTo(3);
    }

    @Test
    void testCountByEmployeeAndStatus() {
        long count = leaveRequestRepository.countByEmployeeAndStatus(employee3, LeaveRequestStatus.APPROVED);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void testCountByFromDateAfter() {
        LocalDate cutoff = baseDate.plusDays(20);
        long count = leaveRequestRepository.countByFromDateAfter(cutoff);
        assertThat(count).isEqualTo(4);
    }

    @Test
    void testCountByToDateBefore() {
        LocalDate cutoff = baseDate.plusDays(10);
        long count = leaveRequestRepository.countByToDateBefore(cutoff);
        assertThat(count).isEqualTo(5);
    }

    @Test
    void testCountByFromDateBetween() {
        LocalDate startDate = baseDate.plusDays(10);
        LocalDate endDate = baseDate.plusDays(20);
        long count = leaveRequestRepository.countByFromDateBetween(startDate, endDate);
        assertThat(count).isEqualTo(6);
    }

    @Test
    void testCountByToDateBetween() {
        LocalDate startDate = baseDate.plusDays(10);
        LocalDate endDate = baseDate.plusDays(20);
        long count = leaveRequestRepository.countByToDateBetween(startDate, endDate);
        assertThat(count).isEqualTo(5);
    }

    @Test
    void testCountLeaveRequestsInDateRange() {
        LocalDate startDate = baseDate.plusDays(16);
        LocalDate endDate = baseDate.plusDays(18);
        long count = leaveRequestRepository.countLeaveRequestsInDateRange(startDate, endDate);
        assertThat(count).isEqualTo(4);
    }

    @Test
    void testCountLeaveRequestsForEmployeeInDateRange() {
        LocalDate startDate = baseDate.plusDays(16);
        LocalDate endDate = baseDate.plusDays(18);
        long count = leaveRequestRepository.countLeaveRequestsForEmployeeInDateRange(employee2, startDate, endDate);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void testCountLeaveRequestsForEmployeeIdInDateRange() {
        LocalDate startDate = baseDate.plusDays(16);
        LocalDate endDate = baseDate.plusDays(18);
        long count = leaveRequestRepository.countLeaveRequestsForEmployeeIdInDateRange(employee4.getId(), startDate, endDate);
        assertThat(count).isEqualTo(1);
    }

    // ==================== RECENT LEAVE REQUESTS TESTS ====================

    @Test
    void testFindRecentLeaveRequestsByEmployee() {
        List<LeaveRequest> recentRequests = leaveRequestRepository.findTop10ByEmployeeOrderByFromDateDesc(employee1);
        
        assertThat(recentRequests).hasSize(3);
        assertThat(recentRequests.get(0)).isEqualTo(leaveRequest3); // Most recent
        assertThat(recentRequests.get(1)).isEqualTo(leaveRequest2);
        assertThat(recentRequests.get(2)).isEqualTo(leaveRequest1); // Oldest
    }

    @Test
    void testFindRecentLeaveRequestsByEmployeeId() {
        List<LeaveRequest> recentRequests = leaveRequestRepository.findTop10ByEmployeeIdOrderByFromDateDesc(employee2.getId());
        
        assertThat(recentRequests).hasSize(3);
        assertThat(recentRequests.get(0)).isEqualTo(leaveRequest6); // Most recent
        assertThat(recentRequests.get(1)).isEqualTo(leaveRequest5);
        assertThat(recentRequests.get(2)).isEqualTo(leaveRequest4); // Oldest
    }

    @Test
    void testFindRecentLeaveRequestsByStatus() {
        List<LeaveRequest> recentPendingRequests = leaveRequestRepository.findTop10ByStatusOrderByFromDateDesc(LeaveRequestStatus.PENDING);
        assertThat(recentPendingRequests).hasSize(6);
        assertThat(recentPendingRequests.get(0).getStatus()).isEqualTo(LeaveRequestStatus.PENDING);
        assertThat(recentPendingRequests.get(0).getEmployee().getId()).isEqualTo(employee2.getId());
        assertThat(recentPendingRequests.get(0).getFromDate()).isEqualTo(leaveRequest6.getFromDate());
        assertThat(recentPendingRequests.get(1).getStatus()).isEqualTo(LeaveRequestStatus.PENDING);
        assertThat(recentPendingRequests.get(1).getEmployee().getId()).isEqualTo(employee3.getId());
        assertThat(recentPendingRequests.get(1).getFromDate()).isEqualTo(leaveRequest9.getFromDate());
        assertThat(recentPendingRequests.get(2).getStatus()).isEqualTo(LeaveRequestStatus.PENDING);
        assertThat(recentPendingRequests.get(2).getEmployee().getId()).isEqualTo(employee5.getId());
        assertThat(recentPendingRequests.get(2).getFromDate()).isEqualTo(leaveRequest14.getFromDate());
        assertThat(recentPendingRequests.get(3).getStatus()).isEqualTo(LeaveRequestStatus.PENDING);
        assertThat(recentPendingRequests.get(3).getEmployee().getId()).isEqualTo(employee4.getId());
        assertThat(recentPendingRequests.get(3).getFromDate()).isEqualTo(leaveRequest10.getFromDate());
        assertThat(recentPendingRequests.get(4).getStatus()).isEqualTo(LeaveRequestStatus.PENDING);
        assertThat(recentPendingRequests.get(4).getEmployee().getId()).isEqualTo(employee2.getId());
        assertThat(recentPendingRequests.get(4).getFromDate()).isEqualTo(leaveRequest4.getFromDate());
        assertThat(recentPendingRequests.get(5).getStatus()).isEqualTo(LeaveRequestStatus.PENDING);
        assertThat(recentPendingRequests.get(5).getEmployee().getId()).isEqualTo(employee1.getId());
        assertThat(recentPendingRequests.get(5).getFromDate()).isEqualTo(leaveRequest1.getFromDate());
    }

    @Test
    void testFindRecentLeaveRequestsByEmployeeAndStatus() {
        List<LeaveRequest> recentRequests = leaveRequestRepository.findTop10ByEmployeeAndStatusOrderByFromDateDesc(employee3, LeaveRequestStatus.APPROVED);
        
        assertThat(recentRequests).hasSize(1);
        assertThat(recentRequests.get(0)).isEqualTo(leaveRequest7);
    }

    @Test
    void testFindRecentLeaveRequestsByEmployeeIdAndStatus() {
        List<LeaveRequest> recentRequests = leaveRequestRepository.findTop10ByEmployeeIdAndStatusOrderByFromDateDesc(employee4.getId(), LeaveRequestStatus.REJECTED);
        
        assertThat(recentRequests).hasSize(1);
        assertThat(recentRequests.get(0)).isEqualTo(leaveRequest12);
    }
}
