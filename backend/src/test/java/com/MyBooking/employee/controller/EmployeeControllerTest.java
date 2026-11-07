package com.MyBooking.employee.controller;

import com.MyBooking.auth.domain.Role;
import com.MyBooking.auth.domain.User;
import com.MyBooking.common.config.CorsConfig;
import com.MyBooking.common.security.SecurityConfig;
import com.MyBooking.common.security.UserPrincipal;
import com.MyBooking.employee.domain.*;
import com.MyBooking.employee.dto.EmployeeTrainingUpdateRequestDto;
import com.MyBooking.employee.dto.LeaveRequestCreateRequestDto;
import com.MyBooking.employee.dto.TaskUpdateRequestDto;
import com.MyBooking.employee.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.MyBooking.hotel_management.HotelManagementApplication;
import com.MyBooking.hotel_management.HotelManagementApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = HotelManagementApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmployeeService employeeService;

    private User testUser;
    private Employee testEmployee;
    private EmployeeTask testTask;
    private LeaveRequest testLeaveRequest;
    private Training testTraining;
    private EmployeeTraining testEmployeeTraining;
    private Shift testShift;

    private static final Long EMPLOYEE_USER_ID = 10L;
    private static final Long SPOOFED_USER_ID = 9999L;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(EMPLOYEE_USER_ID);
        testUser.setEmail("employee10@hotel.com");
        testUser.setFirstName("Alice");
        testUser.setLastName("Smith");
        testUser.setRole(Role.EMPLOYEE);

        testEmployee = new Employee(testUser, EmployeeStatus.ACTIVE, "Front Desk");
        testEmployee.setUserId(EMPLOYEE_USER_ID);

        testTask = new EmployeeTask(testUser, "Clean Lobby", "Thoroughly clean front lobby", TaskStatus.TODO);
        testTask.setId(101L);
        testTask.setNote("Standard note");

        testLeaveRequest = new LeaveRequest(testUser, LocalDate.now().plusDays(5), LocalDate.now().plusDays(10), LeaveRequestStatus.PENDING, "Vacation");
        testLeaveRequest.setId(201L);

        testTraining = new Training("Fire Safety", LocalDate.now(), LocalDate.now().plusDays(2));
        testTraining.setId(301L);

        testEmployeeTraining = new EmployeeTraining(testUser, testTraining, TrainingStatus.IN_PROGRESS);

        testShift = new Shift(testUser, LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(9));
        testShift.setId(401L);
    }

    private Authentication employeeAuth(Long userId, String email) {
        UserPrincipal principal = new UserPrincipal(userId, email, "EMPLOYEE");
        return new UsernamePasswordAuthenticationToken(principal, "N/A", principal.getAuthorities());
    }

    private Authentication clientAuth(Long userId, String email) {
        UserPrincipal principal = new UserPrincipal(userId, email, "CLIENT");
        return new UsernamePasswordAuthenticationToken(principal, "N/A", principal.getAuthorities());
    }

    @Nested
    @DisplayName("Authentication and Authorization Access Guards")
    class AccessControlTests {

        @Test
        @DisplayName("Unauthenticated request should return 401 Unauthorized")
        void unauthenticatedRequestShouldReturn401() throws Exception {
            mockMvc.perform(get("/api/employee/profile"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Authenticated caller with non-employee role (ROLE_CLIENT) should return 403 Forbidden")
        void nonEmployeeRoleShouldReturn403() throws Exception {
            mockMvc.perform(get("/api/employee/profile")
                    .with(authentication(clientAuth(99L, "client@hotel.com"))))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Zero Trust Identity: All 9 Endpoints derive userId from UserPrincipal and ignore X-User-Id")
    class ZeroTrustIdentityTests {

        @Test
        @DisplayName("1. GET /api/employee/profile should resolve userId from principal and ignore X-User-Id header")
        void getProfileResolvesFromPrincipal() throws Exception {
            when(employeeService.getEmployeeByUserId(EMPLOYEE_USER_ID)).thenReturn(testEmployee);

            mockMvc.perform(get("/api/employee/profile")
                    .with(authentication(employeeAuth(EMPLOYEE_USER_ID, "alice@hotel.com")))
                    .header("X-User-Id", SPOOFED_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(EMPLOYEE_USER_ID))
                .andExpect(jsonPath("$.email").value("employee10@hotel.com"));

            verify(employeeService).getEmployeeByUserId(EMPLOYEE_USER_ID);
            verify(employeeService, never()).getEmployeeByUserId(SPOOFED_USER_ID);
        }

        @Test
        @DisplayName("2. GET /api/employee/tasks should resolve userId from principal and ignore X-User-Id header")
        void getTasksResolvesFromPrincipal() throws Exception {
            when(employeeService.getEmployeeTasks(eq(EMPLOYEE_USER_ID), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(testTask)));

            mockMvc.perform(get("/api/employee/tasks")
                    .with(authentication(employeeAuth(EMPLOYEE_USER_ID, "alice@hotel.com")))
                    .header("X-User-Id", SPOOFED_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(101L))
                .andExpect(jsonPath("$.content[0].title").value("Clean Lobby"));

            verify(employeeService).getEmployeeTasks(eq(EMPLOYEE_USER_ID), isNull(), any(Pageable.class));
            verify(employeeService, never()).getEmployeeTasks(eq(SPOOFED_USER_ID), any(), any());
        }

        @Test
        @DisplayName("3. PUT /api/employee/tasks/{taskId} should resolve userId from principal and ignore X-User-Id header")
        void updateTaskResolvesFromPrincipal() throws Exception {
            when(employeeService.getEmployeeTasks(eq(EMPLOYEE_USER_ID), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(testTask)));
            when(employeeService.updateTaskStatus(eq(101L), eq(TaskStatus.DONE), anyString()))
                .thenReturn(testTask);

            TaskUpdateRequestDto requestDto = new TaskUpdateRequestDto(TaskStatus.DONE, "Done well", null);

            mockMvc.perform(put("/api/employee/tasks/101")
                    .with(authentication(employeeAuth(EMPLOYEE_USER_ID, "alice@hotel.com")))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto))
                    .header("X-User-Id", SPOOFED_USER_ID))
                .andExpect(status().isOk());

            verify(employeeService).getEmployeeTasks(eq(EMPLOYEE_USER_ID), isNull(), any(Pageable.class));
            verify(employeeService).updateTaskStatus(101L, TaskStatus.DONE, "Done well");
            verify(employeeService, never()).getEmployeeTasks(eq(SPOOFED_USER_ID), any(), any());
        }

        @Test
        @DisplayName("4. POST /api/employee/leave-requests should resolve userId from principal and ignore X-User-Id header")
        void createLeaveRequestResolvesFromPrincipal() throws Exception {
            LocalDate from = LocalDate.now().plusDays(2);
            LocalDate to = LocalDate.now().plusDays(5);
            LeaveRequestCreateRequestDto dto = new LeaveRequestCreateRequestDto(from, to, "Vacation");

            when(employeeService.createLeaveRequest(eq(EMPLOYEE_USER_ID), eq(from), eq(to), eq("Vacation")))
                .thenReturn(testLeaveRequest);

            mockMvc.perform(post("/api/employee/leave-requests")
                    .with(authentication(employeeAuth(EMPLOYEE_USER_ID, "alice@hotel.com")))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto))
                    .header("X-User-Id", SPOOFED_USER_ID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(201L));

            verify(employeeService).createLeaveRequest(EMPLOYEE_USER_ID, from, to, "Vacation");
            verify(employeeService, never()).createLeaveRequest(eq(SPOOFED_USER_ID), any(), any(), any());
        }

        @Test
        @DisplayName("5. GET /api/employee/leave-requests should resolve userId from principal and ignore X-User-Id header")
        void getLeaveRequestsResolvesFromPrincipal() throws Exception {
            when(employeeService.getEmployeeLeaveRequests(eq(EMPLOYEE_USER_ID), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(testLeaveRequest)));

            mockMvc.perform(get("/api/employee/leave-requests")
                    .with(authentication(employeeAuth(EMPLOYEE_USER_ID, "alice@hotel.com")))
                    .header("X-User-Id", SPOOFED_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(201L));

            verify(employeeService).getEmployeeLeaveRequests(eq(EMPLOYEE_USER_ID), isNull(), any(Pageable.class));
            verify(employeeService, never()).getEmployeeLeaveRequests(eq(SPOOFED_USER_ID), any(), any());
        }

        @Test
        @DisplayName("6. GET /api/employee/trainings should resolve userId from principal and ignore X-User-Id header")
        void getTrainingsResolvesFromPrincipal() throws Exception {
            when(employeeService.getEmployeeTrainings(eq(EMPLOYEE_USER_ID), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(testEmployeeTraining)));

            mockMvc.perform(get("/api/employee/trainings")
                    .with(authentication(employeeAuth(EMPLOYEE_USER_ID, "alice@hotel.com")))
                    .header("X-User-Id", SPOOFED_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].trainingTitle").value("Fire Safety"));

            verify(employeeService).getEmployeeTrainings(eq(EMPLOYEE_USER_ID), isNull(), any(Pageable.class));
            verify(employeeService, never()).getEmployeeTrainings(eq(SPOOFED_USER_ID), any(), any());
        }

        @Test
        @DisplayName("7. PUT /api/employee/trainings/{trainingId}/status should resolve userId from principal and ignore X-User-Id header")
        void updateTrainingStatusResolvesFromPrincipal() throws Exception {
            EmployeeTrainingUpdateRequestDto requestDto = new EmployeeTrainingUpdateRequestDto(TrainingStatus.COMPLETED);

            when(employeeService.updateEmployeeTrainingStatus(eq(EMPLOYEE_USER_ID), eq(301L), eq(TrainingStatus.COMPLETED)))
                .thenReturn(testEmployeeTraining);

            mockMvc.perform(put("/api/employee/trainings/301/status")
                    .with(authentication(employeeAuth(EMPLOYEE_USER_ID, "alice@hotel.com")))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto))
                    .header("X-User-Id", SPOOFED_USER_ID))
                .andExpect(status().isOk());

            verify(employeeService).updateEmployeeTrainingStatus(EMPLOYEE_USER_ID, 301L, TrainingStatus.COMPLETED);
            verify(employeeService, never()).updateEmployeeTrainingStatus(eq(SPOOFED_USER_ID), any(), any());
        }

        @Test
        @DisplayName("8. GET /api/employee/shifts should resolve userId from principal and ignore X-User-Id header")
        void getShiftsResolvesFromPrincipal() throws Exception {
            when(employeeService.getEmployeeShifts(eq(EMPLOYEE_USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(testShift)));

            mockMvc.perform(get("/api/employee/shifts")
                    .with(authentication(employeeAuth(EMPLOYEE_USER_ID, "alice@hotel.com")))
                    .header("X-User-Id", SPOOFED_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(401L));

            verify(employeeService).getEmployeeShifts(eq(EMPLOYEE_USER_ID), any(Pageable.class));
            verify(employeeService, never()).getEmployeeShifts(eq(SPOOFED_USER_ID), any());
        }

        @Test
        @DisplayName("9. GET /api/employee/availability should resolve userId from principal and ignore X-User-Id header")
        void checkAvailabilityResolvesFromPrincipal() throws Exception {
            when(employeeService.isEmployeeAvailableForTask(EMPLOYEE_USER_ID)).thenReturn(true);
            when(employeeService.getEmployeeByUserId(EMPLOYEE_USER_ID)).thenReturn(testEmployee);

            mockMvc.perform(get("/api/employee/availability")
                    .with(authentication(employeeAuth(EMPLOYEE_USER_ID, "alice@hotel.com")))
                    .header("X-User-Id", SPOOFED_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(EMPLOYEE_USER_ID))
                .andExpect(jsonPath("$.available").value(true));

            verify(employeeService).isEmployeeAvailableForTask(EMPLOYEE_USER_ID);
            verify(employeeService).getEmployeeByUserId(EMPLOYEE_USER_ID);
            verify(employeeService, never()).isEmployeeAvailableForTask(SPOOFED_USER_ID);
            verify(employeeService, never()).getEmployeeByUserId(SPOOFED_USER_ID);
        }
    }
}
