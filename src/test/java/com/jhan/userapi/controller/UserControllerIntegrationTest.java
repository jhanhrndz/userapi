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
class UserControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();

        User adminUser = new User();
        adminUser.setUsername("admincrud");
        adminUser.setEmail("admincrud@example.com");
        adminUser.setPassword(passwordEncoder.encode("AdminPass123!"));
        adminUser.setFirstName("Admin");
        adminUser.setLastName("CRUD");
        adminUser.setRole(Role.ADMIN);
        userRepository.save(adminUser);

        adminToken = TestDataBuilder.getAuthToken(mockMvc, objectMapper, "admincrud", "AdminPass123!");
    }

    @Test
    void fullCrudCycle_createReadUpdateDelete() throws Exception {
        UserRequestDTO createRequest = TestDataBuilder.validUserRequest();
        String createResponse = mockMvc.perform(post("/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(createRequest.getUsername()))
                .andReturn().getResponse().getContentAsString();

        Long createdUserId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(get("/users/" + createdUserId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(createRequest.getUsername()));

        mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        UserRequestDTO updateRequest = UserRequestDTO.builder()
                .username("updatedusername")
                .email("updated@example.com")
                .password("NewPass123!")
                .firstName("Updated")
                .lastName("Name")
                .build();

        mockMvc.perform(put("/users/" + createdUserId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("updatedusername"));

        mockMvc.perform(delete("/users/" + createdUserId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/" + createdUserId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllUsers_emptyList_returnsEmptyArray() throws Exception {
        userRepository.deleteAll();

        User adminOnly = new User();
        adminOnly.setUsername("adminonly");
        adminOnly.setEmail("adminonly@example.com");
        adminOnly.setPassword(passwordEncoder.encode("AdminPass123!"));
        adminOnly.setFirstName("Admin");
        adminOnly.setLastName("Only");
        adminOnly.setRole(Role.ADMIN);
        userRepository.save(adminOnly);

        String token = TestDataBuilder.getAuthToken(mockMvc, objectMapper, "adminonly", "AdminPass123!");

        mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getUserById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/users/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateUser_notFound_returns404() throws Exception {
        UserRequestDTO request = TestDataBuilder.validUserRequest();

        mockMvc.perform(put("/users/999999")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/users/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void createUser_duplicateUsername_returns409() throws Exception {
        UserRequestDTO request = TestDataBuilder.validUserRequest();

        mockMvc.perform(post("/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}