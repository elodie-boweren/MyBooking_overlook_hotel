package com.MyBooking.employee.service;

import com.MyBooking.auth.domain.User;
import com.MyBooking.auth.domain.Role;
import com.MyBooking.auth.repository.UserRepository;
import com.MyBooking.common.exception.BusinessRuleException;
import com.MyBooking.common.exception.NotFoundException;
import com.MyBooking.employee.domain.*;
import com.MyBooking.employee.repository.*;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeTaskRepository employeeTaskRepository;

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private TrainingRepository trainingRepository;

    @Mock
    private EmployeeTrainingRepository employeeTrainingRepository;

    @Mock
    private ShiftRepository shiftRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EmployeeService employeeService;

    private User testUser;
    private Employee testEmployee;
    private EmployeeTask testTask;
    private LeaveRequest testLeaveRequest;
    private Training testTraining;
    private EmployeeTraining testEmployeeTraining;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("employee@hotel.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setRole(Role.EMPLOYEE);

        // Setup test employee
        testEmployee = new Employee();
        testEmployee.setUserId(1L);
        testEmployee.setUser(testUser);
        testEmployee.setStatus(EmployeeStatus.ACTIVE);
        testEmployee.setJobTitle("Receptionist");

        // Setup test task
        testTask = new EmployeeTask();
        testTask.setId(1L);
        testTask.setEmployee(testUser);
        testTask.setTitle("Clean Room 101");
        testTask.setDescription("Deep clean room 101");
        testTask.setStatus(TaskStatus.TODO);
        testTask.setCreatedAt(LocalDateTime.now());

        // Setup test leave request
        testLeaveRequest = new LeaveRequest();
        testLeaveRequest.setId(1L);
        testLeaveRequest.setEmployee(testUser);
        testLeaveRequest.setFromDate(LocalDate.now().plusDays(7));
        testLeaveRequest.setToDate(LocalDate.now().plusDays(10));
        testLeaveRequest.setStatus(LeaveRequestStatus.PENDING);
        testLeaveRequest.setReason("Vacation");

        // Setup test training
        testTraining = new Training();
        testTraining.setId(1L);
        testTraining.setTitle("Customer Service Training");
        testTraining.setStartDate(LocalDate.now().plusDays(1));
        testTraining.setEndDate(LocalDate.now().plusDays(3));

        // Setup test employee training
        testEmployeeTraining = new EmployeeTraining();
        testEmployeeTraining.setEmployee(testUser);
        testEmployeeTraining.setTraining(testTraining);
        testEmployeeTraining.setStatus(TrainingStatus.ASSIGNED);
    }

    // ========== EMPLOYEE MANAGEMENT TESTS ==========

    @Test
    void createEmployee_WithValidData_ShouldCreateEmployee() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(employeeRepository.existsByUserId(1L)).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenReturn(testEmployee);

        // When
        Employee result = employeeService.createEmployee(1L, "Receptionist");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(testUser);
        assertThat(result.getJobTitle()).isEqualTo("Receptionist");
        assertThat(result.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);

        verify(userRepository).findById(1L);
        verify(employeeRepository).existsByUserId(1L);
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    void createEmployee_WithNonExistentUser_ShouldThrowNotFoundException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> employeeService.createEmployee(999L, "Receptionist"))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("User not found with ID: 999");

        verify(userRepository).findById(999L);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void createEmployee_WithExistingEmployee_ShouldThrowBusinessRuleException() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(employeeRepository.existsByUserId(1L)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> employeeService.createEmployee(1L, "Receptionist"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("User is already an employee");

        verify(userRepository).findById(1L);
        verify(employeeRepository).existsByUserId(1L);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void updateEmployee_WithValidData_ShouldUpdateEmployee() {
        // Given
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(testEmployee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(testEmployee);

        // When
        Employee result = employeeService.updateEmployee(1L, "Senior Receptionist", EmployeeStatus.ACTIVE);

        // Then
        assertThat(result).isNotNull();
        verify(employeeRepository).findByUserId(1L);
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    void deactivateEmployee_WithValidEmployee_ShouldDeactivateEmployee() {
        // Given
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(testEmployee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(testEmployee);

        // When
        employeeService.deactivateEmployee(1L, "End of contract");

        // Then
        verify(employeeRepository).findByUserId(1L);
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    void getEmployeeByUserId_WithValidId_ShouldReturnEmployee() {
        // Given
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(testEmployee));

        // When
        Employee result = employeeService.getEmployeeByUserId(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getJobTitle()).isEqualTo("Receptionist");

        verify(employeeRepository).findByUserId(1L);
    }

    @Test
    void getEmployeeByUserId_WithNonExistentId_ShouldThrowNotFoundException() {
        // Given
        when(employeeRepository.findByUserId(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> employeeService.getEmployeeByUserId(999L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Employee not found with user ID: 999");

        verify(employeeRepository).findByUserId(999L);
    }

    @Test
    void searchEmployees_WithValidCriteria_ShouldReturnEmployees() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Employee> expectedPage = new PageImpl<>(Arrays.asList(testEmployee), pageable, 1);
        when(employeeRepository.findByStatusAndJobTitle(EmployeeStatus.ACTIVE, "Receptionist", pageable))
            .thenReturn(expectedPage);

        // When
        Page<Employee> result = employeeService.searchEmployees(EmployeeStatus.ACTIVE, "Receptionist", pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(EmployeeStatus.ACTIVE);

        verify(employeeRepository).findByStatusAndJobTitle(EmployeeStatus.ACTIVE, "Receptionist", pageable);
    }

    @Test
    void getActiveEmployeeCount_ShouldReturnCorrectCount() {
        // Given
        when(employeeRepository.countByStatus(EmployeeStatus.ACTIVE)).thenReturn(5L);

        // When
        long result = employeeService.getActiveEmployeeCount();

        // Then
        assertThat(result).isEqualTo(5L);
        verify(employeeRepository).countByStatus(EmployeeStatus.ACTIVE);
    }

    // ========== TASK MANAGEMENT TESTS ==========

    @Test
    void createTask_WithValidData_ShouldCreateTask() {
        // Given
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(testEmployee));
        when(employeeTrainingRepository.existsByEmployeeAndStatus(testUser, TrainingStatus.IN_PROGRESS))
            .thenReturn(false);
        when(employeeTaskRepository.save(any(EmployeeTask.class))).thenReturn(testTask);

        // When
        EmployeeTask result = employeeService.createTask(1L, "Clean Room 101", "Deep clean room 101");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Clean Room 101");
        assertThat(result.getDescription()).isEqualTo("Deep clean room 101");
        assertThat(result.getStatus()).isEqualTo(TaskStatus.TODO);

        verify(employeeRepository, times(2)).findByUserId(1L); // Called twice: once in createTask, once in hasInProgressTraining
        verify(employeeTrainingRepository).existsByEmployeeAndStatus(testUser, TrainingStatus.IN_PROGRESS);
        verify(employeeTaskRepository).save(any(EmployeeTask.class));
    }

    @Test
    void createTask_WithInactiveEmployee_ShouldThrowBusinessRuleException() {
        // Given
        testEmployee.setStatus(EmployeeStatus.INACTIVE);
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(testEmployee));

        // When & Then
        assertThatThrownBy(() -> employeeService.createTask(1L, "Clean Room 101", "Deep clean room 101"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Cannot assign task to inactive employee");

        verify(employeeRepository).findByUserId(1L);
        verify(employeeTaskRepository, never()).save(any());
    }

    @Test
    void createTask_WithEmployeeInTraining_ShouldThrowBusinessRuleException() {
        // Given
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(testEmployee));
        when(employeeTrainingRepository.existsByEmployeeAndStatus(testUser, TrainingStatus.IN_PROGRESS))
            .thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> employeeService.createTask(1L, "Clean Room 101", "Deep clean room 101"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Cannot assign task to employee currently in training");

        verify(employeeRepository, times(2)).findByUserId(1L); // Called twice: once in createTask, once in hasInProgressTraining
        verify(employeeTrainingRepository).existsByEmployeeAndStatus(testUser, TrainingStatus.IN_PROGRESS);
        verify(employeeTaskRepository, never()).save(any());
    }

    @Test
    void updateTaskStatus_WithValidTask_ShouldUpdateTask() {
        // Given
        when(employeeTaskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(employeeTaskRepository.save(any(EmployeeTask.class))).thenReturn(testTask);

        // When
        EmployeeTask result = employeeService.updateTaskStatus(1L, TaskStatus.IN_PROGRESS, "Started cleaning");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(result.getNote()).isEqualTo("Started cleaning");

        verify(employeeTaskRepository).findById(1L);
        verify(employeeTaskRepository).save(any(EmployeeTask.class));
    }

    @Test
    void updateTaskStatus_WithNonExistentTask_ShouldThrowNotFoundException() {
        // Given
        when(employeeTaskRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> employeeService.updateTaskStatus(999L, TaskStatus.DONE, "Completed"))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Task not found with ID: 999");

        verify(employeeTaskRepository).findById(999L);
        verify(employeeTaskRepository, never()).save(any());
    }

    @Test
    void getEmployeeTasks_WithValidEmployee_ShouldReturnTasks() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<EmployeeTask> expectedPage = new PageImpl<>(Arrays.asList(testTask), pageable, 1);
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(testEmployee));
        when(employeeTaskRepository.findByEmployee(testUser, pageable)).thenReturn(expectedPage);

        // When
        Page<EmployeeTask> result = employeeService.getEmployeeTasks(1L, null, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Clean Room 101");

        verify(employeeRepository).findByUserId(1L);
        verify(employeeTaskRepository).findByEmployee(testUser, pageable);
    }

    // ========== LEAVE REQUEST TESTS ==========

    @Test
    void createLeaveRequest_WithValidData_ShouldCreateRequest() {
        // Given
        LocalDate fromDate = LocalDate.now().plusDays(7);
        LocalDate toDate = LocalDate.now().plusDays(10);
        
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(testEmployee));
        when(leaveRequestRepository.findOverlappingLeaveRequestsForEmployeeId(1L, fromDate, toDate))
            .thenReturn(Collections.emptyList());
        when(employeeTrainingRepository.findByEmployee(testUser)).thenReturn(Collections.emptyList());
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(testLeaveRequest);

        // When
        LeaveRequest result = employeeService.createLeaveRequest(1L, fromDate, toDate, "Vacation");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFromDate()).isEqualTo(fromDate);
        assertThat(result.getToDate()).isEqualTo(toDate);
        assertThat(result.getStatus()).isEqualTo(LeaveRequestStatus.PENDING);

        verify(employeeRepository, times(2)).findByUserId(1L); // Called twice: once in createLeaveRequest, once in hasOverlappingTraining
        verify(leaveRequestRepository).findOverlappingLeaveRequestsForEmployeeId(1L, fromDate, toDate);
        verify(leaveRequestRepository).save(any(LeaveRequest.class));
    }

    @Test
    void createLeaveRequest_WithInvalidDateRange_ShouldThrowBusinessRuleException() {
        // Given
        LocalDate fromDate = LocalDate.now().plusDays(10);
        LocalDate toDate = LocalDate.now().plusDays(7); // End before start
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(testEmployee));

        // When & Then
        assertThatThrownBy(() -> employeeService.createLeaveRequest(1L, fromDate, toDate, "Vacation"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Leave start date cannot be after end date");

        verify(employeeRepository).findByUserId(1L);
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    void createLeaveRequest_WithPastStartDate_ShouldThrowBusinessRuleException() {
        // Given
        LocalDate fromDate = LocalDate.now().minusDays(1);
        LocalDate toDate = LocalDate.now().plusDays(3);
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(testEmployee));

        // When & Then
        assertThatThrownBy(() -> employeeService.createLeaveRequest(1L, fromDate, toDate, "Vacation"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Leave start date cannot be in the past");

        verify(employeeRepository).findByUserId(1L);
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    void createLeaveRequest_WithOverlappingRequest_ShouldThrowBusinessRuleException() {
        // Given
        LocalDate fromDate = LocalDate.now().plusDays(7);
        LocalDate toDate = LocalDate.now().plusDays(10);
        
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(testEmployee));
        when(leaveRequestRepository.findOverlappingLeaveRequestsForEmployeeId(1L, fromDate, toDate))
            .thenReturn(Arrays.asList(testLeaveRequest));

        // When & Then
        assertThatThrownBy(() -> employeeService.createLeaveRequest(1L, fromDate, toDate, "Vacation"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Employee already has overlapping leave request");

        verify(employeeRepository).findByUserId(1L);
        verify(leaveRequestRepository).findOverlappingLeaveRequestsForEmployeeId(1L, fromDate, toDate);
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    void approveLeaveRequest_WithValidRequest_ShouldApproveRequest() {
        // Given
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(testLeaveRequest));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(testLeaveRequest);

        // When
        LeaveRequest result = employeeService.approveLeaveRequest(1L, "Approved by manager");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(LeaveRequestStatus.APPROVED);

        verify(leaveRequestRepository).findById(1L);
        verify(leaveRequestRepository).save(any(LeaveRequest.class));
    }

    @Test
    void approveLeaveRequest_WithNonPendingRequest_ShouldThrowBusinessRuleException() {
        // Given
        testLeaveRequest.setStatus(LeaveRequestStatus.APPROVED);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(testLeaveRequest));

        // When & Then
        assertThatThrownBy(() -> employeeService.approveLeaveRequest(1L, "Approved"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Only pending leave requests can be approved");

        verify(leaveRequestRepository).findById(1L);
        verify(leaveRequestRepository, never()).save(any());
    }

    // ========== TRAINING MANAGEMENT TESTS ==========

    @Test
    void assignTraining_WithValidData_ShouldAssignTraining() {
        // Given
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(testEmployee));
        when(trainingRepository.findById(1L)).thenReturn(Optional.of(testTraining));
        when(employeeTrainingRepository.findByEmployee(testUser)).thenReturn(Collections.emptyList());
        when(employeeTrainingRepository.existsByEmployeeAndTraining(testUser, testTraining)).thenReturn(false);
        when(employeeTrainingRepository.save(any(EmployeeTraining.class))).thenReturn(testEmployeeTraining);

        // When
        EmployeeTraining result = employeeService.assignTraining(1L, 1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmployee()).isEqualTo(testUser);
        assertThat(result.getTraining()).isEqualTo(testTraining);
        assertThat(result.getStatus()).isEqualTo(TrainingStatus.ASSIGNED);

        verify(employeeRepository, times(2)).findByUserId(1L); // Called twice: once in assignTraining, once in hasOverlappingTraining
        verify(trainingRepository).findById(1L);
        verify(employeeTrainingRepository).save(any(EmployeeTraining.class));
    }

    @Test
    void assignTraining_WithOverlappingTraining_ShouldThrowBusinessRuleException() {
        // Given
        testEmployeeTraining.setStatus(TrainingStatus.IN_PROGRESS);
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(testEmployee));
        when(trainingRepository.findById(1L)).thenReturn(Optional.of(testTraining));
        when(employeeTrainingRepository.findByEmployee(testUser)).thenReturn(Arrays.asList(testEmployeeTraining));

        // When & Then
        assertThatThrownBy(() -> employeeService.assignTraining(1L, 1L))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Employee already has overlapping training");

        verify(employeeRepository, times(2)).findByUserId(1L); // Called twice: once in assignTraining, once in hasOverlappingTraining
        verify(trainingRepository).findById(1L);
        verify(employeeTrainingRepository, never()).save(any());
    }

    @Test
    void hasInProgressTraining_WithActiveTraining_ShouldReturnTrue() {
        // Given
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(testEmployee));
        when(employeeTrainingRepository.existsByEmployeeAndStatus(testUser, TrainingStatus.IN_PROGRESS))
            .thenReturn(true);

        // When
        boolean result = employeeService.hasInProgressTraining(1L);

        // Then
        assertThat(result).isTrue();
        verify(employeeRepository).findByUserId(1L);
        verify(employeeTrainingRepository).existsByEmployeeAndStatus(testUser, TrainingStatus.IN_PROGRESS);
    }

    @Test
    void hasInProgressTraining_WithNoActiveTraining_ShouldReturnFalse() {
        // Given
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(testEmployee));
        when(employeeTrainingRepository.existsByEmployeeAndStatus(testUser, TrainingStatus.IN_PROGRESS))
            .thenReturn(false);

        // When
        boolean result = employeeService.hasInProgressTraining(1L);

        // Then
        assertThat(result).isFalse();
        verify(employeeRepository).findByUserId(1L);
        verify(employeeTrainingRepository).existsByEmployeeAndStatus(testUser, TrainingStatus.IN_PROGRESS);
    }

    // ========== AVAILABILITY CHECK TESTS ==========

    @Test
    void isEmployeeAvailableForShift_WithActiveEmployee_ShouldReturnTrue() {
        // Given
        LocalDate date = LocalDate.now().plusDays(1);
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(testEmployee));
        when(employeeTrainingRepository.existsByEmployeeAndStatus(testUser, TrainingStatus.IN_PROGRESS))
            .thenReturn(false);
        when(leaveRequestRepository.findByEmployeeAndStatus(testUser, LeaveRequestStatus.APPROVED))
            .thenReturn(Collections.emptyList());

        // When
        boolean result = employeeService.isEmployeeAvailableForShift(1L, date);

        // Then
        assertThat(result).isTrue();
        verify(employeeRepository, times(2)).findByUserId(1L); // Called twice: once in isEmployeeAvailableForShift, once in hasInProgressTraining
        verify(employeeTrainingRepository).existsByEmployeeAndStatus(testUser, TrainingStatus.IN_PROGRESS);
        verify(leaveRequestRepository).findByEmployeeAndStatus(testUser, LeaveRequestStatus.APPROVED);
    }

    @Test
    void isEmployeeAvailableForShift_WithEmployeeInTraining_ShouldReturnFalse() {
        // Given
        LocalDate date = LocalDate.now().plusDays(1);
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(testEmployee));
        when(employeeTrainingRepository.existsByEmployeeAndStatus(testUser, TrainingStatus.IN_PROGRESS))
            .thenReturn(true);

        // When
        boolean result = employeeService.isEmployeeAvailableForShift(1L, date);

        // Then
        assertThat(result).isFalse();
        verify(employeeRepository, times(2)).findByUserId(1L); // Called twice: once in isEmployeeAvailableForShift, once in hasInProgressTraining
        verify(employeeTrainingRepository).existsByEmployeeAndStatus(testUser, TrainingStatus.IN_PROGRESS);
    }

    @Test
    void isEmployeeAvailableForShift_WithApprovedLeave_ShouldReturnFalse() {
        // Given
        LocalDate date = LocalDate.now().plusDays(8); // Within leave period
        testLeaveRequest.setStatus(LeaveRequestStatus.APPROVED);
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(testEmployee));
        when(employeeTrainingRepository.existsByEmployeeAndStatus(testUser, TrainingStatus.IN_PROGRESS))
            .thenReturn(false);
        when(leaveRequestRepository.findByEmployeeAndStatus(testUser, LeaveRequestStatus.APPROVED))
            .thenReturn(Arrays.asList(testLeaveRequest));

        // When
        boolean result = employeeService.isEmployeeAvailableForShift(1L, date);

        // Then
        assertThat(result).isFalse();
        verify(employeeRepository, times(2)).findByUserId(1L); // Called twice: once in isEmployeeAvailableForShift, once in hasInProgressTraining
        verify(employeeTrainingRepository).existsByEmployeeAndStatus(testUser, TrainingStatus.IN_PROGRESS);
        verify(leaveRequestRepository).findByEmployeeAndStatus(testUser, LeaveRequestStatus.APPROVED);
    }

    @Test
    void isEmployeeAvailableForTask_WithActiveEmployee_ShouldReturnTrue() {
        // Given
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(testEmployee));
        when(employeeTrainingRepository.existsByEmployeeAndStatus(testUser, TrainingStatus.IN_PROGRESS))
            .thenReturn(false);

        // When
        boolean result = employeeService.isEmployeeAvailableForTask(1L);

        // Then
        assertThat(result).isTrue();
        verify(employeeRepository, times(2)).findByUserId(1L); // Called twice: once in isEmployeeAvailableForTask, once in hasInProgressTraining
        verify(employeeTrainingRepository).existsByEmployeeAndStatus(testUser, TrainingStatus.IN_PROGRESS);
    }

    @Test
    void isEmployeeAvailableForTask_WithInactiveEmployee_ShouldReturnFalse() {
        // Given
        testEmployee.setStatus(EmployeeStatus.INACTIVE);
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(testEmployee));

        // When
        boolean result = employeeService.isEmployeeAvailableForTask(1L);

        // Then
        assertThat(result).isFalse();
        verify(employeeRepository).findByUserId(1L);
        verify(employeeTrainingRepository, never()).existsByEmployeeAndStatus(any(), any());
    }

    // ========== STATISTICS TESTS ==========

    @Test
    void getEmployeeStatistics_ShouldReturnCorrectStatistics() {
        // Given
        when(employeeRepository.count()).thenReturn(10L);
        when(employeeRepository.countByStatus(EmployeeStatus.ACTIVE)).thenReturn(8L);
        when(employeeRepository.countByStatus(EmployeeStatus.INACTIVE)).thenReturn(2L);
        when(leaveRequestRepository.countByStatus(LeaveRequestStatus.PENDING)).thenReturn(3L);
        when(leaveRequestRepository.countByStatus(LeaveRequestStatus.APPROVED)).thenReturn(5L);
        when(employeeTrainingRepository.countByStatus(TrainingStatus.IN_PROGRESS)).thenReturn(2L);
        when(employeeTrainingRepository.countByStatus(TrainingStatus.COMPLETED)).thenReturn(7L);

        // When
        EmployeeStatistics result = employeeService.getEmployeeStatistics();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalEmployees()).isEqualTo(10L);
        assertThat(result.getActiveEmployees()).isEqualTo(8L);
        assertThat(result.getInactiveEmployees()).isEqualTo(2L);
        assertThat(result.getPendingLeaveRequests()).isEqualTo(3L);
        assertThat(result.getApprovedLeaveRequests()).isEqualTo(5L);
        assertThat(result.getInProgressTrainings()).isEqualTo(2L);
        assertThat(result.getCompletedTrainings()).isEqualTo(7L);

        verify(employeeRepository).count();
        verify(employeeRepository, times(2)).countByStatus(any());
        verify(leaveRequestRepository, times(2)).countByStatus(any());
        verify(employeeTrainingRepository, times(2)).countByStatus(any());
    }

    @Test
    void getDistinctJobTitles_ShouldReturnJobTitles() {
        // Given
        List<String> jobTitles = Arrays.asList("Receptionist", "Manager", "Housekeeper");
        when(employeeRepository.findDistinctJobTitles()).thenReturn(jobTitles);

        // When
        List<String> result = employeeService.getDistinctJobTitles();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly("Receptionist", "Manager", "Housekeeper");

        verify(employeeRepository).findDistinctJobTitles();
    }

    @Test
    void getEmployeesCreatedInLastDays_ShouldReturnEmployees() {
        // Given
        List<Employee> employees = Arrays.asList(testEmployee);
        when(employeeRepository.getEmployeesCreatedInLastDays(any(LocalDateTime.class)))
            .thenReturn(employees);

        // When
        List<Employee> result = employeeService.getEmployeesCreatedInLastDays(7);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testEmployee);

        verify(employeeRepository).getEmployeesCreatedInLastDays(any(LocalDateTime.class));
    }
}