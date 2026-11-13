package com.MyBooking.auth.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_preference", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "notification_type"}))
public class NotificationPreference {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotBlank(message = "Notification type is required")
    @Size(max = 50, message = "Notification type must not exceed 50 characters")
    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;
    
    @NotNull(message = "Email enabled is required")
    @Column(name = "email_enabled", nullable = false)
    private Boolean emailEnabled = true;
    
    @NotNull(message = "SMS enabled is required")
    @Column(name = "sms_enabled", nullable = false)
    private Boolean smsEnabled = false;
    
    @NotNull(message = "Push enabled is required")
    @Column(name = "push_enabled", nullable = false)
    private Boolean pushEnabled = true;
    
    @NotNull(message = "Is active is required")
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Constructors
    public NotificationPreference() {}
    
    public NotificationPreference(User user, String notificationType, Boolean emailEnabled, Boolean smsEnabled, Boolean pushEnabled) {
        this.user = user;
        this.notificationType = notificationType;
        this.emailEnabled = emailEnabled;
        this.smsEnabled = smsEnabled;
        this.pushEnabled = pushEnabled;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }
    
    public Boolean getEmailEnabled() { return emailEnabled; }
    public void setEmailEnabled(Boolean emailEnabled) { this.emailEnabled = emailEnabled; }
    
    public Boolean getSmsEnabled() { return smsEnabled; }
    public void setSmsEnabled(Boolean smsEnabled) { this.smsEnabled = smsEnabled; }
    
    public Boolean getPushEnabled() { return pushEnabled; }
    public void setPushEnabled(Boolean pushEnabled) { this.pushEnabled = pushEnabled; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    // Business methods
    public boolean isEmailEnabled() {
        return Boolean.TRUE.equals(emailEnabled);
    }
    
    public boolean isSmsEnabled() {
        return Boolean.TRUE.equals(smsEnabled);
    }
    
    public boolean isPushEnabled() {
        return Boolean.TRUE.equals(pushEnabled);
    }
    
    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }
    
    public boolean hasAnyNotificationEnabled() {
        return isEmailEnabled() || isSmsEnabled() || isPushEnabled();
    }
}

