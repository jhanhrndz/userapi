package com.jhan.userapi.security;

import com.jhan.userapi.models.Role;
import com.jhan.userapi.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class CustomAuthenticationProviderTest {

    private CustomAuthenticationProvider provider;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        provider = new CustomAuthenticationProvider(userDetailsService, passwordEncoder);
    }

    @Test
    void authenticate_returnsAuthenticationForValidCredentials() {
        String username = "testuser";
        String password = "password123";
        String encodedPassword = "encodedPassword123";

        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setPassword(encodedPassword);
        user.setEmail("test@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(Role.USER);

        when(userDetailsService.loadUserByUsername(username)).thenReturn(user);
        when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password);
        var result = provider.authenticate(authToken);

        assertThat(result).isNotNull();
        assertThat(result.getPrincipal()).isEqualTo(user);
        assertThat(result.isAuthenticated()).isTrue();
    }

    @Test
    void authenticate_throwsBadCredentialsExceptionForWrongPassword() {
        String username = "testuser";
        String password = "wrongPassword";
        String encodedPassword = "encodedPassword123";

        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setPassword(encodedPassword);
        user.setEmail("test@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(Role.USER);

        when(userDetailsService.loadUserByUsername(username)).thenReturn(user);
        when(passwordEncoder.matches(password, encodedPassword)).thenReturn(false);

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password);

        assertThatThrownBy(() -> provider.authenticate(authToken))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid password");
    }

    @Test
    void authenticate_throwsBadCredentialsExceptionForNonExistentUser() {
        String username = "nonexistent";
        String password = "password123";

        when(userDetailsService.loadUserByUsername(username))
                .thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found"));

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password);

        assertThatThrownBy(() -> provider.authenticate(authToken))
                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class);
    }

    @Test
    void supports_returnsTrueForUsernamePasswordAuthenticationToken() {
        assertThat(provider.supports(UsernamePasswordAuthenticationToken.class)).isTrue();
    }

    @Test
    void supports_returnsFalseForOtherAuthenticationTypes() {
        assertThat(provider.supports(Object.class)).isFalse();
    }
}