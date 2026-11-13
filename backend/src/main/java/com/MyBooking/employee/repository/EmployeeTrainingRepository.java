package com.MyBooking.employee.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import com.MyBooking.employee.domain.EmployeeTraining;
import com.MyBooking.employee.domain.TrainingStatus;
import com.MyBooking.auth.domain.User;
import com.MyBooking.employee.domain.Training;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmployeeTrainingRepository extends JpaRepository<EmployeeTraining, Long> {
    
    // ==================== BASIC QUERIES ====================
    
    // Find by employee
    List<EmployeeTraining> findByEmployee(User employee);
    List<EmployeeTraining> findByEmployeeId(Long employeeId);
    
    // Find by training
    List<EmployeeTraining> findByTraining(Training training);
    List<EmployeeTraining> findByTrainingId(Long trainingId);
    
    // Find by status
    List<EmployeeTraining> findByStatus(TrainingStatus status);
    
    // ==================== COMBINED QUERIES ====================
    
    // Find by employee and status
    List<EmployeeTraining> findByEmployeeAndStatus(User employee, TrainingStatus status);
    List<EmployeeTraining> findByEmployeeIdAndStatus(Long employeeId, TrainingStatus status);
    
    // Find by training and status
    List<EmployeeTraining> findByTrainingAndStatus(Training training, TrainingStatus status);
    List<EmployeeTraining> findByTrainingIdAndStatus(Long trainingId, TrainingStatus status);
    
    // Find by employee and training
    List<EmployeeTraining> findByEmployeeAndTraining(User employee, Training training);
    List<EmployeeTraining> findByEmployeeIdAndTrainingId(Long employeeId, Long trainingId);

    @Query("SELECT CASE WHEN COUNT(et) > 0 THEN true ELSE false END " +
           "FROM EmployeeTraining et JOIN et.training t " +
           "WHERE et.employee = :employee " +
           "AND et.status = com.MyBooking.employee.domain.TrainingStatus.IN_PROGRESS " +
           "AND t.startDate <= :toDate AND t.endDate >= :fromDate")
    boolean existsOverlappingInProgressTraining(
        @Param("employee") User employee,
        @Param("fromDate") java.time.LocalDate fromDate,
        @Param("toDate") java.time.LocalDate toDate
    );
    
    // ==================== DATE-BASED QUERIES ====================
    
    // Find by assignment date
    List<EmployeeTraining> findByAssignedAt(LocalDateTime assignedAt);
    List<EmployeeTraining> findByAssignedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    List<EmployeeTraining> findByAssignedAtAfter(LocalDateTime assignedAt);
    List<EmployeeTraining> findByAssignedAtBefore(LocalDateTime assignedAt);
    
    // Find by completion date
    List<EmployeeTraining> findByCompletedAt(LocalDateTime completedAt);
    List<EmployeeTraining> findByCompletedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    List<EmployeeTraining> findByCompletedAtAfter(LocalDateTime completedAt);
    List<EmployeeTraining> findByCompletedAtBefore(LocalDateTime completedAt);
    
    // Find completed trainings (has completion date)
    List<EmployeeTraining> findByCompletedAtIsNotNull();
    List<EmployeeTraining> findByCompletedAtIsNull();
    
    // ==================== COMPLEX QUERIES ====================
    
    // Find recent assignments
    @Query("SELECT et FROM EmployeeTraining et WHERE et.assignedAt >= :since ORDER BY et.assignedAt DESC")
    List<EmployeeTraining> findRecentAssignments(@Param("since") LocalDateTime since);
    
    // Find recent completions
    @Query("SELECT et FROM EmployeeTraining et WHERE et.completedAt >= :since ORDER BY et.completedAt DESC")
    List<EmployeeTraining> findRecentCompletions(@Param("since") LocalDateTime since);
    
    // Find overdue trainings (assigned but not completed after certain period)
    @Query("SELECT et FROM EmployeeTraining et WHERE et.status = 'ASSIGNED' AND et.assignedAt < :deadline")
    List<EmployeeTraining> findOverdueTrainings(@Param("deadline") LocalDateTime deadline);
    
    // Find trainings by employee with specific status and date range
    @Query("SELECT et FROM EmployeeTraining et WHERE et.employee = :employee AND et.status = :status AND et.assignedAt BETWEEN :startDate AND :endDate")
    List<EmployeeTraining> findByEmployeeStatusAndDateRange(@Param("employee") User employee, 
                                                           @Param("status") TrainingStatus status,
                                                           @Param("startDate") LocalDateTime startDate, 
                                                           @Param("endDate") LocalDateTime endDate);
    
    // ==================== EXISTENCE CHECKS ====================
    
    // Check if employee has specific training
    boolean existsByEmployeeAndTraining(User employee, Training training);
    boolean existsByEmployeeIdAndTrainingId(Long employeeId, Long trainingId);
    
    // Check if employee has training with specific status
    boolean existsByEmployeeAndStatus(User employee, TrainingStatus status);
    boolean existsByEmployeeIdAndStatus(Long employeeId, TrainingStatus status);
    
    // Check if training is assigned to any employee
    boolean existsByTraining(Training training);
    boolean existsByTrainingId(Long trainingId);
    
    // Check if training is assigned with specific status
    boolean existsByTrainingAndStatus(Training training, TrainingStatus status);
    boolean existsByTrainingIdAndStatus(Long trainingId, TrainingStatus status);
    
    // ==================== PAGINATION SUPPORT ====================
    
    // Paginated queries
    Page<EmployeeTraining> findByEmployee(User employee, Pageable pageable);
    Page<EmployeeTraining> findByEmployeeId(Long employeeId, Pageable pageable);
    Page<EmployeeTraining> findByTraining(Training training, Pageable pageable);
    Page<EmployeeTraining> findByTrainingId(Long trainingId, Pageable pageable);
    Page<EmployeeTraining> findByStatus(TrainingStatus status, Pageable pageable);
    Page<EmployeeTraining> findByEmployeeAndStatus(User employee, TrainingStatus status, Pageable pageable);
    Page<EmployeeTraining> findByEmployeeIdAndStatus(Long employeeId, TrainingStatus status, Pageable pageable);
    Page<EmployeeTraining> findByAssignedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    Page<EmployeeTraining> findByCompletedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    Page<EmployeeTraining> findByCompletedAtIsNotNull(Pageable pageable);
    Page<EmployeeTraining> findByCompletedAtIsNull(Pageable pageable);
    
    // ==================== STATISTICS AND COUNTS ====================
    
    // Count by employee
    long countByEmployee(User employee);
    long countByEmployeeId(Long employeeId);
    
    // Count by training
    long countByTraining(Training training);
    long countByTrainingId(Long trainingId);
    
    // Count by status
    long countByStatus(TrainingStatus status);
    
    // Count by employee and status
    long countByEmployeeAndStatus(User employee, TrainingStatus status);
    long countByEmployeeIdAndStatus(Long employeeId, TrainingStatus status);
    
    // Count by training and status
    long countByTrainingAndStatus(Training training, TrainingStatus status);
    long countByTrainingIdAndStatus(Long trainingId, TrainingStatus status);
    
    // Count by date ranges
    long countByAssignedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    long countByCompletedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    long countByAssignedAtAfter(LocalDateTime assignedAt);
    long countByAssignedAtBefore(LocalDateTime assignedAt);
    long countByCompletedAtAfter(LocalDateTime completedAt);
    long countByCompletedAtBefore(LocalDateTime completedAt);
    
    // Count completed vs assigned
    long countByCompletedAtIsNotNull();
    long countByCompletedAtIsNull();
    
    // ==================== CUSTOM BUSINESS QUERIES ====================
    
    // Find employees who completed specific training
    @Query("SELECT et.employee FROM EmployeeTraining et WHERE et.training = :training AND et.status = 'COMPLETED'")
    List<User> findEmployeesWhoCompletedTraining(@Param("training") Training training);
    
    // Find employees who completed specific training by ID
    @Query("SELECT et.employee FROM EmployeeTraining et WHERE et.training.id = :trainingId AND et.status = 'COMPLETED'")
    List<User> findEmployeesWhoCompletedTrainingById(@Param("trainingId") Long trainingId);
    
    // Find trainings assigned to specific employee
    @Query("SELECT et.training FROM EmployeeTraining et WHERE et.employee = :employee")
    List<Training> findTrainingsAssignedToEmployee(@Param("employee") User employee);
    
    // Find trainings assigned to specific employee by ID
    @Query("SELECT et.training FROM EmployeeTraining et WHERE et.employee.id = :employeeId")
    List<Training> findTrainingsAssignedToEmployeeById(@Param("employeeId") Long employeeId);
    
    // Find completed trainings for specific employee
    @Query("SELECT et.training FROM EmployeeTraining et WHERE et.employee = :employee AND et.status = 'COMPLETED'")
    List<Training> findCompletedTrainingsForEmployee(@Param("employee") User employee);
    
    // Find completed trainings for specific employee by ID
    @Query("SELECT et.training FROM EmployeeTraining et WHERE et.employee.id = :employeeId AND et.status = 'COMPLETED'")
    List<Training> findCompletedTrainingsForEmployeeById(@Param("employeeId") Long employeeId);
    
    // Find pending trainings for specific employee
    @Query("SELECT et.training FROM EmployeeTraining et WHERE et.employee = :employee AND et.status = 'ASSIGNED'")
    List<Training> findPendingTrainingsForEmployee(@Param("employee") User employee);
    
    // Find pending trainings for specific employee by ID
    @Query("SELECT et.training FROM EmployeeTraining et WHERE et.employee.id = :employeeId AND et.status = 'ASSIGNED'")
    List<Training> findPendingTrainingsForEmployeeById(@Param("employeeId") Long employeeId);
}
