package com.MyBooking.announcement.dto;

import com.MyBooking.announcement.domain.AnnouncementPriority;
import com.MyBooking.announcement.domain.AnnouncementStatus;
import com.MyBooking.auth.domain.Role;
import com.MyBooking.employee.domain.Department;
import java.time.LocalDateTime;

public class AnnouncementResponseDto {

    private Long id;
    private String title;
    private String content;
    private Long createdByUserId;
    private String createdByName;
    private AnnouncementPriority priority;
    private AnnouncementStatus status;
    private Department targetDepartment;
    private Role targetRole;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AnnouncementResponseDto() {}

    public AnnouncementResponseDto(Long id, String title, String content, Long createdByUserId,
                                   String createdByName, AnnouncementPriority priority,
                                   AnnouncementStatus status, Department targetDepartment,
                                   Role targetRole, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.createdByUserId = createdByUserId;
        this.createdByName = createdByName;
        this.priority = priority;
        this.status = status;
        this.targetDepartment = targetDepartment;
        this.targetRole = targetRole;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(Long createdByUserId) { this.createdByUserId = createdByUserId; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public AnnouncementPriority getPriority() { return priority; }
    public void setPriority(AnnouncementPriority priority) { this.priority = priority; }

    public AnnouncementStatus getStatus() { return status; }
    public void setStatus(AnnouncementStatus status) { this.status = status; }

    public Department getTargetDepartment() { return targetDepartment; }
    public void setTargetDepartment(Department targetDepartment) { this.targetDepartment = targetDepartment; }

    public Role getTargetRole() { return targetRole; }
    public void setTargetRole(Role targetRole) { this.targetRole = targetRole; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

