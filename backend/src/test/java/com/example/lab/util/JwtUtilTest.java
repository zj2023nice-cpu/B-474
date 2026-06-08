package com.example.lab.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
    }

    @Test
    void testGenerateToken_ShouldGenerateValidToken() {
        Long userId = 1L;
        String username = "testuser";
        String role = "ADMIN";

        String token = jwtUtil.generateToken(userId, username, role);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3);
    }

    @Test
    void testExtractAllClaims_ShouldExtractCorrectClaims() {
        Long userId = 1L;
        String username = "testuser";
        String role = "TEACHER";

        String token = jwtUtil.generateToken(userId, username, role);
        Claims claims = jwtUtil.extractAllClaims(token);

        assertNotNull(claims);
        assertEquals(userId, claims.get("userId", Long.class));
        assertEquals(username, claims.get("username", String.class));
        assertEquals(role, claims.get("role", String.class));
        assertEquals(username, claims.getSubject());
    }

    @Test
    void testExtractUsername_ShouldReturnCorrectUsername() {
        String username = "testuser";
        String token = jwtUtil.generateToken(1L, username, "STUDENT");

        String extractedUsername = jwtUtil.extractUsername(token);

        assertEquals(username, extractedUsername);
    }

    @Test
    void testExtractUserId_ShouldReturnCorrectUserId() {
        Long userId = 123L;
        String token = jwtUtil.generateToken(userId, "testuser", "ADMIN");

        Long extractedUserId = jwtUtil.extractUserId(token);

        assertEquals(userId, extractedUserId);
    }

    @Test
    void testExtractRole_ShouldReturnCorrectRole() {
        String role = "TEACHER";
        String token = jwtUtil.generateToken(1L, "testuser", role);

        String extractedRole = jwtUtil.extractRole(token);

        assertEquals(role, extractedRole);
    }

    @Test
    void testExtractExpiration_ShouldReturnFutureDate() {
        String token = jwtUtil.generateToken(1L, "testuser", "ADMIN");

        java.util.Date expiration = jwtUtil.extractExpiration(token);

        assertNotNull(expiration);
        assertTrue(expiration.after(new java.util.Date()));
    }

    @Test
    void testIsTokenExpired_ShouldReturnFalseForValidToken() {
        String token = jwtUtil.generateToken(1L, "testuser", "ADMIN");

        assertFalse(jwtUtil.isTokenExpired(token));
    }

    @Test
    void testIsTokenExpired_ShouldReturnTrueForInvalidToken() {
        String invalidToken = "invalid.token.here";

        assertTrue(jwtUtil.isTokenExpired(invalidToken));
    }

    @Test
    void testValidateToken_ShouldReturnTrueForValidToken() {
        String token = jwtUtil.generateToken(1L, "testuser", "ADMIN");

        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void testValidateToken_ShouldReturnFalseForInvalidToken() {
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjEsInVzZXJuYW1lIjoidGVzdCIsInJvbGUiOiJBRE1JTiIsImlhdCI6MTYwMDAsImV4cCI6MTcwMDB9.invalid-signature";

        assertFalse(jwtUtil.validateToken(invalidToken));
    }

    @Test
    void testValidateToken_ShouldReturnFalseForMalformedToken() {
        String malformedToken = "not-a-jwt-token";

        assertFalse(jwtUtil.validateToken(malformedToken));
    }

    @Test
    void testFullTokenFlow_ShouldWorkCorrectly() {
        Long userId = 456L;
        String username = "john_doe";
        String role = "STUDENT";

        String token = jwtUtil.generateToken(userId, username, role);

        assertTrue(jwtUtil.validateToken(token));
        assertEquals(userId, jwtUtil.extractUserId(token));
        assertEquals(username, jwtUtil.extractUsername(token));
        assertEquals(role, jwtUtil.extractRole(token));
    }

    @Test
    void testDifferentTokens_ShouldHaveDifferentSignatures() {
        String token1 = jwtUtil.generateToken(1L, "user1", "ADMIN");
        String token2 = jwtUtil.generateToken(2L, "user2", "TEACHER");

        assertNotEquals(token1, token2);
    }

    @Test
    void testExtractAllClaims_ShouldThrowExceptionForInvalidToken() {
        String invalidToken = "invalid.token";

        assertThrows(Exception.class, () -> {
            jwtUtil.extractAllClaims(invalidToken);
        });
    }
}
