package com.MyBooking.announcement.controller;

import com.MyBooking.announcement.domain.AnnouncementPriority;
import com.MyBooking.announcement.domain.AnnouncementStatus;
import com.MyBooking.announcement.dto.*;
import com.MyBooking.announcement.service.AnnouncementService;
import com.MyBooking.auth.domain.Role;
import com.MyBooking.common.security.UserPrincipal;
import com.MyBooking.employee.domain.Department;
import com.MyBooking.hotel_management.HotelManagementApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = HotelManagementApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnnouncementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AnnouncementService announcementService;

    private Authentication authFor(Long userId, String email, String role) {
        UserPrincipal principal = new UserPrincipal(userId, email, role);
        return new UsernamePasswordAuthenticationToken(principal, "N/A", principal.getAuthorities());
    }

    private AnnouncementResponseDto sampleDto(Department department) {
        return new AnnouncementResponseDto(
                1L, "Shift Update", "Please review schedule",
                10L, "Admin User", AnnouncementPriority.HIGH,
                AnnouncementStatus.PUBLISHED, department, Role.EMPLOYEE,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("Unauthenticated request to /api/announcements returns 401")
    void unauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/announcements"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CLIENT is forbidden from viewing or creating announcements")
    void clientIsForbidden() throws Exception {
        mockMvc.perform(get("/api/announcements")
                        .with(authentication(authFor(30L, "client@hotel.com", "CLIENT"))))
                .andExpect(status().isForbidden());

        AnnouncementCreateRequestDto req = new AnnouncementCreateRequestDto(
                "Title", "Content", AnnouncementPriority.LOW, Department.ALL, null);
        mockMvc.perform(post("/api/announcements")
                        .with(authentication(authFor(30L, "client@hotel.com", "CLIENT")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("EMPLOYEE can view visible announcements and reply, but cannot create or archive")
    void employeeCanReadAndReplyButCannotMutate() throws Exception {
        when(announcementService.getVisibleAnnouncementsForUserAsDto(eq(20L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleDto(Department.HOUSEKEEPING))));

        mockMvc.perform(get("/api/announcements")
                        .with(authentication(authFor(20L, "emp@hotel.com", "EMPLOYEE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].targetDepartment").value("HOUSEKEEPING"));

        AnnouncementCreateRequestDto createReq = new AnnouncementCreateRequestDto(
                "Title", "Content", AnnouncementPriority.MEDIUM, Department.HOUSEKEEPING, Role.EMPLOYEE);
        mockMvc.perform(post("/api/announcements")
                        .with(authentication(authFor(20L, "emp@hotel.com", "EMPLOYEE")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/announcements/1/archive")
                        .with(authentication(authFor(20L, "emp@hotel.com", "EMPLOYEE")))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        AnnouncementReplyResponseDto replyDto = new AnnouncementReplyResponseDto(
                5L, 1L, 20L, "Emp User", "Acknowledged", LocalDateTime.now());
        when(announcementService.addReplyAsDto(eq(1L), eq(20L), any(AnnouncementReplyRequestDto.class)))
                .thenReturn(replyDto);

        mockMvc.perform(post("/api/announcements/1/replies")
                        .with(authentication(authFor(20L, "emp@hotel.com", "EMPLOYEE")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AnnouncementReplyRequestDto("Acknowledged"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Acknowledged"));
    }

    @Test
    @DisplayName("ADMIN can create, update, and archive announcements")
    void adminCanCreateUpdateAndArchive() throws Exception {
        AnnouncementResponseDto dto = sampleDto(Department.FRONT_DESK);
        when(announcementService.createAnnouncementAsDto(eq(10L), any(AnnouncementCreateRequestDto.class)))
                .thenReturn(dto);

        AnnouncementCreateRequestDto createReq = new AnnouncementCreateRequestDto(
                "Shift Update", "Please review schedule", AnnouncementPriority.HIGH, Department.FRONT_DESK, Role.EMPLOYEE);

        mockMvc.perform(post("/api/announcements")
                        .with(authentication(authFor(10L, "admin@hotel.com", "ADMIN")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetDepartment").value("FRONT_DESK"));

        when(announcementService.updateAnnouncementAsDto(eq(1L), any(AnnouncementUpdateRequestDto.class)))
                .thenReturn(dto);
        AnnouncementUpdateRequestDto updateReq = new AnnouncementUpdateRequestDto(
                "Updated Title", "Updated Content", AnnouncementPriority.HIGH);

        mockMvc.perform(put("/api/announcements/1")
                        .with(authentication(authFor(10L, "admin@hotel.com", "ADMIN")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk());

        dto.setStatus(AnnouncementStatus.ARCHIVED);
        when(announcementService.archiveAnnouncementAsDto(1L)).thenReturn(dto);

        mockMvc.perform(patch("/api/announcements/1/archive")
                        .with(authentication(authFor(10L, "admin@hotel.com", "ADMIN")))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }
}

