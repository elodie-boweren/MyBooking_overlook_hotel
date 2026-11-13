package com.MyBooking.employee.dto;

import com.MyBooking.employee.domain.Department;
import com.MyBooking.employee.domain.EmployeeStatus;
import java.time.LocalDateTime;

public class EmployeeResponseDto {
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private EmployeeStatus status;
    private String jobTitle;
    private Department department;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public EmployeeResponseDto() {}

    public EmployeeResponseDto(Long userId, String email, String firstName, String lastName, 
                              EmployeeStatus status, String jobTitle, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(userId, email, firstName, lastName, status, jobTitle, Department.ALL, createdAt, updatedAt);
    }

    public EmployeeResponseDto(Long userId, String email, String firstName, String lastName, 
                              EmployeeStatus status, String jobTitle, Department department,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.userId = userId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.status = status;
        this.jobTitle = jobTitle;
        this.department = department != null ? department : Department.ALL;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public EmployeeStatus getStatus() { return status; }
    public void setStatus(EmployeeStatus status) { this.status = status; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
