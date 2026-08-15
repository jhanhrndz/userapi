package com.jhan.userapi.controllers;

import com.jhan.userapi.dto.AuthRequestDTO;
import com.jhan.userapi.dto.AuthResponseDTO;
import com.jhan.userapi.dto.UserRequestDTO;
import com.jhan.userapi.security.CustomUserDetailsService;
import com.jhan.userapi.security.JwtService;
import com.jhan.userapi.services.UserService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;
    private final CustomUserDetailsService customUserDetailsService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService, 
                          UserService userService, CustomUserDetailsService customUserDetailsService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userService = userService;
        this.customUserDetailsService = customUserDetailsService;
    }

    @PostMapping("/login")
    public AuthResponseDTO login(@Valid @RequestBody AuthRequestDTO request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(request.getUsername());
        var user = (com.jhan.userapi.models.User) userDetails;

        String token = jwtService.generateToken(userDetails);

        return new AuthResponseDTO(
            token,
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRole()
        );
    }

    @PostMapping("/register")
    public AuthResponseDTO register(@Valid @RequestBody UserRequestDTO request) {
        userService.createUser(request);
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(request.getUsername());
        
        String token = jwtService.generateToken(userDetails);

        var user = (com.jhan.userapi.models.User) userDetails;
        return new AuthResponseDTO(
            token,
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRole()
        );
    }
}