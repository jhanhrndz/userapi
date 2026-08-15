package com.jhan.userapi.security;

import com.jhan.userapi.AbstractIntegrationTest;
import com.jhan.userapi.models.Role;
import com.jhan.userapi.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest extends AbstractIntegrationTest {

    @Autowired
    private JwtService jwtService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRole(Role.USER);
    }

    @Test
    void generateToken_returnsValidToken() {
        String token = jwtService.generateToken(testUser);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void generateToken_withExtraClaims_includesClaims() {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("customClaim", "customValue");

        String token = jwtService.generateToken(testUser, extraClaims);

        assertThat(token).isNotNull();
        String extractedClaim = jwtService.extractClaim(token, claims -> claims.get("customClaim", String.class));
        assertThat(extractedClaim).isEqualTo("customValue");
    }

    @Test
    void extractUsername_returnsCorrectUsername() {
        String token = jwtService.generateToken(testUser);

        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("testuser");
    }

    @Test
    void extractClaim_returnsCorrectClaim() {
        String token = jwtService.generateToken(testUser);

        Date expiration = jwtService.extractClaim(token, claims -> claims.getExpiration());

        assertThat(expiration).isNotNull();
        assertThat(expiration).isAfter(new Date());
    }

    @Test
    void isTokenValid_returnsTrueForValidToken() {
        String token = jwtService.generateToken(testUser);

        boolean isValid = jwtService.isTokenValid(token, testUser);

        assertThat(isValid).isTrue();
    }

    @Test
    void isTokenValid_returnsFalseForDifferentUser() {
        String token = jwtService.generateToken(testUser);

        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@example.com");
        otherUser.setPassword("encodedPassword");
        otherUser.setFirstName("Other");
        otherUser.setLastName("User");
        otherUser.setRole(Role.USER);

        boolean isValid = jwtService.isTokenValid(token, otherUser);

        assertThat(isValid).isFalse();
    }

    @Test
    void generateToken_createsDifferentTokensForSameUser() {
        Map<String, Object> claims1 = Map.of("nonce", 1);
        Map<String, Object> claims2 = Map.of("nonce", 2);
        
        String token1 = jwtService.generateToken(testUser, claims1);
        String token2 = jwtService.generateToken(testUser, claims2);

        assertThat(token1).isNotEqualTo(token2);
    }
}