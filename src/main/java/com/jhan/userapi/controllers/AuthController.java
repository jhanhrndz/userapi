package com.jhan.userapi.controllers;

import com.jhan.userapi.dto.AuthRequestDTO;
import com.jhan.userapi.dto.AuthResponseDTO;
import com.jhan.userapi.dto.ForgotPasswordRequestDTO;
import com.jhan.userapi.dto.ResetPasswordRequestDTO;
import com.jhan.userapi.dto.UserRequestDTO;
import com.jhan.userapi.security.CustomUserDetailsService;
import com.jhan.userapi.security.JwtService;
import com.jhan.userapi.services.PasswordResetService;
import com.jhan.userapi.services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;
    private final CustomUserDetailsService customUserDetailsService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService, 
                          UserService userService, CustomUserDetailsService customUserDetailsService,
                          PasswordResetService passwordResetService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userService = userService;
        this.customUserDetailsService = customUserDetailsService;
        this.passwordResetService = passwordResetService;
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

    @PostMapping("/forgot-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        String token = passwordResetService.createPasswordResetToken(request.getEmail());

        // En producción: enviar email con link http://localhost:8080/reset-password?token={token}
        // Para testing: retornamos el token en la respuesta
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Se ha enviado un enlace de restablecimiento a su email");
        response.put("resetLink", "http://localhost:8080/reset-password?token=" + token);
        response.put("token", token); // Solo para testing, remover en producción

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Contraseña restablecida correctamente");

        return ResponseEntity.ok(response);
    }
}