package com.MyBooking.common.security;

import com.MyBooking.auth.domain.Role;
import com.MyBooking.auth.domain.User;
import com.MyBooking.auth.repository.UserRepository;
import com.MyBooking.hotel_management.HotelManagementApplication;
import com.MyBooking.loyalty.domain.LoyaltyAccount;
import com.MyBooking.loyalty.repository.LoyaltyAccountRepository;
import com.MyBooking.room.domain.Room;
import com.MyBooking.room.domain.RoomStatus;
import com.MyBooking.room.domain.RoomType;
import com.MyBooking.room.repository.RoomRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P3-T12: Controller & Security Layer Alignment Verification.
 */
@SpringBootTest(classes = HotelManagementApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Phase3ControllerSecurityAlignmentTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private LoyaltyAccountRepository loyaltyAccountRepository;
    @Autowired private RoomRepository roomRepository;

    @Test
    @DisplayName("P3-T12: Unauthenticated reset-password is rejected")
    void unauthenticatedResetPassword_ShouldBeRejected() throws Exception {
        mockMvc.perform(put("/api/auth/reset-password")
                        .param("email", "anyone@example.com")
                        .param("newPassword", "NewPassword123!"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("P3-T12: Self-registration forces CLIENT role even when ADMIN is requested, and validates @Valid")
    void register_ShouldForceClientRoleAndValidatePayload() throws Exception {
        String email = "selfreg-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String payload = """
                {
                  "email": "%s",
                  "password": "Password123!",
                  "firstName": "Self",
                  "lastName": "Reg",
                  "phone": "+1234567890",
                  "address": "123 Main St",
                  "birthDate": "1990-01-01",
                  "role": "ADMIN"
                }
                """.formatted(email);

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("role").asText()).isEqualTo("CLIENT");

        // Invalid payload (missing/short password, invalid email) -> 400 Bad Request
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("P3-T12: Loyalty account lookup blocks cross-client IDOR while allowing own access")
    void loyaltyAccountLookup_ShouldEnforceOwnershipForClients() throws Exception {
        User clientA = saveUser("loy-a-" + UUID.randomUUID() + "@test.com", Role.CLIENT);
        User clientB = saveUser("loy-b-" + UUID.randomUUID() + "@test.com", Role.CLIENT);

        LoyaltyAccount accB = new LoyaltyAccount();
        accB.setUser(clientB);
        accB.setBalance(750);
        accB = loyaltyAccountRepository.save(accB);

        Authentication authA = authFor(clientA);
        Authentication authB = authFor(clientB);

        // Client A cannot read Client B's loyalty account by accountId or userId
        mockMvc.perform(get("/api/loyalty/account/" + accB.getId()).with(authentication(authA)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/loyalty/account/user/" + clientB.getId()).with(authentication(authA)))
                .andExpect(status().isForbidden());

        // Client B can read their own loyalty account
        mockMvc.perform(get("/api/loyalty/account/" + accB.getId()).with(authentication(authB)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/loyalty/account/user/" + clientB.getId()).with(authentication(authB)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("P3-T12: Analytics dashboard enforces ADMIN role and RoomStatusUpdateRequest enforces @Valid")
    void analyticsAndRoomStatusValidation_ShouldBeEnforced() throws Exception {
        User client = saveUser("sec-client-" + UUID.randomUUID() + "@test.com", Role.CLIENT);
        User employee = saveUser("sec-emp-" + UUID.randomUUID() + "@test.com", Role.EMPLOYEE);
        User admin = saveUser("sec-admin-" + UUID.randomUUID() + "@test.com", Role.ADMIN);

        mockMvc.perform(get("/api/admin/analytics/dashboard").with(authentication(authFor(client))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/analytics/dashboard").with(authentication(authFor(admin))))
                .andExpect(status().isOk());

        Room room = new Room();
        room.setNumber("SEC-" + UUID.randomUUID().toString().substring(0, 8));
        room.setRoomType(RoomType.SINGLE);
        room.setCapacity(2);
        room.setPrice(new BigDecimal("100.00"));
        room.setCurrency("USD");
        room.setStatus(RoomStatus.AVAILABLE);
        room = roomRepository.save(room);

        mockMvc.perform(put("/api/rooms/employee/" + room.getId() + "/status")
                        .with(authentication(authFor(employee)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("P3-T12: Invalid filter params, malformed JSON, and missing employee tasks return 400/404 instead of 500")
    void exceptionHandling_ShouldReturn400And404InsteadOf500() throws Exception {
        User employee = saveUser("exc-emp-" + UUID.randomUUID() + "@test.com", Role.EMPLOYEE);
        User admin = saveUser("exc-admin-" + UUID.randomUUID() + "@test.com", Role.ADMIN);

        mockMvc.perform(get("/api/admin/reservations")
                        .param("status", "INVALID_STATUS")
                        .with(authentication(authFor(admin))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/employee/reservations/search")
                        .param("checkInFrom", "not-a-date")
                        .with(authentication(authFor(employee))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/employee/reservations/date-range")
                        .param("startDate", "bad-date")
                        .param("endDate", "2026-12-01")
                        .with(authentication(authFor(employee))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/employee/tasks/999999")
                        .with(authentication(authFor(employee)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest());
    }

    private Authentication authFor(User user) {
        UserPrincipal principal = new UserPrincipal(user.getId(), user.getEmail(), user.getRole().name());
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private User saveUser(String email, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setPassword("$2a$10$7EqJtq98hPqEX7fNZaFWoO5rN8U1C4C4C4C4C4C4C4C4C4C4C4C4C");
        u.setFirstName("Sec");
        u.setLastName("User");
        u.setPhone("1234567890");
        u.setAddress("123 Sec St");
        u.setBirthDate(LocalDate.of(1990, 1, 1));
        u.setRole(role);
        return userRepository.save(u);
    }
}
