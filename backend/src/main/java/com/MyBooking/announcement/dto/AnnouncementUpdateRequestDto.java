package com.MyBooking.announcement.dto;

import com.MyBooking.announcement.domain.AnnouncementPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AnnouncementUpdateRequestDto {

    @NotBlank(message = "Announcement title is required")
    @Size(max = 200, message = "Announcement title must not exceed 200 characters")
    private String title;

    @NotBlank(message = "Announcement content is required")
    @Size(max = 2000, message = "Announcement content must not exceed 2000 characters")
    private String content;

    @NotNull(message = "Announcement priority is required")
    private AnnouncementPriority priority;

    public AnnouncementUpdateRequestDto() {}

    public AnnouncementUpdateRequestDto(String title, String content, AnnouncementPriority priority) {
        this.title = title;
        this.content = content;
        this.priority = priority;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public AnnouncementPriority getPriority() { return priority; }
    public void setPriority(AnnouncementPriority priority) { this.priority = priority; }
}

