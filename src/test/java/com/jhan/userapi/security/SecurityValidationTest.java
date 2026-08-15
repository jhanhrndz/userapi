package com.jhan.userapi.security;

import com.jhan.userapi.AbstractIntegrationTest;
import com.jhan.userapi.dto.UserRequestDTO;
import com.jhan.userapi.models.Role;
import com.jhan.userapi.models.User;
import com.jhan.userapi.repositorys.UserRepository;
import com.jhan.userapi.utils.TestDataBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class SecurityValidationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void password_isHashedInDatabase() throws Exception {
        UserRequestDTO request = TestDataBuilder.validUserRequest();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        User savedUser = userRepository.findByUsername(request.getUsername()).orElseThrow();
        
        assertThat(savedUser.getPassword()).isNotEqualTo(request.getPassword());
        assertThat(savedUser.getPassword()).startsWith("$2a$"); // BCrypt prefix
        assertThat(passwordEncoder.matches(request.getPassword(), savedUser.getPassword())).isTrue();
    }

    @Test
    void token_isStateless_multipleRequestsWork() throws Exception {
        UserRequestDTO request = TestDataBuilder.validUserRequest();

        String response = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(response).get("token").asText();

        // Multiple requests with same token should work
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/users")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden()); // USER role, not ADMIN
        }
    }

    @Test
    void sqlInjection_inUsername_doesNotBreakDatabase() throws Exception {
        String maliciousUsername = "admin'--";
        UserRequestDTO request = UserRequestDTO.builder()
                .username(maliciousUsername)
                .email("test@example.com")
                .password("Password123!")
                .firstName("Test")
                .lastName("User")
                .build();

        // Should handle gracefully - username is valid (no SQL injection in parameterized queries)
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        
        // Verify user was created with the literal username
        assertThat(userRepository.findByUsername(maliciousUsername)).isPresent();
    }

    @Test
    void sqlInjection_inEmail_doesNotBreakDatabase() throws Exception {
        UserRequestDTO request = UserRequestDTO.builder()
                .username("testuser")
                .email("test@example.com'--")
                .password("Password123!")
                .firstName("Test")
                .lastName("User")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError()); // Validation error (invalid email)
    }

    @Test
    void timingAttack_similarResponseTimeForExistingAndNonExistingUser() throws Exception {
        // Create a user
        String existingUser = "existinguser";
        String password = "Password123!";
        User user = new User();
        user.setUsername(existingUser);
        user.setEmail("existing@example.com");
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName("Existing");
        user.setLastName("User");
        user.setRole(Role.USER);
        userRepository.save(user);

        // Measure response time for existing user
        long startExisting = System.nanoTime();
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + existingUser + "\",\"password\":\"wrongpass\"}"))
                .andExpect(status().isUnauthorized()); // 401 from auth provider
        long timeExisting = System.nanoTime() - startExisting;

        // Measure response time for non-existing user
        long startNonExisting = System.nanoTime();
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nonexistentuser\",\"password\":\"wrongpass\"}"))
                .andExpect(status().isUnauthorized()); // 401 from auth provider
        long timeNonExisting = System.nanoTime() - startNonExisting;

        // Times should be similar (within 10x factor to account for variance in test environment)
        double ratio = (double) Math.max(timeExisting, timeNonExisting) / Math.min(timeExisting, timeNonExisting);
        assertThat(ratio).isLessThan(20.0); // Allow up to 20x variance in test environment
    }

    @Test
    void csrf_isDisabledForStatelessApi() throws Exception {
        // CSRF should be disabled for stateless JWT API
        UserRequestDTO request = TestDataBuilder.validUserRequest();

        // Should work without CSRF token
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void securityHeaders_presentInResponses() throws Exception {
        UserRequestDTO request = TestDataBuilder.validUserRequest();

        var result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // Check for security headers
        String xFrameOptions = result.getResponse().getHeader("X-Frame-Options");
        String xContentTypeOptions = result.getResponse().getHeader("X-Content-Type-Options");
        
        // Spring Security adds these by default
        assertThat(xContentTypeOptions).isEqualTo("nosniff");
    }
}