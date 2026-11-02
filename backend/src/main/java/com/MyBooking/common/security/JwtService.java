package com.MyBooking.common.security; 

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration; 

    public String generateToken(Long userId, String username, String role) {
        return Jwts.builder()
            .subject(username)
            .claim("userId", userId)
            .claim("role", role)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(getSigningKey(), Jwts.SIG.HS256)
            .compact();
    }

    @Deprecated
    public String generateToken(String username, String role) {
        return generateToken(1L, username, role);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true; 
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    public String extractUsername(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject();
    }
    
    public String extractRole(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .get("role", String.class);
    }

    public Long extractUserId(String token) {
        Object userId = Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .get("userId");
        if (userId instanceof Number number) {
            return number.longValue();
        }
        return userId != null ? Long.parseLong(userId.toString()) : null;
    }
    
    public String getSecretKey() {
        return secretKey;
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
