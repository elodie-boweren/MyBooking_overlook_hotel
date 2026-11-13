package com.MyBooking.announcement.dto;

import java.time.LocalDateTime;

public class AnnouncementReplyResponseDto {

    private Long id;
    private Long announcementId;
    private Long userId;
    private String authorName;
    private String message;
    private LocalDateTime createdAt;

    public AnnouncementReplyResponseDto() {}

    public AnnouncementReplyResponseDto(Long id, Long announcementId, Long userId,
                                        String authorName, String message, LocalDateTime createdAt) {
        this.id = id;
        this.announcementId = announcementId;
        this.userId = userId;
        this.authorName = authorName;
        this.message = message;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAnnouncementId() { return announcementId; }
    public void setAnnouncementId(Long announcementId) { this.announcementId = announcementId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

