package com.MyBooking.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtAuthenticationConverterTest {

    private JwtAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        converter = new JwtAuthenticationConverter();
    }

    private Jwt createJwt(Map<String, Object> claims, String subject) {
        return new Jwt(
            "dummy-token-value",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("alg", "HS256", "typ", "JWT"),
            claims
        );
    }

    @Test
    @DisplayName("Should successfully convert valid JWT with Long userId into UserPrincipal")
    void shouldConvertValidJwtWithLongUserId() {
        Map<String, Object> claims = Map.of(
            "userId", 123L,
            "sub", "user@mybooking.com",
            "role", "EMPLOYEE"
        );
        Jwt jwt = createJwt(claims, "user@mybooking.com");

        AbstractAuthenticationToken auth = converter.convert(jwt);

        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_EMPLOYEE");
        assertThat(auth.getPrincipal()).isInstanceOf(UserPrincipal.class);

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        assertThat(principal.getId()).isEqualTo(123L);
        assertThat(principal.getEmail()).isEqualTo("user@mybooking.com");
        assertThat(principal.getRole()).isEqualTo("EMPLOYEE");
    }

    @Test
    @DisplayName("Should convert JWT when userId is Integer/Number")
    void shouldConvertWhenUserIdIsInteger() {
        Map<String, Object> claims = Map.of(
            "userId", 456,
            "sub", "client@test.com",
            "role", "CLIENT"
        );
        Jwt jwt = createJwt(claims, "client@test.com");

        AbstractAuthenticationToken auth = converter.convert(jwt);

        assertThat(auth).isNotNull();
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        assertThat(principal.getId()).isEqualTo(456L);
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_CLIENT");
    }

    @Test
    @DisplayName("Should convert JWT when userId is numeric String")
    void shouldConvertWhenUserIdIsString() {
        Map<String, Object> claims = Map.of(
            "userId", "789",
            "sub", "admin@test.com",
            "role", "ADMIN"
        );
        Jwt jwt = createJwt(claims, "admin@test.com");

        AbstractAuthenticationToken auth = converter.convert(jwt);

        assertThat(auth).isNotNull();
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        assertThat(principal.getId()).isEqualTo(789L);
        assertThat(principal.getRole()).isEqualTo("ADMIN");
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Should preserve existing ROLE_ prefix without duplicate ROLE_ROLE_")
    void shouldHandleExistingRolePrefix() {
        Map<String, Object> claims = Map.of(
            "userId", 10L,
            "sub", "admin@test.com",
            "role", "ROLE_ADMIN"
        );
        Jwt jwt = createJwt(claims, "admin@test.com");

        AbstractAuthenticationToken auth = converter.convert(jwt);

        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        assertThat(principal.getRole()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when userId claim is missing")
    void shouldThrowWhenUserIdIsMissing() {
        Map<String, Object> claims = Map.of(
            "sub", "user@test.com",
            "role", "CLIENT"
        );
        Jwt jwt = createJwt(claims, "user@test.com");

        assertThatThrownBy(() -> converter.convert(jwt))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessageContaining("missing mandatory 'userId' claim");
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when userId is non-numeric string")
    void shouldThrowWhenUserIdIsNotNumeric() {
        Map<String, Object> claims = Map.of(
            "userId", "invalid-id",
            "sub", "user@test.com",
            "role", "CLIENT"
        );
        Jwt jwt = createJwt(claims, "user@test.com");

        assertThatThrownBy(() -> converter.convert(jwt))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessageContaining("malformed 'userId' claim");
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when subject/email is missing")
    void shouldThrowWhenSubjectIsMissing() {
        Map<String, Object> claims = Map.of(
            "userId", 1L,
            "role", "CLIENT"
        );
        Jwt jwt = createJwt(claims, null);

        assertThatThrownBy(() -> converter.convert(jwt))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessageContaining("missing subject/email claim");
    }
}
