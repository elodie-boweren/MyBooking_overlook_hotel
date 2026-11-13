package com.MyBooking.employee.controller;

import com.MyBooking.employee.dto.*;
import com.MyBooking.employee.service.EmployeeService;
import com.MyBooking.employee.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/employees")
@PreAuthorize("hasRole('ADMIN')")
public class AdminEmployeeController {

    @Autowired
    private EmployeeService employeeService;

    // ==================== EMPLOYEE MANAGEMENT ====================

    @PostMapping
    public ResponseEntity<EmployeeResponseDto> createEmployee(@Valid @RequestBody EmployeeCreateRequestDto request) {
        Employee employee = employeeService.createEmployee(request.getUserId(), request.getJobTitle());
        EmployeeResponseDto response = convertToEmployeeResponseDto(employee);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<EmployeeResponseDto> getEmployee(@PathVariable Long userId) {
        Employee employee = employeeService.getEmployeeByUserId(userId);
        EmployeeResponseDto response = convertToEmployeeResponseDto(employee);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<EmployeeResponseDto> updateEmployee(@PathVariable Long userId, 
                                                             @Valid @RequestBody EmployeeUpdateRequestDto request) {
        Employee employee = employeeService.updateEmployee(userId, request.getJobTitle(), request.getStatus());
        EmployeeResponseDto response = convertToEmployeeResponseDto(employee);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long userId) {
        employeeService.deleteEmployee(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<EmployeeResponseDto>> searchEmployees(
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(required = false) String jobTitle,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String email,
            Pageable pageable) {
        Page<Employee> employees = employeeService.searchEmployees(status, jobTitle, pageable);
        Page<EmployeeResponseDto> response = employees.map(this::convertToEmployeeResponseDto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    public ResponseEntity<List<EmployeeResponseDto>> getActiveEmployees() {
        List<Employee> employees = employeeService.getActiveEmployees();
        List<EmployeeResponseDto> response = employees.stream()
                .map(this::convertToEmployeeResponseDto)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/job-title/{jobTitle}")
    public ResponseEntity<List<EmployeeResponseDto>> getEmployeesByJobTitle(@PathVariable String jobTitle) {
        List<Employee> employees = employeeService.getEmployeesByJobTitle(jobTitle);
        List<EmployeeResponseDto> response = employees.stream()
                .map(this::convertToEmployeeResponseDto)
                .toList();
        return ResponseEntity.ok(response);
    }

    // ==================== TASK MANAGEMENT ====================

    @PostMapping("/tasks")
    public ResponseEntity<TaskResponseDto> createTask(@Valid @RequestBody TaskCreateRequestDto request) {
        EmployeeTask task = employeeService.createTask(request.getEmployeeId(), request.getTitle(), request.getDescription());
        TaskResponseDto response = convertToTaskResponseDto(task);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/tasks/{taskId}")
    public ResponseEntity<TaskResponseDto> updateTask(@PathVariable Long taskId, 
                                                     @Valid @RequestBody TaskUpdateRequestDto request) {
        EmployeeTask task = employeeService.updateTaskStatus(taskId, request.getStatus(), request.getNote());
        TaskResponseDto response = convertToTaskResponseDto(task);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tasks")
    public ResponseEntity<Page<TaskResponseDto>> getAllTasks(
            @RequestParam(required = false) TaskStatus status,
            Pageable pageable) {
        Page<EmployeeTask> tasks = employeeService.getAllTasks(status, pageable);
        Page<TaskResponseDto> response = tasks.map(this::convertToTaskResponseDto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{employeeId}/tasks")
    public ResponseEntity<Page<TaskResponseDto>> getEmployeeTasks(
            @PathVariable Long employeeId,
            @RequestParam(required = false) TaskStatus status,
            Pageable pageable) {
        Page<EmployeeTask> tasks = employeeService.getEmployeeTasks(employeeId, status, pageable);
        Page<TaskResponseDto> response = tasks.map(this::convertToTaskResponseDto);
        return ResponseEntity.ok(response);
    }

    // ==================== LEAVE REQUEST MANAGEMENT ====================

    @PutMapping("/leave-requests/{requestId}/approve")
    public ResponseEntity<LeaveRequestResponseDto> approveLeaveRequest(@PathVariable Long requestId) {
        LeaveRequest request = employeeService.approveLeaveRequest(requestId, null);
        LeaveRequestResponseDto response = convertToLeaveRequestResponseDto(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/leave-requests/{requestId}/reject")
    public ResponseEntity<LeaveRequestResponseDto> rejectLeaveRequest(@PathVariable Long requestId) {
        LeaveRequest request = employeeService.rejectLeaveRequest(requestId, null);
        LeaveRequestResponseDto response = convertToLeaveRequestResponseDto(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/leave-requests/pending")
    public ResponseEntity<Page<LeaveRequestResponseDto>> getPendingLeaveRequests(Pageable pageable) {
        Page<LeaveRequest> requests = employeeService.getPendingLeaveRequests(pageable);
        Page<LeaveRequestResponseDto> response = requests.map(this::convertToLeaveRequestResponseDto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/leave-requests")
    public ResponseEntity<Page<LeaveRequestResponseDto>> getAllLeaveRequests(
            @RequestParam(required = false) LeaveRequestStatus status,
            Pageable pageable) {
        Page<LeaveRequest> requests = employeeService.getAllLeaveRequests(status, pageable);
        Page<LeaveRequestResponseDto> response = requests.map(this::convertToLeaveRequestResponseDto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{employeeId}/leave-requests")
    public ResponseEntity<Page<LeaveRequestResponseDto>> getEmployeeLeaveRequests(
            @PathVariable Long employeeId,
            @RequestParam(required = false) LeaveRequestStatus status,
            Pageable pageable) {
        Page<LeaveRequest> requests = employeeService.getEmployeeLeaveRequests(employeeId, status, pageable);
        Page<LeaveRequestResponseDto> response = requests.map(this::convertToLeaveRequestResponseDto);
        return ResponseEntity.ok(response);
    }

    // ==================== TRAINING MANAGEMENT ====================

    @PostMapping("/trainings")
    public ResponseEntity<TrainingResponseDto> createTraining(@Valid @RequestBody TrainingCreateRequestDto request) {
        Training training = employeeService.createTraining(request.getTitle(), request.getStartDate(), request.getEndDate());
        TrainingResponseDto response = convertToTrainingResponseDto(training);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/trainings/{trainingId}")
    public ResponseEntity<TrainingResponseDto> updateTraining(@PathVariable Long trainingId, 
                                                             @Valid @RequestBody TrainingUpdateRequestDto request) {
        Training training = employeeService.updateTraining(trainingId, request.getTitle(), request.getStartDate(), request.getEndDate());
        TrainingResponseDto response = convertToTrainingResponseDto(training);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/trainings/{trainingId}")
    public ResponseEntity<Void> deleteTraining(@PathVariable Long trainingId) {
        employeeService.deleteTraining(trainingId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/trainings")
    public ResponseEntity<Page<TrainingResponseDto>> getAllTrainings(Pageable pageable) {
        Page<Training> trainings = employeeService.getAllTrainings(pageable);
        Page<TrainingResponseDto> response = trainings.map(this::convertToTrainingResponseDto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/trainings/{trainingId}")
    public ResponseEntity<TrainingResponseDto> getTraining(@PathVariable Long trainingId) {
        Training training = employeeService.getTraining(trainingId);
        TrainingResponseDto response = convertToTrainingResponseDto(training);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/trainings/assign")
    public ResponseEntity<EmployeeTrainingResponseDto> assignTraining(@Valid @RequestBody EmployeeTrainingCreateRequestDto request) {
        EmployeeTraining employeeTraining = employeeService.assignTraining(request.getEmployeeId(), request.getTrainingId());
        EmployeeTrainingResponseDto response = convertToEmployeeTrainingResponseDto(employeeTraining);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/trainings/{employeeId}/{trainingId}/status")
    public ResponseEntity<EmployeeTrainingResponseDto> updateTrainingStatus(
            @PathVariable Long employeeId,
            @PathVariable Long trainingId,
            @Valid @RequestBody EmployeeTrainingUpdateRequestDto request) {
        EmployeeTraining employeeTraining = employeeService.updateEmployeeTrainingStatus(employeeId, trainingId, request.getStatus());
        EmployeeTrainingResponseDto response = convertToEmployeeTrainingResponseDto(employeeTraining);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{employeeId}/trainings")
    public ResponseEntity<Page<EmployeeTrainingResponseDto>> getEmployeeTrainings(
            @PathVariable Long employeeId,
            @RequestParam(required = false) TrainingStatus status,
            Pageable pageable) {
        Page<EmployeeTraining> trainings = employeeService.getEmployeeTrainings(employeeId, status, pageable);
        Page<EmployeeTrainingResponseDto> response = trainings.map(this::convertToEmployeeTrainingResponseDto);
        return ResponseEntity.ok(response);
    }

    // ==================== SHIFT MANAGEMENT ====================

    @PostMapping("/shifts")
    public ResponseEntity<ShiftResponseDto> createShift(@Valid @RequestBody ShiftCreateRequestDto request) {
        Shift shift = employeeService.createShift(request.getEmployeeId(), request.getStartAt(), request.getEndAt());
        ShiftResponseDto response = convertToShiftResponseDto(shift);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/shifts/{shiftId}")
    public ResponseEntity<ShiftResponseDto> updateShift(@PathVariable Long shiftId, 
                                                       @Valid @RequestBody ShiftUpdateRequestDto request) {
        Shift shift = employeeService.updateShift(shiftId, request.getStartAt(), request.getEndAt());
        ShiftResponseDto response = convertToShiftResponseDto(shift);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/shifts/{shiftId}")
    public ResponseEntity<Void> deleteShift(@PathVariable Long shiftId) {
        employeeService.deleteShift(shiftId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/shifts")
    public ResponseEntity<Page<ShiftResponseDto>> getAllShifts(Pageable pageable) {
        Page<Shift> shifts = employeeService.getAllShifts(pageable);
        Page<ShiftResponseDto> response = shifts.map(this::convertToShiftResponseDto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/shifts/{shiftId}")
    public ResponseEntity<ShiftResponseDto> getShift(@PathVariable Long shiftId) {
        Shift shift = employeeService.getShift(shiftId);
        ShiftResponseDto response = convertToShiftResponseDto(shift);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{employeeId}/shifts")
    public ResponseEntity<Page<ShiftResponseDto>> getEmployeeShifts(
            @PathVariable Long employeeId,
            Pageable pageable) {
        Page<Shift> shifts = employeeService.getEmployeeShifts(employeeId, pageable);
        Page<ShiftResponseDto> response = shifts.map(this::convertToShiftResponseDto);
        return ResponseEntity.ok(response);
    }

    // ==================== STATISTICS ====================

    @GetMapping("/statistics")
    public ResponseEntity<EmployeeStatisticsDto> getEmployeeStatistics() {
        EmployeeStatisticsDto response = employeeService.getEmployeeStatisticsAsDto();
        return ResponseEntity.ok(response);
    }

    // ==================== AVAILABILITY CHECKS ====================

    @GetMapping("/{employeeId}/availability")
    public ResponseEntity<EmployeeAvailabilityDto> checkEmployeeAvailability(@PathVariable Long employeeId) {
        boolean available = employeeService.isEmployeeAvailableForTask(employeeId);
        Employee employee = employeeService.getEmployeeByUserId(employeeId);
        
        String reason = "ACTIVE";
        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            reason = "INACTIVE";
        } else if (employeeService.hasInProgressTraining(employeeId)) {
            reason = "IN_TRAINING";
        } else {
            // Check for approved leave
            LocalDate today = LocalDate.now();
            if (!employeeService.isEmployeeAvailableForShift(employeeId, today)) {
                reason = "ON_LEAVE";
            }
        }
        
        EmployeeAvailabilityDto response = new EmployeeAvailabilityDto(
                employeeId,
                employee.getUser().getFirstName() + " " + employee.getUser().getLastName(),
                available,
                reason
        );
        return ResponseEntity.ok(response);
    }

    // ==================== DTO CONVERSION METHODS ====================

    private EmployeeResponseDto convertToEmployeeResponseDto(Employee employee) {
        return new EmployeeResponseDto(
                employee.getUserId(),
                employee.getUser().getEmail(),
                employee.getUser().getFirstName(),
                employee.getUser().getLastName(),
                employee.getStatus(),
                employee.getJobTitle(),
                employee.getDepartment(),
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }

    private TaskResponseDto convertToTaskResponseDto(EmployeeTask task) {
        return new TaskResponseDto(
                task.getId(),
                task.getEmployee().getId(),
                task.getEmployee().getFirstName() + " " + task.getEmployee().getLastName(),
                task.getEmployee().getEmail(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getNote(),
                task.getPhotoUrl(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    private LeaveRequestResponseDto convertToLeaveRequestResponseDto(LeaveRequest request) {
        return new LeaveRequestResponseDto(
                request.getId(),
                request.getEmployee().getId(),
                request.getEmployee().getFirstName() + " " + request.getEmployee().getLastName(),
                request.getEmployee().getEmail(),
                request.getFromDate(),
                request.getToDate(),
                request.getStatus(),
                request.getReason()
        );
    }

    private TrainingResponseDto convertToTrainingResponseDto(Training training) {
        return new TrainingResponseDto(
                training.getId(),
                training.getTitle(),
                training.getStartDate(),
                training.getEndDate()
        );
    }

    private EmployeeTrainingResponseDto convertToEmployeeTrainingResponseDto(EmployeeTraining employeeTraining) {
        return new EmployeeTrainingResponseDto(
                employeeTraining.getEmployee().getId(),
                employeeTraining.getEmployee().getFirstName() + " " + employeeTraining.getEmployee().getLastName(),
                employeeTraining.getEmployee().getEmail(),
                employeeTraining.getTraining().getId(),
                employeeTraining.getTraining().getTitle(),
                employeeTraining.getTraining().getStartDate(),
                employeeTraining.getTraining().getEndDate(),
                employeeTraining.getStatus(),
                employeeTraining.getAssignedAt(),
                employeeTraining.getCompletedAt()
        );
    }

    private ShiftResponseDto convertToShiftResponseDto(Shift shift) {
        return new ShiftResponseDto(
                shift.getId(),
                shift.getEmployee().getId(),
                shift.getEmployee().getFirstName() + " " + shift.getEmployee().getLastName(),
                shift.getEmployee().getEmail(),
                shift.getStartAt(),
                shift.getEndAt()
        );
    }
}
