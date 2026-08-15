package com.jhan.userapi.dto;

import com.jhan.userapi.models.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponseDTO {
    private String token;
    private Long id;
    private String username;
    private String email;
    private Role role;
}