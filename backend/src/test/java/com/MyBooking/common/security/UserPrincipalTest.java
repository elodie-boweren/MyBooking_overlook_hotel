package com.MyBooking.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserPrincipalTest {

    @Test
    @DisplayName("UserPrincipal should correctly hold and expose domain identity attributes")
    void shouldHoldDomainIdentityAttributes() {
        Long userId = 42L;
        String email = "staff@hotel.com";
        String role = "EMPLOYEE";
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));

        UserPrincipal principal = new UserPrincipal(userId, email, role, authorities);

        assertThat(principal.getId()).isEqualTo(userId);
        assertThat(principal.getEmail()).isEqualTo(email);
        assertThat(principal.getUsername()).isEqualTo(email);
        assertThat(principal.getRole()).isEqualTo(role);
        assertThat(principal.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_EMPLOYEE");
    }

    @Test
    @DisplayName("UserPrincipal should fulfill UserDetails contract without exposing passwords")
    void shouldFulfillUserDetailsContractWithoutPassword() {
        UserPrincipal principal = new UserPrincipal(1L, "guest@test.com", "CLIENT", List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));

        assertThat(principal.getPassword()).isNull();
        assertThat(principal.isAccountNonExpired()).isTrue();
        assertThat(principal.isAccountNonLocked()).isTrue();
        assertThat(principal.isCredentialsNonExpired()).isTrue();
        assertThat(principal.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("UserPrincipal should handle null authorities gracefully")
    void shouldHandleNullAuthoritiesGracefully() {
        UserPrincipal principal = new UserPrincipal(100L, "admin@hotel.com", "ADMIN", null);

        assertThat(principal.getAuthorities()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("UserPrincipal equality and hashcode based on id and email")
    void testEqualsAndHashCode() {
        UserPrincipal p1 = new UserPrincipal(1L, "user@test.com", "CLIENT", List.of());
        UserPrincipal p2 = new UserPrincipal(1L, "user@test.com", "CLIENT", List.of());
        UserPrincipal p3 = new UserPrincipal(2L, "other@test.com", "CLIENT", List.of());

        assertThat(p1).isEqualTo(p2);
        assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
        assertThat(p1).isNotEqualTo(p3);
    }
}
