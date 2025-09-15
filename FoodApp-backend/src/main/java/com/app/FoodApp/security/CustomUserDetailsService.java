package com.app.FoodApp.security;

import com.app.FoodApp.authUsers.entities.User;
import com.app.FoodApp.authUsers.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// CustomUserDetailsService loads your domain User from the DB.
// AuthUser adapts the entity into a Spring Security-friendly UserDetails.

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return AuthUser.builder()
                .user(user)
                .build();
    }
}
