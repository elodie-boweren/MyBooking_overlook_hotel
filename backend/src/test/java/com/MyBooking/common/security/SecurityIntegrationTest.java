package com.MyBooking.common.security;

import com.MyBooking.auth.domain.Role;
import com.MyBooking.auth.domain.User;
import com.MyBooking.employee.domain.Employee;
import com.MyBooking.employee.domain.EmployeeStatus;
import com.MyBooking.employee.service.EmployeeService;
import com.MyBooking.feedback.service.FeedbackService;
import com.MyBooking.hotel_management.HotelManagementApplication;
import com.MyBooking.reservation.service.ReservationService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = HotelManagementApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private EmployeeService employeeService;

    @MockBean
    private ReservationService reservationService;

    @MockBean
    private FeedbackService feedbackService;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtService.getSecretKey().getBytes(StandardCharsets.UTF_8));
    }

    @Nested
    @DisplayName("1. Authentication & Token Validity Tests")
    class AuthenticationTests {

        @Test
        @DisplayName("Unauthenticated request to protected endpoint should return 401 with WWW-Authenticate header")
        void unauthenticatedRequest_ShouldReturn401() throws Exception {
            mockMvc.perform(get("/api/employee/profile"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")));
        }

        @Test
        @DisplayName("Unauthenticated request to method-protected endpoint should return 401")
        void unauthenticatedRequestToMethodProtectedEndpoint_ShouldReturn401() throws Exception {
            mockMvc.perform(get("/api/rooms"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Tampered token signature should return 401 invalid_token")
        void tamperedTokenSignature_ShouldReturn401() throws Exception {
            String validToken = jwtService.generateToken(1L, "user@hotel.com", "CLIENT");
            // Corrupt the signature part (last segment)
            String tamperedToken = validToken.substring(0, validToken.length() - 6) + "XXXXXX";

            mockMvc.perform(get("/api/employee/profile")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tamperedToken))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("invalid_token")));
        }

        @Test
        @DisplayName("Expired token should return 401 invalid_token")
        void expiredToken_ShouldReturn401() throws Exception {
            String expiredToken = Jwts.builder()
                    .subject("expired@hotel.com")
                    .claim("userId", 1L)
                    .claim("role", "CLIENT")
                    .issuedAt(new Date(System.currentTimeMillis() - 200_000))
                    .expiration(new Date(System.currentTimeMillis() - 100_000))
                    .signWith(getSigningKey(), Jwts.SIG.HS256)
                    .compact();

            mockMvc.perform(get("/api/employee/profile")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("invalid_token")));
        }

        @Test
        @DisplayName("Malformed token string should return 401 invalid_token")
        void malformedToken_ShouldReturn401() throws Exception {
            mockMvc.perform(get("/api/employee/profile")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer completely-not-a-jwt-token"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("invalid_token")));
        }
    }

    @Nested
    @DisplayName("2. Role-Based Access Control & Method Security Tests")
    class AuthorizationTests {

        @Test
        @DisplayName("Cross-role: CLIENT token accessing EMPLOYEE endpoint should return 403 insufficient_scope")
        void clientAccessToEmployeeEndpoint_ShouldReturn403() throws Exception {
            String clientToken = jwtService.generateToken(2L, "client@hotel.com", "CLIENT");

            mockMvc.perform(get("/api/employee/profile")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken))
                    .andExpect(status().isForbidden())
                    .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("insufficient_scope")));
        }

        @Test
        @DisplayName("Cross-role: EMPLOYEE token accessing ADMIN endpoint should return 403 insufficient_scope")
        void employeeAccessToAdminEndpoint_ShouldReturn403() throws Exception {
            String employeeToken = jwtService.generateToken(3L, "employee@hotel.com", "EMPLOYEE");

            mockMvc.perform(get("/api/admin/reservations")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
                    .andExpect(status().isForbidden())
                    .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("insufficient_scope")));
        }

        @Test
        @DisplayName("Cross-role: CLIENT token accessing ADMIN endpoint should return 403 insufficient_scope")
        void clientAccessToAdminEndpoint_ShouldReturn403() throws Exception {
            String clientToken = jwtService.generateToken(4L, "client2@hotel.com", "CLIENT");

            mockMvc.perform(get("/api/admin/reservations")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken))
                    .andExpect(status().isForbidden())
                    .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("insufficient_scope")));
        }
    }

    @Nested
    @DisplayName("3. Authorized Access Tests")
    class AuthorizedAccessTests {

        @Test
        @DisplayName("Authorized EMPLOYEE token accessing employee profile should return 200 OK")
        void authorizedEmployee_ShouldReturn200() throws Exception {
            Long employeeUserId = 10L;
            String employeeToken = jwtService.generateToken(employeeUserId, "alice@hotel.com", "EMPLOYEE");

            User user = new User();
            user.setId(employeeUserId);
            user.setEmail("alice@hotel.com");
            user.setFirstName("Alice");
            user.setLastName("Smith");
            user.setRole(Role.EMPLOYEE);

            Employee employee = new Employee(user, EmployeeStatus.ACTIVE, "Front Desk");
            employee.setUserId(employeeUserId);

            when(employeeService.getEmployeeByUserId(employeeUserId)).thenReturn(employee);

            mockMvc.perform(get("/api/employee/profile")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Authorized CLIENT token accessing my feedbacks should return 200 OK")
        void authorizedClient_ShouldReturn200() throws Exception {
            Long clientId = 5L;
            String clientToken = jwtService.generateToken(clientId, "bob@hotel.com", "CLIENT");

            when(feedbackService.getFeedbacksByUserIdAsDto(eq(clientId), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/feedback/my")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Authorized ADMIN token accessing reservations should return 200 OK")
        void authorizedAdmin_ShouldReturn200() throws Exception {
            Long adminId = 1L;
            String adminToken = jwtService.generateToken(adminId, "admin@hotel.com", "ADMIN");

            when(reservationService.getAllReservationsAsDto(any(Pageable.class)))
                    .thenReturn(Page.empty());

            mockMvc.perform(get("/api/admin/reservations")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("4. Public Endpoint Tests")
    class PublicEndpointTests {

        @Test
        @DisplayName("Public health endpoint should be accessible without token returning 200 OK")
        void publicHealthEndpoint_ShouldReturn200() throws Exception {
            mockMvc.perform(get("/api/auth/health"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Auth service is running"));
        }
    }
}

