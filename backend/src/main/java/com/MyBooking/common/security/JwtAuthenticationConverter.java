package com.MyBooking.common.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Converts a verified Spring Security OAuth2 Jwt into an authenticated Authentication token
 * backed by the application-level decoupled UserPrincipal.
 */
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Object userIdClaim = jwt.getClaim("userId");
        if (userIdClaim == null) {
            throw new BadCredentialsException("Invalid JWT token: missing mandatory 'userId' claim");
        }

        Long userId;
        if (userIdClaim instanceof Number number) {
            userId = number.longValue();
        } else {
            try {
                userId = Long.parseLong(userIdClaim.toString());
            } catch (NumberFormatException e) {
                throw new BadCredentialsException("Invalid JWT token: malformed 'userId' claim");
            }
        }

        String email = jwt.getSubject();
        if (!StringUtils.hasText(email)) {
            email = jwt.getClaimAsString("email");
        }
        if (!StringUtils.hasText(email)) {
            throw new BadCredentialsException("Invalid JWT token: missing subject/email claim");
        }

        String rawRole = jwt.getClaimAsString("role");
        if (!StringUtils.hasText(rawRole)) {
            rawRole = "CLIENT";
        }

        String normalizedRole = rawRole.toUpperCase();
        String authorityName = normalizedRole.startsWith("ROLE_") ? normalizedRole : "ROLE_" + normalizedRole;
        String cleanRole = normalizedRole.startsWith("ROLE_") ? normalizedRole.substring(5) : normalizedRole;

        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(authorityName));
        UserPrincipal userPrincipal = new UserPrincipal(userId, email, cleanRole, authorities);

        return new DecoupledJwtAuthenticationToken(jwt, authorities, userPrincipal);
    }

    /**
     * Specialized JwtAuthenticationToken that presents UserPrincipal as the primary authentication principal,
     * allowing controllers to inject @AuthenticationPrincipal UserPrincipal directly.
     */
    public static class DecoupledJwtAuthenticationToken extends JwtAuthenticationToken {
        private final UserPrincipal userPrincipal;

        public DecoupledJwtAuthenticationToken(Jwt jwt, Collection<? extends GrantedAuthority> authorities, UserPrincipal userPrincipal) {
            super(jwt, authorities, userPrincipal.getUsername());
            this.userPrincipal = userPrincipal;
            setAuthenticated(true);
        }

        @Override
        public Object getPrincipal() {
            return userPrincipal;
        }

        public UserPrincipal getUserPrincipal() {
            return userPrincipal;
        }
    }
}

