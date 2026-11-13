package com.MyBooking.announcement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AnnouncementReplyRequestDto {

    @NotBlank(message = "Reply message is required")
    @Size(max = 1000, message = "Reply message must not exceed 1000 characters")
    private String message;

    public AnnouncementReplyRequestDto() {}

    public AnnouncementReplyRequestDto(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

