package com.MyBooking.announcement.controller;

import com.MyBooking.announcement.dto.*;
import com.MyBooking.announcement.service.AnnouncementService;
import com.MyBooking.common.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AnnouncementResponseDto> createAnnouncement(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AnnouncementCreateRequestDto request) {
        AnnouncementResponseDto created = announcementService.createAnnouncementAsDto(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<Page<AnnouncementResponseDto>> getVisibleAnnouncements(
            @AuthenticationPrincipal UserPrincipal principal,
            Pageable pageable) {
        Page<AnnouncementResponseDto> page = announcementService.getVisibleAnnouncementsForUserAsDto(principal.getId(), pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<AnnouncementResponseDto> getAnnouncementById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AnnouncementResponseDto dto = announcementService.getAnnouncementByIdForUserAsDto(id, principal.getId());
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AnnouncementResponseDto> updateAnnouncement(
            @PathVariable Long id,
            @Valid @RequestBody AnnouncementUpdateRequestDto request) {
        AnnouncementResponseDto updated = announcementService.updateAnnouncementAsDto(id, request);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AnnouncementResponseDto> archiveAnnouncement(@PathVariable Long id) {
        AnnouncementResponseDto archived = announcementService.archiveAnnouncementAsDto(id);
        return ResponseEntity.ok(archived);
    }

    @PostMapping("/{id}/replies")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<AnnouncementReplyResponseDto> addReply(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody AnnouncementReplyRequestDto request) {
        AnnouncementReplyResponseDto reply = announcementService.addReplyAsDto(id, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(reply);
    }

    @GetMapping("/{id}/replies")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<List<AnnouncementReplyResponseDto>> getReplies(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        List<AnnouncementReplyResponseDto> replies = announcementService.getRepliesAsDto(id, principal.getId());
        return ResponseEntity.ok(replies);
    }
}

