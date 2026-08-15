package com.jhan.userapi.security;

import com.jhan.userapi.models.Role;
import com.jhan.userapi.models.User;
import com.jhan.userapi.repositorys.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class CustomUserDetailsServiceTest {

    private CustomUserDetailsService userDetailsService;

    @Mock
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userDetailsService = new CustomUserDetailsService(userRepository);
    }

    @Test
    void loadUserByUsername_returnsUserWhenExists() {
        String username = "testuser";
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(Role.USER);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        var result = userDetailsService.loadUserByUsername(username);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username);
        assertThat(result.getAuthorities()).hasSize(1);
    }

    @Test
    void loadUserByUsername_throwsExceptionWhenUserNotFound() {
        String username = "nonexistent";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(username))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found: " + username);
    }

    @Test
    void loadUserByUsername_returnsUserWithCorrectAuthorities() {
        String username = "adminuser";
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setEmail("admin@example.com");
        user.setPassword("encodedPassword");
        user.setFirstName("Admin");
        user.setLastName("User");
        user.setRole(Role.ADMIN);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        var result = userDetailsService.loadUserByUsername(username);

        assertThat(result.getAuthorities()).hasSize(1);
        assertThat(result.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
    }
}