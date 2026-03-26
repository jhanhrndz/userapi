package com.jhan.userapi.services;

import com.jhan.userapi.dto.UserRequestDTO;
import com.jhan.userapi.dto.UserResponseDTO;
import com.jhan.userapi.exceptions.DuplicateResourceException;
import com.jhan.userapi.exceptions.UserNotFoundException;
import com.jhan.userapi.models.Role;
import com.jhan.userapi.models.User;
import com.jhan.userapi.repositorys.UserRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponseDTO createUser(UserRequestDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) throw new DuplicateResourceException("Username already in use.");
        if (userRepository.existsByEmail(dto.getEmail())) throw new DuplicateResourceException("Email already in use.");

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
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

    public UserResponseDTO updateUser(Long id, UserRequestDTO dto){
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
