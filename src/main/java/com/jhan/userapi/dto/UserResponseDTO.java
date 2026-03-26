package com.jhan.userapi.dto;

import com.jhan.userapi.models.Role;

import java.time.LocalDateTime;

public record UserResponseDTO(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        Role role,
        LocalDateTime createdAt,
        LocalDateTime updateAt
) { }
