package com.jhan.userapi.services;

import com.jhan.userapi.dto.UserRequestDTO;
import com.jhan.userapi.dto.UserResponseDTO;
import com.jhan.userapi.dto.UserUpdatePasswordDTO;
import com.jhan.userapi.dto.UserUpdateRequestDTO;
import com.jhan.userapi.exceptions.DuplicateResourceException;
import com.jhan.userapi.exceptions.InvalidCredentialsException;
import com.jhan.userapi.exceptions.UserNotFoundException;
import com.jhan.userapi.models.Role;
import com.jhan.userapi.models.User;
import com.jhan.userapi.repositorys.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponseDTO createUser(UserRequestDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) throw new DuplicateResourceException("Username already in use.");
        if (userRepository.existsByEmail(dto.getEmail())) throw new DuplicateResourceException("Email already in use.");

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setRole(Role.USER);

        User savedUser = userRepository.save(user);

        return mapToResponseDTO(savedUser);
    }

    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    public UserResponseDTO getUserById(Long id){
        User user = findUserEntityById(id);
        return mapToResponseDTO(user);
    }

    public void deleteUser(Long id){
        User user = findUserEntityById(id);
        userRepository.delete(user);
    }

    public UserResponseDTO updateProfile(Long id, UserUpdateRequestDTO dto){
        if (userRepository.existsByUsernameAndIdNot(dto.getUsername(), id)) throw new DuplicateResourceException("Username already in use.");
        if (userRepository.existsByEmailAndIdNot(dto.getEmail(), id)) throw new DuplicateResourceException("Email already in use.");

        User user = findUserEntityById(id);
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());

        User updatedUser = userRepository.save(user);
        return mapToResponseDTO(updatedUser);
    }

    public void updatePassword(Long id, UserUpdatePasswordDTO dto){
        User user = findUserEntityById(id);

        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new InvalidCredentialsException("Las contraseñas nuevas no coinciden");
        }

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("La contraseña actual es incorrecta");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }

    private User findUserEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    private UserResponseDTO mapToResponseDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}