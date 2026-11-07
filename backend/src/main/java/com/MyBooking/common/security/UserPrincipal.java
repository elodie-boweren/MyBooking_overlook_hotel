package com.MyBooking.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Decoupled application-level principal representing an authenticated user.
 * Decouples domain identity resolution from specific IdP token structures or libraries.
 */
public class UserPrincipal implements UserDetails, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String email;
    private final String role;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Long id, String email, String role) {
        this(id, email, role, role != null ? List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())) : Collections.emptyList());
    }

    public UserPrincipal(Long id, String email, String role, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.authorities = authorities != null ? Collections.unmodifiableCollection(authorities) : Collections.emptyList();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        // OAuth2 Resource Server tokens carry verified identity; passwords are not managed or held by resource servers
        return null;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPrincipal that = (UserPrincipal) o;
        return Objects.equals(id, that.id) && Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email);
    }

    @Override
    public String toString() {
        return "UserPrincipal{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", authorities=" + authorities +
                '}';
    }
}

