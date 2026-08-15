package com.jhan.userapi.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("userSecurity")
public class UserSecurity {

    public boolean isOwner(Authentication authentication, Long userId) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return false;
        }
        
        if (authentication.getPrincipal() instanceof com.jhan.userapi.models.User user) {
            return user.getId().equals(userId);
        }
        
        return false;
    }
}