package com.jhan.userapi.services;

import com.jhan.userapi.models.PasswordResetToken;
import com.jhan.userapi.models.User;
import com.jhan.userapi.repositorys.PasswordResetTokenRepository;
import com.jhan.userapi.repositorys.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Token válido por 1 hora
    private static final int TOKEN_EXPIRY_HOURS = 1;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                                UserRepository userRepository,
                                PasswordEncoder passwordEncoder) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String createPasswordResetToken(String email) {
        // Eliminar tokens anteriores no usados para este email
        tokenRepository.deleteByEmail(email);

        // Verificar que el usuario existe
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No existe usuario con ese email"));

        // Generar token único
        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setEmail(email);
        resetToken.setExpiryDate(LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS));
        resetToken.setUsed(false);

        tokenRepository.save(resetToken);

        return token;
    }

    public void validateToken(String token) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido o no existe"));

        if (resetToken.isUsed()) {
            throw new IllegalArgumentException("El token ya ha sido utilizado");
        }

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("El token ha expirado");
        }
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        validateToken(token);

        PasswordResetToken resetToken = tokenRepository.findByToken(token).get();
        String email = resetToken.getEmail();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Marcar token como usado
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }

    // Método para testing: obtener el token generado para un email
    public Optional<String> getLatestTokenForEmail(String email) {
        return tokenRepository.findByEmailAndUsedFalse(email)
                .map(PasswordResetToken::getToken);
    }
}