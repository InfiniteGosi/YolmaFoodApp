package com.app.FoodApp.authUsers.services;

import com.app.FoodApp.authUsers.dtos.LoginRequest;
import com.app.FoodApp.authUsers.dtos.LoginResponse;
import com.app.FoodApp.authUsers.dtos.RegistrationRequest;
import com.app.FoodApp.authUsers.entities.User;
import com.app.FoodApp.authUsers.repositories.UserRepository;
import com.app.FoodApp.exceptions.BadRequestException;
import com.app.FoodApp.exceptions.NotFoundException;
import com.app.FoodApp.response.Response;
import com.app.FoodApp.role.entities.Role;
import com.app.FoodApp.role.repositories.RoleRepository;
import com.app.FoodApp.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RoleRepository roleRepository;

    /**
     * Registers a new user account.
     * Steps:
     * 1. Check if email already exists
     * 2. Collect roles from request (or assign default CUSTOMER role)
     * 3. Build User entity with encoded password and default metadata
     * 4. Save User in database
     * 5. Return success response
     *
     * @param registrationRequest DTO containing user details and optional roles
     * @return Response<?> indicating success or failure
     * @throws BadRequestException if email already exists
     * @throws NotFoundException if one or more roles do not exist
     */
    @Override
    public Response<?> register(RegistrationRequest registrationRequest) {
        log.info("Inside register method");

        // Step 1: Reject if email already exists
        if (userRepository.existsByEmail(registrationRequest.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        // Step 2: Collect roles from request, or fallback to CUSTOMER role
        List<Role> userRoles;
        if (registrationRequest.getRoles() != null && !registrationRequest.getRoles().isEmpty()) {
            userRoles = registrationRequest.getRoles().stream()
                    .map(roleName -> roleRepository.findByName(roleName.toUpperCase())
                            .orElseThrow(() -> new NotFoundException("Role with name: " + roleName + " not found")))
                    .toList();
        } else {
            // Default role assignment when none provided
            Role defaultRole = roleRepository.findByName("CUSTOMER")
                    .orElseThrow(() -> new NotFoundException("CUSTOMER role not found"));
            userRoles = List.of(defaultRole);
        }

        // Step 3: Build User entity with encoded password and default values
        User userToSave = User.builder()
                .name(registrationRequest.getName())
                .email(registrationRequest.getEmail())
                .password(passwordEncoder.encode(registrationRequest.getPassword()))
                .phoneNumber(registrationRequest.getPhoneNumber())
                .address(registrationRequest.getAddress())
                .roles(userRoles)
                .isActive(true) // New users are active by default
                .createdAt(LocalDateTime.now())
                .build();

        // Step 4: Persist user entity
        userRepository.save(userToSave);

        log.info("User registered successfully");

        // Step 5: Build success response
        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("User registered successfully")
                .build();
    }

    /**
     * Authenticates a user and generates a JWT token.
     * Steps:
     * 1. Validate user existence by email
     * 2. Ensure account is active
     * 3. Verify password against encoded hash
     * 4. Generate JWT token
     * 5. Return token and user roles in response
     *
     * @param loginRequest DTO containing email and password
     * @return Response<LoginResponse> with JWT token and list of roles
     * @throws NotFoundException if user is not found or inactive
     * @throws BadRequestException if password is invalid
     */
    @Override
    public Response<LoginResponse> login(LoginRequest loginRequest) {
        log.info("Inside login method");

        // Step 1: Find user by email or fail
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new NotFoundException("Email not found"));

        // Step 2: Ensure account is active
        if (!user.getIsActive()) {
            throw new NotFoundException("User not active, please contact customer support");
        }

        // Step 3: Verify password
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new BadRequestException("Wrong password");
        }

        // Step 4: Generate a JWT token
        String token = jwtUtils.generateToken(user.getEmail());

        // Step 5: Extract role names
        List<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .toList();

        // Build login response DTO
        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setToken(token);
        loginResponse.setRoles(roleNames);

        return Response.<LoginResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Login successful")
                .data(loginResponse)
                .build();
    }
}

