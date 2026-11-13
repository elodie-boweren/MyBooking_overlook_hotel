package com.MyBooking.announcement.dto;

import com.MyBooking.announcement.domain.AnnouncementPriority;
import com.MyBooking.auth.domain.Role;
import com.MyBooking.employee.domain.Department;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AnnouncementCreateRequestDto {

    @NotBlank(message = "Announcement title is required")
    @Size(max = 200, message = "Announcement title must not exceed 200 characters")
    private String title;

    @NotBlank(message = "Announcement content is required")
    @Size(max = 2000, message = "Announcement content must not exceed 2000 characters")
    private String content;

    @NotNull(message = "Announcement priority is required")
    private AnnouncementPriority priority;

    private Department targetDepartment = Department.ALL;

    private Role targetRole;

    public AnnouncementCreateRequestDto() {}

    public AnnouncementCreateRequestDto(String title, String content, AnnouncementPriority priority,
                                        Department targetDepartment, Role targetRole) {
        this.title = title;
        this.content = content;
        this.priority = priority;
        this.targetDepartment = targetDepartment != null ? targetDepartment : Department.ALL;
        this.targetRole = targetRole;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public AnnouncementPriority getPriority() { return priority; }
    public void setPriority(AnnouncementPriority priority) { this.priority = priority; }

    public Department getTargetDepartment() { return targetDepartment; }
    public void setTargetDepartment(Department targetDepartment) {
        this.targetDepartment = targetDepartment != null ? targetDepartment : Department.ALL;
    }

    public Role getTargetRole() { return targetRole; }
    public void setTargetRole(Role targetRole) { this.targetRole = targetRole; }
}

