package com.jhan.userapi.controller;

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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class UserControllerSecurityTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String userToken;
    private User adminUser;
    private User regularUser;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();

        // Create admin user
        adminUser = new User();
        adminUser.setUsername("adminuser");
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword(passwordEncoder.encode("AdminPass123!"));
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setRole(Role.ADMIN);
        adminUser = userRepository.save(adminUser);

        // Create regular user
        regularUser = new User();
        regularUser.setUsername("regularuser");
        regularUser.setEmail("regular@example.com");
        regularUser.setPassword(passwordEncoder.encode("UserPass123!"));
        regularUser.setFirstName("Regular");
        regularUser.setLastName("User");
        regularUser.setRole(Role.USER);
        regularUser = userRepository.save(regularUser);

        // Get tokens
        adminToken = TestDataBuilder.getAuthToken(mockMvc, objectMapper, "adminuser", "AdminPass123!");
        userToken = TestDataBuilder.getAuthToken(mockMvc, objectMapper, "regularuser", "UserPass123!");
    }

    // ========== ADMIN ACCESS TESTS ==========

    @Test
    void admin_getAllUsers_returns200() throws Exception {
        mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void admin_getUserById_returns200() throws Exception {
        mockMvc.perform(get("/users/" + regularUser.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("regularuser"));
    }

    @Test
    void admin_createUser_returns200() throws Exception {
        UserRequestDTO request = TestDataBuilder.validUserRequest();

        mockMvc.perform(post("/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(request.getUsername()));
    }

    @Test
    void admin_updateUser_returns200() throws Exception {
        UserRequestDTO request = UserRequestDTO.builder()
                .username("updateduser")
                .email("updated@example.com")
                .password("NewPass123!")
                .firstName("Updated")
                .lastName("User")
                .build();

        mockMvc.perform(put("/users/" + regularUser.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("updateduser"));
    }

    @Test
    void admin_deleteUser_returns204() throws Exception {
        mockMvc.perform(delete("/users/" + regularUser.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    // ========== USER ACCESS TESTS ==========

    @Test
    void user_getOwnProfile_returns200() throws Exception {
        // USER can access their own profile
        mockMvc.perform(get("/users/" + regularUser.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("regularuser"));
    }

    @Test
    void user_getOtherProfile_returns403() throws Exception {
        // USER cannot access other user's profile
        mockMvc.perform(get("/users/" + adminUser.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void user_createUser_returns403() throws Exception {
        UserRequestDTO request = TestDataBuilder.validUserRequest();

        mockMvc.perform(post("/users")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void user_updateUser_returns403() throws Exception {
        UserRequestDTO request = UserRequestDTO.builder()
                .username("updateduser")
                .email("updated@example.com")
                .password("NewPass123!")
                .firstName("Updated")
                .lastName("User")
                .build();

        mockMvc.perform(put("/users/" + regularUser.getId())
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void user_deleteUser_returns403() throws Exception {
        mockMvc.perform(delete("/users/" + regularUser.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ========== UNAUTHENTICATED ACCESS TESTS ==========

    @Test
    void noToken_getUsers_returns401() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidToken_getUsers_returns401() throws Exception {
        mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedToken_getUsers_returns401() throws Exception {
        mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void noToken_createUser_returns401() throws Exception {
        UserRequestDTO request = TestDataBuilder.validUserRequest();

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}