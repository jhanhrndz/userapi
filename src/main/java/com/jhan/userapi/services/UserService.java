package com.jhan.userapi.services;

import com.jhan.userapi.dto.UserRequestDTO;
import com.jhan.userapi.dto.UserResponseDTO;
import com.jhan.userapi.exceptions.DuplicateResourceException;
import com.jhan.userapi.exceptions.UserNotFoundException;
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

        User savedUser = userRepository.save(user);

        return new UserResponseDTO(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail()
        );
    }

    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(user -> new UserResponseDTO(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail()
                ))
                .toList();
    }

    public UserResponseDTO getUserById(Long id){
        User user = findUserEntityById(id);

        return new UserResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail()
        );
    }

    public void deleteUser(Long id){
        User user = findUserEntityById(id);
        userRepository.delete(user);
    }

    public UserResponseDTO updateUser(Long id, UserRequestDTO dto){
        User user = findUserEntityById(id);
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());

        User updatedUser = userRepository.save(user);
        return new UserResponseDTO(
                updatedUser.getId(),
                updatedUser.getUsername(),
                updatedUser.getEmail()
        );
    }

    private User findUserEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}
