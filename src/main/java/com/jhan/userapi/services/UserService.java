package com.jhan.userapi.services;

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

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id){
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public void deleteUser(Long id){
        User user = getUserById(id);
        userRepository.delete(user);
    }

    public User updateUser(Long id, User userDetails){
        User user = getUserById(id);
        if (user != null){
            user.setUsername(userDetails.getUsername());
            user.setEmail(userDetails.getEmail());
            return userRepository.save(user);
        }
        return null;
    }
}
