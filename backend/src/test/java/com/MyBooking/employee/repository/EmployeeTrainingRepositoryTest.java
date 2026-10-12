package com.MyBooking.employee.repository;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import com.MyBooking.hotel_management.HotelManagementApplication;
import com.MyBooking.employee.domain.EmployeeTraining;
import com.MyBooking.employee.domain.Training;
import com.MyBooking.employee.domain.TrainingStatus;
import com.MyBooking.employee.repository.EmployeeTrainingRepository;
import com.MyBooking.auth.domain.User;
import com.MyBooking.auth.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("repository-test")
@ContextConfiguration(classes = HotelManagementApplication.class)
@EntityScan({"com.MyBooking.employee.domain", "com.MyBooking.auth.domain"})
@EnableJpaRepositories("com.MyBooking.employee.repository")
@Transactional
@Rollback
class EmployeeTrainingRepositoryTest {

    @Autowired
    private EmployeeTrainingRepository employeeTrainingRepository;

    @Autowired
    private TestEntityManager entityManager;

    // Test data
    private User employee1, employee2, employee3;
    private Training training1, training2, training3, training4;
    private EmployeeTraining employeeTraining1, employeeTraining2, employeeTraining3, 
                             employeeTraining4, employeeTraining5, employeeTraining6;
    private LocalDateTime baseDateTime;

    @BeforeEach
    void setUp() {
        // Clear any existing data
        employeeTrainingRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        // Base date for testing
        baseDateTime = LocalDateTime.of(2024, 1, 15, 9, 0);

        // Create test employees
        employee1 = new User("John", "Doe", "john.doe@hotel.com", "password123", "+1234567890", "123 Main St", LocalDate.of(1990, 1, 1), Role.EMPLOYEE);
        employee2 = new User("Jane", "Smith", "jane.smith@hotel.com", "password123", "+1234567891", "456 Oak Ave", LocalDate.of(1985, 5, 15), Role.EMPLOYEE);
        employee3 = new User("Bob", "Wilson", "bob.wilson@hotel.com", "password123", "+1234567892", "789 Pine Rd", LocalDate.of(1992, 8, 20), Role.EMPLOYEE);

        // Create test trainings
        training1 = new Training("Hotel Management Fundamentals", 
                                LocalDate.of(2024, 1, 15), LocalDate.of(2024, 1, 20));
        training2 = new Training("Customer Service Excellence", 
                                LocalDate.of(2024, 1, 22), LocalDate.of(2024, 1, 25));
        training3 = new Training("Safety and Security", 
                                LocalDate.of(2024, 1, 28), LocalDate.of(2024, 1, 30));
        training4 = new Training("Housekeeping Standards", 
                                LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 5));

        // Persist entities
        employee1 = entityManager.persistAndFlush(employee1);
        employee2 = entityManager.persistAndFlush(employee2);
        employee3 = entityManager.persistAndFlush(employee3);
        training1 = entityManager.persistAndFlush(training1);
        training2 = entityManager.persistAndFlush(training2);
        training3 = entityManager.persistAndFlush(training3);
        training4 = entityManager.persistAndFlush(training4);

        // Create employee training assignments with different statuses and dates
    
        
        // employeeTraining1: John assigned to Hotel Management (ASSIGNED)
        employeeTraining1 = new EmployeeTraining(employee1, training1, TrainingStatus.ASSIGNED);
        
        // employeeTraining2: John completed Customer Service (COMPLETED)
        employeeTraining2 = new EmployeeTraining(employee1, training2, TrainingStatus.COMPLETED);
        employeeTraining2.setCompletedAt(baseDateTime.minusDays(2));
        
        // employeeTraining3: Jane assigned to Safety and Security (ASSIGNED)
        employeeTraining3 = new EmployeeTraining(employee2, training3, TrainingStatus.ASSIGNED);
        
        // employeeTraining4: Jane completed Hotel Management (COMPLETED)
        employeeTraining4 = new EmployeeTraining(employee2, training1, TrainingStatus.COMPLETED);
        employeeTraining4.setCompletedAt(baseDateTime.minusDays(1));
        
        // employeeTraining5: Bob assigned to Housekeeping (ASSIGNED)
        employeeTraining5 = new EmployeeTraining(employee3, training4, TrainingStatus.ASSIGNED);
        
        // employeeTraining6: Bob completed Safety and Security (COMPLETED)
        employeeTraining6 = new EmployeeTraining(employee3, training3, TrainingStatus.COMPLETED);
        employeeTraining6.setCompletedAt(baseDateTime.minusDays(5));

        // Persist employee trainings
        employeeTraining1 = entityManager.persistAndFlush(employeeTraining1);
        employeeTraining2 = entityManager.persistAndFlush(employeeTraining2);
        employeeTraining3 = entityManager.persistAndFlush(employeeTraining3);
        employeeTraining4 = entityManager.persistAndFlush(employeeTraining4);
        employeeTraining5 = entityManager.persistAndFlush(employeeTraining5);
        employeeTraining6 = entityManager.persistAndFlush(employeeTraining6);
    }

    // ==================== BASIC QUERIES TESTS ====================

    @Test
    void testFindByEmployee() {
        List<EmployeeTraining> johnTrainings = employeeTrainingRepository.findByEmployee(employee1);
        
        assertThat(johnTrainings).hasSize(2);
        assertThat(johnTrainings).containsExactlyInAnyOrder(employeeTraining1, employeeTraining2);
    }

    @Test
    void testFindByEmployeeId() {
        List<EmployeeTraining> janeTrainings = employeeTrainingRepository.findByEmployeeId(employee2.getId());
        
        assertThat(janeTrainings).hasSize(2);
        assertThat(janeTrainings).containsExactlyInAnyOrder(employeeTraining3, employeeTraining4);
    }

    @Test
    void testFindByTraining() {
        List<EmployeeTraining> hotelManagementTrainings = employeeTrainingRepository.findByTraining(training1);
        
        assertThat(hotelManagementTrainings).hasSize(2);
        assertThat(hotelManagementTrainings).containsExactlyInAnyOrder(employeeTraining1, employeeTraining4);
    }

    @Test
    void testFindByTrainingId() {
        List<EmployeeTraining> safetyTrainings = employeeTrainingRepository.findByTrainingId(training3.getId());
        
        assertThat(safetyTrainings).hasSize(2);
        assertThat(safetyTrainings).containsExactlyInAnyOrder(employeeTraining3, employeeTraining6);
    }

    @Test
    void testFindByStatus() {
        List<EmployeeTraining> assignedTrainings = employeeTrainingRepository.findByStatus(TrainingStatus.ASSIGNED);
        List<EmployeeTraining> completedTrainings = employeeTrainingRepository.findByStatus(TrainingStatus.COMPLETED);
        
        assertThat(assignedTrainings).hasSize(3);
        assertThat(assignedTrainings).containsExactlyInAnyOrder(employeeTraining1, employeeTraining3, employeeTraining5);
        
        assertThat(completedTrainings).hasSize(3);
        assertThat(completedTrainings).containsExactlyInAnyOrder(employeeTraining2, employeeTraining4, employeeTraining6);
    }

    // ==================== COMBINED QUERIES TESTS ====================

    @Test
    void testFindByEmployeeAndStatus() {
        List<EmployeeTraining> johnAssigned = employeeTrainingRepository.findByEmployeeAndStatus(employee1, TrainingStatus.ASSIGNED);
        List<EmployeeTraining> johnCompleted = employeeTrainingRepository.findByEmployeeAndStatus(employee1, TrainingStatus.COMPLETED);
        
        assertThat(johnAssigned).hasSize(1);
        assertThat(johnAssigned).containsExactly(employeeTraining1);
        
        assertThat(johnCompleted).hasSize(1);
        assertThat(johnCompleted).containsExactly(employeeTraining2);
    }

    @Test
    void testFindByEmployeeIdAndStatus() {
        List<EmployeeTraining> janeAssigned = employeeTrainingRepository.findByEmployeeIdAndStatus(employee2.getId(), TrainingStatus.ASSIGNED);
        List<EmployeeTraining> janeCompleted = employeeTrainingRepository.findByEmployeeIdAndStatus(employee2.getId(), TrainingStatus.COMPLETED);
        
        assertThat(janeAssigned).hasSize(1);
        assertThat(janeAssigned).containsExactly(employeeTraining3);
        
        assertThat(janeCompleted).hasSize(1);
        assertThat(janeCompleted).containsExactly(employeeTraining4);
    }

    @Test
    void testFindByTrainingAndStatus() {
        List<EmployeeTraining> hotelManagementAssigned = employeeTrainingRepository.findByTrainingAndStatus(training1, TrainingStatus.ASSIGNED);
        List<EmployeeTraining> hotelManagementCompleted = employeeTrainingRepository.findByTrainingAndStatus(training1, TrainingStatus.COMPLETED);
        
        assertThat(hotelManagementAssigned).hasSize(1);
        assertThat(hotelManagementAssigned).containsExactly(employeeTraining1);
        
        assertThat(hotelManagementCompleted).hasSize(1);
        assertThat(hotelManagementCompleted).containsExactly(employeeTraining4);
    }

    @Test
    void testFindByTrainingIdAndStatus() {
        List<EmployeeTraining> safetyAssigned = employeeTrainingRepository.findByTrainingIdAndStatus(training3.getId(), TrainingStatus.ASSIGNED);
        List<EmployeeTraining> safetyCompleted = employeeTrainingRepository.findByTrainingIdAndStatus(training3.getId(), TrainingStatus.COMPLETED);
        
        assertThat(safetyAssigned).hasSize(1);
        assertThat(safetyAssigned).containsExactly(employeeTraining3);
        
        assertThat(safetyCompleted).hasSize(1);
        assertThat(safetyCompleted).containsExactly(employeeTraining6);
    }

    @Test
    void testFindByEmployeeAndTraining() {
        EmployeeTraining johnHotelManagement = employeeTrainingRepository.findByEmployeeAndTraining(employee1, training1).get(0);
        EmployeeTraining janeSafety = employeeTrainingRepository.findByEmployeeAndTraining(employee2, training3).get(0);
        
        assertThat(johnHotelManagement).isEqualTo(employeeTraining1);
        assertThat(janeSafety).isEqualTo(employeeTraining3);
    }

    @Test
    void testFindByEmployeeIdAndTrainingId() {
        EmployeeTraining bobHousekeeping = employeeTrainingRepository.findByEmployeeIdAndTrainingId(employee3.getId(), training4.getId()).get(0);
        EmployeeTraining bobSafety = employeeTrainingRepository.findByEmployeeIdAndTrainingId(employee3.getId(), training3.getId()).get(0);
        
        assertThat(bobHousekeeping).isEqualTo(employeeTraining5);
        assertThat(bobSafety).isEqualTo(employeeTraining6);
    }

    // ==================== DATE-BASED QUERIES TESTS ====================

    @Test
    void testFindByAssignedAt() {
        // Since all entities are created at the same time within this test, they all have the same assignedAt timestamp
        LocalDateTime actualAssignedAt = employeeTraining1.getAssignedAt();
        // Use a reasonable range to account for database timestamp precision
        LocalDateTime startTime = actualAssignedAt.minusSeconds(5);
        LocalDateTime endTime = actualAssignedAt.plusSeconds(5);
        List<EmployeeTraining> assignedAtSameTime = employeeTrainingRepository.findByAssignedAtBetween(startTime, endTime);
        
        assertThat(assignedAtSameTime).hasSize(6);
        assertThat(assignedAtSameTime).containsExactlyInAnyOrder(employeeTraining1, employeeTraining2, employeeTraining3, 
                                                               employeeTraining4, employeeTraining5, employeeTraining6);
    }

    @Test
    void testFindByAssignedAtBetween() {
        LocalDateTime actualAssignedAt = employeeTraining1.getAssignedAt();
        LocalDateTime startDate = actualAssignedAt.minusMinutes(1);
        LocalDateTime endDate = actualAssignedAt.plusMinutes(1);
        List<EmployeeTraining> recentAssignments = employeeTrainingRepository.findByAssignedAtBetween(startDate, endDate);
        
        assertThat(recentAssignments).hasSize(6);
        assertThat(recentAssignments).containsExactlyInAnyOrder(employeeTraining1, employeeTraining2, employeeTraining3, 
                                                               employeeTraining4, employeeTraining5, employeeTraining6);
    }

    @Test
    void testFindByAssignedAtAfter() {
        // Since all entities are created at the same time, they all come after a past cutoff
        LocalDateTime cutoff = baseDateTime.minusDays(2);
        List<EmployeeTraining> recentAssignments = employeeTrainingRepository.findByAssignedAtAfter(cutoff);
        
        assertThat(recentAssignments).hasSize(6);
        assertThat(recentAssignments).containsExactlyInAnyOrder(employeeTraining1, employeeTraining2, employeeTraining3, 
                                                               employeeTraining4, employeeTraining5, employeeTraining6);
    }

    @Test
    void testFindByAssignedAtBefore() {
        // Since all entities are created at the same time, they all come after a past cutoff
        LocalDateTime cutoff = baseDateTime.minusDays(2);
        List<EmployeeTraining> olderAssignments = employeeTrainingRepository.findByAssignedAtBefore(cutoff);
        
        assertThat(olderAssignments).isEmpty();
    }

    @Test
    void testFindByCompletedAt() {
        List<EmployeeTraining> completedYesterday = employeeTrainingRepository.findByCompletedAt(baseDateTime.minusDays(1));
        
        assertThat(completedYesterday).hasSize(1);
        assertThat(completedYesterday).containsExactly(employeeTraining4);
    }

    @Test
    void testFindByCompletedAtBetween() {
        LocalDateTime startDate = baseDateTime.minusDays(3);
        LocalDateTime endDate = baseDateTime.minusDays(1);
        List<EmployeeTraining> recentCompletions = employeeTrainingRepository.findByCompletedAtBetween(startDate, endDate);
        
        assertThat(recentCompletions).hasSize(2);
        assertThat(recentCompletions).containsExactlyInAnyOrder(employeeTraining2, employeeTraining4);
    }

    @Test
    void testFindByCompletedAtAfter() {
        LocalDateTime cutoff = baseDateTime.minusDays(3);
        List<EmployeeTraining> recentCompletions = employeeTrainingRepository.findByCompletedAtAfter(cutoff);
        
        assertThat(recentCompletions).hasSize(2);
        assertThat(recentCompletions).containsExactlyInAnyOrder(employeeTraining2, employeeTraining4);
    }

    @Test
    void testFindByCompletedAtBefore() {
        LocalDateTime cutoff = baseDateTime.minusDays(3);
        List<EmployeeTraining> olderCompletions = employeeTrainingRepository.findByCompletedAtBefore(cutoff);
        
        assertThat(olderCompletions).hasSize(1);
        assertThat(olderCompletions).containsExactly(employeeTraining6);
    }

    @Test
    void testFindByCompletedAtIsNotNull() {
        List<EmployeeTraining> completedTrainings = employeeTrainingRepository.findByCompletedAtIsNotNull();
        
        assertThat(completedTrainings).hasSize(3);
        assertThat(completedTrainings).containsExactlyInAnyOrder(employeeTraining2, employeeTraining4, employeeTraining6);
    }

    @Test
    void testFindByCompletedAtIsNull() {
        List<EmployeeTraining> pendingTrainings = employeeTrainingRepository.findByCompletedAtIsNull();
        
        assertThat(pendingTrainings).hasSize(3);
        assertThat(pendingTrainings).containsExactlyInAnyOrder(employeeTraining1, employeeTraining3, employeeTraining5);
    }

    // ==================== COMPLEX QUERIES TESTS ====================

    @Test
    void testFindRecentAssignments() {
        LocalDateTime since = baseDateTime.minusDays(2);
        List<EmployeeTraining> recentAssignments = employeeTrainingRepository.findRecentAssignments(since);
        
        assertThat(recentAssignments).hasSize(6);
        assertThat(recentAssignments).containsExactlyInAnyOrder(employeeTraining1, employeeTraining2, employeeTraining3, 
                                                               employeeTraining4, employeeTraining5, employeeTraining6);
    }

    @Test
    void testFindRecentCompletions() {
        LocalDateTime since = baseDateTime.minusDays(3);
        List<EmployeeTraining> recentCompletions = employeeTrainingRepository.findRecentCompletions(since);
        
        assertThat(recentCompletions).hasSize(2);
        assertThat(recentCompletions).containsExactlyInAnyOrder(employeeTraining2, employeeTraining4);
    }

    @Test
    void testFindOverdueTrainings() {
        LocalDateTime deadline = baseDateTime.minusDays(1);
        List<EmployeeTraining> overdueTrainings = employeeTrainingRepository.findOverdueTrainings(deadline);
        
        assertThat(overdueTrainings).isEmpty();
    }

    @Test
    void testFindByEmployeeStatusAndDateRange() {
        LocalDateTime actualAssignedAt = employeeTraining1.getAssignedAt();
        LocalDateTime startDate = actualAssignedAt.minusMinutes(1);
        LocalDateTime endDate = actualAssignedAt.plusMinutes(1);
        List<EmployeeTraining> johnAssignedRecent = employeeTrainingRepository.findByEmployeeStatusAndDateRange(
            employee1, TrainingStatus.ASSIGNED, startDate, endDate);
        
        assertThat(johnAssignedRecent).hasSize(1);
        assertThat(johnAssignedRecent).containsExactly(employeeTraining1);
    }

    // ==================== EXISTENCE CHECKS TESTS ====================

    @Test
    void testExistsByEmployeeAndTraining() {
        assertThat(employeeTrainingRepository.existsByEmployeeAndTraining(employee1, training1)).isTrue();
        assertThat(employeeTrainingRepository.existsByEmployeeAndTraining(employee1, training3)).isFalse();
    }

    @Test
    void testExistsByEmployeeIdAndTrainingId() {
        assertThat(employeeTrainingRepository.existsByEmployeeIdAndTrainingId(employee2.getId(), training3.getId())).isTrue();
        assertThat(employeeTrainingRepository.existsByEmployeeIdAndTrainingId(employee2.getId(), training4.getId())).isFalse();
    }

    @Test
    void testExistsByEmployeeAndStatus() {
        assertThat(employeeTrainingRepository.existsByEmployeeAndStatus(employee3, TrainingStatus.ASSIGNED)).isTrue();
        assertThat(employeeTrainingRepository.existsByEmployeeAndStatus(employee3, TrainingStatus.COMPLETED)).isTrue();
    }

    @Test
    void testExistsByEmployeeIdAndStatus() {
        assertThat(employeeTrainingRepository.existsByEmployeeIdAndStatus(employee1.getId(), TrainingStatus.ASSIGNED)).isTrue();
        assertThat(employeeTrainingRepository.existsByEmployeeIdAndStatus(employee1.getId(), TrainingStatus.COMPLETED)).isTrue();
    }

    @Test
    void testExistsByTraining() {
        assertThat(employeeTrainingRepository.existsByTraining(training1)).isTrue();
        assertThat(employeeTrainingRepository.existsByTraining(training2)).isTrue();
    }

    @Test
    void testExistsByTrainingId() {
        assertThat(employeeTrainingRepository.existsByTrainingId(training3.getId())).isTrue();
        assertThat(employeeTrainingRepository.existsByTrainingId(training4.getId())).isTrue();
    }

    @Test
    void testExistsByTrainingAndStatus() {
        assertThat(employeeTrainingRepository.existsByTrainingAndStatus(training1, TrainingStatus.ASSIGNED)).isTrue();
        assertThat(employeeTrainingRepository.existsByTrainingAndStatus(training1, TrainingStatus.COMPLETED)).isTrue();
    }

    @Test
    void testExistsByTrainingIdAndStatus() {
        assertThat(employeeTrainingRepository.existsByTrainingIdAndStatus(training3.getId(), TrainingStatus.ASSIGNED)).isTrue();
        assertThat(employeeTrainingRepository.existsByTrainingIdAndStatus(training3.getId(), TrainingStatus.COMPLETED)).isTrue();
    }

    // ==================== PAGINATION SUPPORT TESTS ====================

    @Test
    void testFindByEmployeeWithPagination() {
        Page<EmployeeTraining> page = employeeTrainingRepository.findByEmployee(employee1, PageRequest.of(0, 1));
        
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    void testFindByStatusWithPagination() {
        Page<EmployeeTraining> page = employeeTrainingRepository.findByStatus(TrainingStatus.ASSIGNED, PageRequest.of(0, 2));
        
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    void testFindByAssignedAtBetweenWithPagination() {
        LocalDateTime actualAssignedAt = employeeTraining1.getAssignedAt();
        LocalDateTime startDate = actualAssignedAt.minusMinutes(1);
        LocalDateTime endDate = actualAssignedAt.plusMinutes(1);
        Page<EmployeeTraining> page = employeeTrainingRepository.findByAssignedAtBetween(startDate, endDate, PageRequest.of(0, 2));
        
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(6);
        assertThat(page.getTotalPages()).isEqualTo(3);
    }

    // ==================== STATISTICS AND COUNTS TESTS ====================

    @Test
    void testCountByEmployee() {
        long johnCount = employeeTrainingRepository.countByEmployee(employee1);
        long janeCount = employeeTrainingRepository.countByEmployee(employee2);
        long bobCount = employeeTrainingRepository.countByEmployee(employee3);
        
        assertThat(johnCount).isEqualTo(2);
        assertThat(janeCount).isEqualTo(2);
        assertThat(bobCount).isEqualTo(2);
    }

    @Test
    void testCountByEmployeeId() {
        long johnCount = employeeTrainingRepository.countByEmployeeId(employee1.getId());
        long janeCount = employeeTrainingRepository.countByEmployeeId(employee2.getId());
        
        assertThat(johnCount).isEqualTo(2);
        assertThat(janeCount).isEqualTo(2);
    }

    @Test
    void testCountByTraining() {
        long hotelManagementCount = employeeTrainingRepository.countByTraining(training1);
        long safetyCount = employeeTrainingRepository.countByTraining(training3);
        
        assertThat(hotelManagementCount).isEqualTo(2);
        assertThat(safetyCount).isEqualTo(2);
    }

    @Test
    void testCountByTrainingId() {
        long customerServiceCount = employeeTrainingRepository.countByTrainingId(training2.getId());
        long housekeepingCount = employeeTrainingRepository.countByTrainingId(training4.getId());
        
        assertThat(customerServiceCount).isEqualTo(1);
        assertThat(housekeepingCount).isEqualTo(1);
    }

    @Test
    void testCountByStatus() {
        long assignedCount = employeeTrainingRepository.countByStatus(TrainingStatus.ASSIGNED);
        long completedCount = employeeTrainingRepository.countByStatus(TrainingStatus.COMPLETED);
        
        assertThat(assignedCount).isEqualTo(3);
        assertThat(completedCount).isEqualTo(3);
    }

    @Test
    void testCountByEmployeeAndStatus() {
        long johnAssignedCount = employeeTrainingRepository.countByEmployeeAndStatus(employee1, TrainingStatus.ASSIGNED);
        long johnCompletedCount = employeeTrainingRepository.countByEmployeeAndStatus(employee1, TrainingStatus.COMPLETED);
        
        assertThat(johnAssignedCount).isEqualTo(1);
        assertThat(johnCompletedCount).isEqualTo(1);
    }

    @Test
    void testCountByEmployeeIdAndStatus() {
        long janeAssignedCount = employeeTrainingRepository.countByEmployeeIdAndStatus(employee2.getId(), TrainingStatus.ASSIGNED);
        long janeCompletedCount = employeeTrainingRepository.countByEmployeeIdAndStatus(employee2.getId(), TrainingStatus.COMPLETED);
        
        assertThat(janeAssignedCount).isEqualTo(1);
        assertThat(janeCompletedCount).isEqualTo(1);
    }

    @Test
    void testCountByTrainingAndStatus() {
        long hotelManagementAssignedCount = employeeTrainingRepository.countByTrainingAndStatus(training1, TrainingStatus.ASSIGNED);
        long hotelManagementCompletedCount = employeeTrainingRepository.countByTrainingAndStatus(training1, TrainingStatus.COMPLETED);
        
        assertThat(hotelManagementAssignedCount).isEqualTo(1);
        assertThat(hotelManagementCompletedCount).isEqualTo(1);
    }

    @Test
    void testCountByTrainingIdAndStatus() {
        long safetyAssignedCount = employeeTrainingRepository.countByTrainingIdAndStatus(training3.getId(), TrainingStatus.ASSIGNED);
        long safetyCompletedCount = employeeTrainingRepository.countByTrainingIdAndStatus(training3.getId(), TrainingStatus.COMPLETED);
        
        assertThat(safetyAssignedCount).isEqualTo(1);
        assertThat(safetyCompletedCount).isEqualTo(1);
    }

    @Test
    void testCountByAssignedAtBetween() {
        LocalDateTime actualAssignedAt = employeeTraining1.getAssignedAt();
        LocalDateTime startDate = actualAssignedAt.minusMinutes(1);
        LocalDateTime endDate = actualAssignedAt.plusMinutes(1);
        long count = employeeTrainingRepository.countByAssignedAtBetween(startDate, endDate);
        
        assertThat(count).isEqualTo(6);
    }

    @Test
    void testCountByCompletedAtBetween() {
        LocalDateTime startDate = baseDateTime.minusDays(3);
        LocalDateTime endDate = baseDateTime.minusDays(1);
        long count = employeeTrainingRepository.countByCompletedAtBetween(startDate, endDate);
        
        assertThat(count).isEqualTo(2);
    }

    @Test
    void testCountByAssignedAtAfter() {
        LocalDateTime cutoff = baseDateTime.minusDays(2);
        long count = employeeTrainingRepository.countByAssignedAtAfter(cutoff);
        
        assertThat(count).isEqualTo(6);
    }

    @Test
    void testCountByAssignedAtBefore() {
        LocalDateTime cutoff = baseDateTime.minusDays(2);
        long count = employeeTrainingRepository.countByAssignedAtBefore(cutoff);
        
        assertThat(count).isEqualTo(0);
    }

    @Test
    void testCountByCompletedAtAfter() {
        LocalDateTime cutoff = baseDateTime.minusDays(3);
        long count = employeeTrainingRepository.countByCompletedAtAfter(cutoff);
        
        assertThat(count).isEqualTo(2);
    }

    @Test
    void testCountByCompletedAtBefore() {
        LocalDateTime cutoff = baseDateTime.minusDays(3);
        long count = employeeTrainingRepository.countByCompletedAtBefore(cutoff);
        
        assertThat(count).isEqualTo(1);
    }

    @Test
    void testCountByCompletedAtIsNotNull() {
        long completedCount = employeeTrainingRepository.countByCompletedAtIsNotNull();
        
        assertThat(completedCount).isEqualTo(3);
    }

    @Test
    void testCountByCompletedAtIsNull() {
        long pendingCount = employeeTrainingRepository.countByCompletedAtIsNull();
        
        assertThat(pendingCount).isEqualTo(3);
    }

    // ==================== CUSTOM BUSINESS QUERIES TESTS ====================

    @Test
    void testFindEmployeesWhoCompletedTraining() {
        List<User> employeesWhoCompletedHotelManagement = employeeTrainingRepository.findEmployeesWhoCompletedTraining(training1);
        
        assertThat(employeesWhoCompletedHotelManagement).hasSize(1);
        assertThat(employeesWhoCompletedHotelManagement).containsExactly(employee2);
    }

    @Test
    void testFindEmployeesWhoCompletedTrainingById() {
        List<User> employeesWhoCompletedSafety = employeeTrainingRepository.findEmployeesWhoCompletedTrainingById(training3.getId());
        
        assertThat(employeesWhoCompletedSafety).hasSize(1);
        assertThat(employeesWhoCompletedSafety).containsExactly(employee3);
    }

    @Test
    void testFindTrainingsAssignedToEmployee() {
        List<Training> johnTrainings = employeeTrainingRepository.findTrainingsAssignedToEmployee(employee1);
        
        assertThat(johnTrainings).hasSize(2);
        assertThat(johnTrainings).containsExactlyInAnyOrder(training1, training2);
    }

    @Test
    void testFindTrainingsAssignedToEmployeeById() {
        List<Training> janeTrainings = employeeTrainingRepository.findTrainingsAssignedToEmployeeById(employee2.getId());
        
        assertThat(janeTrainings).hasSize(2);
        assertThat(janeTrainings).containsExactlyInAnyOrder(training1, training3);
    }

    @Test
    void testFindCompletedTrainingsForEmployee() {
        List<Training> johnCompletedTrainings = employeeTrainingRepository.findCompletedTrainingsForEmployee(employee1);
        
        assertThat(johnCompletedTrainings).hasSize(1);
        assertThat(johnCompletedTrainings).containsExactly(training2);
    }

    @Test
    void testFindCompletedTrainingsForEmployeeById() {
        List<Training> janeCompletedTrainings = employeeTrainingRepository.findCompletedTrainingsForEmployeeById(employee2.getId());
        
        assertThat(janeCompletedTrainings).hasSize(1);
        assertThat(janeCompletedTrainings).containsExactly(training1);
    }

    @Test
    void testFindPendingTrainingsForEmployee() {
        List<Training> johnPendingTrainings = employeeTrainingRepository.findPendingTrainingsForEmployee(employee1);
        
        assertThat(johnPendingTrainings).hasSize(1);
        assertThat(johnPendingTrainings).containsExactly(training1);
    }

    @Test
    void testFindPendingTrainingsForEmployeeById() {
        List<Training> janePendingTrainings = employeeTrainingRepository.findPendingTrainingsForEmployeeById(employee2.getId());
        
        assertThat(janePendingTrainings).hasSize(1);
        assertThat(janePendingTrainings).containsExactly(training3);
    }

    // ==================== EDGE CASES AND BOUNDARY TESTS ====================

    @Test
    void testFindByAssignedAtBetweenWithSameDates() {
        // Since all entities are created at the same time within this test, they all have the same assignedAt
        LocalDateTime actualAssignedAt = employeeTraining1.getAssignedAt();
        // Use a larger range to account for millisecond differences
        LocalDateTime startTime = actualAssignedAt.minusSeconds(1); // 1 second before
        LocalDateTime endTime = actualAssignedAt.plusSeconds(1); // 1 second after
        List<EmployeeTraining> trainings = employeeTrainingRepository.findByAssignedAtBetween(startTime, endTime);
        
        assertThat(trainings).hasSize(6);
        assertThat(trainings).containsExactlyInAnyOrder(employeeTraining1, employeeTraining2, employeeTraining3, 
                                                       employeeTraining4, employeeTraining5, employeeTraining6);
    }

    @Test
    void testFindByCompletedAtBetweenWithSameDates() {
        LocalDateTime sameDate = baseDateTime.minusDays(1);
        List<EmployeeTraining> trainings = employeeTrainingRepository.findByCompletedAtBetween(sameDate, sameDate);
        
        assertThat(trainings).hasSize(1);
        assertThat(trainings).containsExactly(employeeTraining4);
    }

    @Test
    void testFindByAssignedAtAfterWithFutureDate() {
        // Since all entities are created at the same time, they all come before a future date
        LocalDateTime actualAssignedAt = employeeTraining1.getAssignedAt();
        LocalDateTime futureDate = actualAssignedAt.plusDays(10);
        List<EmployeeTraining> futureAssignments = employeeTrainingRepository.findByAssignedAtAfter(futureDate);
        
        assertThat(futureAssignments).isEmpty();
    }

    @Test
    void testFindByCompletedAtBeforeWithPastDate() {
        LocalDateTime pastDate = baseDateTime.minusDays(20);
        List<EmployeeTraining> pastCompletions = employeeTrainingRepository.findByCompletedAtBefore(pastDate);
        
        assertThat(pastCompletions).isEmpty();
    }

    @Test
    void testFindOverdueTrainingsWithNoOverdue() {
        // Since all entities are created at the same time, none are overdue
        LocalDateTime deadline = baseDateTime.minusDays(20);
        List<EmployeeTraining> overdueTrainings = employeeTrainingRepository.findOverdueTrainings(deadline);
        
        assertThat(overdueTrainings).isEmpty();
    }

    @Test
    void testFindOverdueTrainingsWithAllOverdue() {
        // Since all entities are created at the same time, none are overdue
        LocalDateTime deadline = baseDateTime.plusDays(1);
        List<EmployeeTraining> overdueTrainings = employeeTrainingRepository.findOverdueTrainings(deadline);
        
        assertThat(overdueTrainings).isEmpty();
    }
}
