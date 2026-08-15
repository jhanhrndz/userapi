package com.jhan.userapi.utils;

import com.jhan.userapi.dto.AuthRequestDTO;
import com.jhan.userapi.dto.UserRequestDTO;
import com.jhan.userapi.models.Role;
import com.jhan.userapi.models.User;
import com.jhan.userapi.repositorys.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

public class TestDataBuilder {

    public static UserRequestDTO validUserRequest() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        return UserRequestDTO.builder()
                .username("testuser_" + unique)
                .email("test_" + unique + "@example.com")
                .password("Password123!")
                .firstName("Test")
                .lastName("User")
                .build();
    }

    public static UserRequestDTO adminUserRequest() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        return UserRequestDTO.builder()
                .username("admin_" + unique)
                .email("admin_" + unique + "@example.com")
                .password("AdminPass123!")
                .firstName("Admin")
                .lastName("User")
                .build();
    }

    public static AuthRequestDTO validAuthRequest(String username, String password) {
        return AuthRequestDTO.builder()
                .username(username)
                .password(password)
                .build();
    }

    public static User createAndSaveUser(UserRepository userRepository, PasswordEncoder passwordEncoder, Role role) {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setUsername("user_" + unique);
        user.setEmail("user_" + unique + "@example.com");
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(role);
        return userRepository.save(user);
    }

    public static String getAuthToken(MockMvc mockMvc, ObjectMapper objectMapper, String username, String password) throws Exception {
        AuthRequestDTO authRequest = validAuthRequest(username, password);
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    public static UserRequestDTO.UserRequestDTOBuilder userRequestBuilder() {
        return UserRequestDTO.builder();
    }
}