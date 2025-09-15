package com.app.FoodApp.authUsers.services;

import com.app.FoodApp.authUsers.dtos.UserDTO;
import com.app.FoodApp.authUsers.entities.User;
import com.app.FoodApp.authUsers.repositories.UserRepository;
import com.app.FoodApp.aws.AwsS3Service;
import com.app.FoodApp.emailNofitication.dtos.NotificationDTO;
import com.app.FoodApp.emailNofitication.services.NotificationService;
import com.app.FoodApp.exceptions.BadRequestException;
import com.app.FoodApp.exceptions.NotFoundException;
import com.app.FoodApp.response.Response;
import com.app.FoodApp.role.entities.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.fasterxml.classmate.AnnotationOverrides.builder;

@Service
@RequiredArgsConstructor
@Slf4j
/**
 * Implementation of {@link UserService} that handles:
 * - Retrieving user information
 * - Updating account details (including profile images in S3)
 * - Deactivating accounts with notifications
 */
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final NotificationService notificationService;
    private final AwsS3Service awsS3Service;

    /**
     * Retrieves the currently authenticated user from the security context.
     *
     * @return User entity of the logged-in account
     * @throws NotFoundException if no user is found for the logged-in email
     */
    @Override
    public User getCurrentLoggedInUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    /**
     * Retrieves all users sorted by ID in descending order.
     *
     * @return Response containing a list of UserDTO objects
     */
    @Override
    public Response<List<UserDTO>> getAllUsers() {
        // Fetch all users, latest first
        List<User> users = userRepository.findAll();

        // Convert User entities → DTOs
        List<UserDTO> userDTOS = modelMapper.map(users, new TypeToken<List<UserDTO>>() {}.getType());

        return Response.<List<UserDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("All users retrieved")
                .data(userDTOS)
                .build();
    }

    /**
     * Retrieves details of the currently logged-in user.
     *
     * @return Response with UserDTO containing account details
     */
    @Override
    public Response<UserDTO> getOwnAccountDetails() {
        User user = getCurrentLoggedInUser();
        UserDTO userDTO = modelMapper.map(user, UserDTO.class);

        return Response.<UserDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Success")
                .data(userDTO)
                .build();
    }

    /**
     * Updates the currently logged-in user's account details.
     * Steps:
     * 1. Retrieve logged-in user
     * 2. Handle profile image replacement in AWS S3
     * 3. Update basic user fields (name, phone, address, etc.)
     * 4. Encode and update password if provided
     * 5. Update email if changed (with uniqueness check)
     * 6. Persist changes in database
     *
     * @param userDTO DTO containing updated user information
     * @return Response indicating update success
     * @throws BadRequestException if new email already exists
     */
    @Override
    public Response<?> updateOwnAccount(UserDTO userDTO) {
        User user = getCurrentLoggedInUser();
        String profileUrl = user.getProfileUrl();
        MultipartFile imageFile = userDTO.getImageFile();

        // Step 2: Check if a new profile image is uploaded
        if (imageFile != null && !imageFile.isEmpty()) {
            // Delete old image in S3 if it exists
            if (profileUrl != null && !profileUrl.isEmpty()) {
                String keyName = profileUrl.substring(profileUrl.lastIndexOf("/") + 1);
                awsS3Service.deleteFile("profile/" + keyName);
            }

            // Upload new image to S3 with a unique name
            String originalName = imageFile.getOriginalFilename();
            String safeName = originalName != null ? originalName.replaceAll("\\s+", "_") : "image";
            String imageName = UUID.randomUUID() + "_" + safeName;
            
            URL newImageUrl = awsS3Service.uploadFile("profile/" + imageName, imageFile);
            user.setProfileUrl(newImageUrl.toString());
        }

        // Step 3: Update non-null fields
        if (userDTO.getName() != null) user.setName(userDTO.getName());
        if (userDTO.getPhoneNumber() != null) user.setPhoneNumber(userDTO.getPhoneNumber());
        if (userDTO.getAddress() != null) user.setAddress(userDTO.getAddress());

        // Step 4: Update password if provided
        if (userDTO.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }

        // Step 5: Update email if changed and unique
        if (userDTO.getEmail() != null && !userDTO.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(userDTO.getEmail())) {
                throw new BadRequestException("Email already exists");
            }
            user.setEmail(userDTO.getEmail());
        }

        user.setUpdatedAt(LocalDateTime.now());

        // Step 6: Persist updated user entity
        userRepository.save(user);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Account updated successfully")
                .build();
    }

    /**
     * Deactivates the currently logged-in user's account.
     * Steps:
     * 1. Mark account as inactive
     * 2. Save changes
     * 3. Send deactivation notification
     *
     * @return Response indicating deactivation success
     */
    @Override
    public Response<?> deactivateOwnAccount() {
        User user = getCurrentLoggedInUser();
        user.setIsActive(false);
        userRepository.save(user);

        // Step 3: Send deactivation email notification
        NotificationDTO notificationDTO = NotificationDTO.builder()
                .recipient(user.getEmail())
                .subject("Account Deactivated")
                .body("Your account has been deactivated. If this was a mistake, please contact support.")
                .build();

        notificationService.sendEmail(notificationDTO);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Account deactivated successfully")
                .build();
    }

    /**
     * Updates any user’s account (admin only).
     *
     * @param userId ID of the user to update
     * @param userDTO DTO with updated details
     * @return Response indicating update success
     */
    @Override
    public Response<?> updateUserAsAdmin(Long userId, UserDTO userDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        String profileUrl = user.getProfileUrl();
        MultipartFile imageFile = userDTO.getImageFile();

        // Handle profile image replacement
        if (imageFile != null && !imageFile.isEmpty()) {
            if (profileUrl != null && !profileUrl.isEmpty()) {
                String keyName = profileUrl.substring(profileUrl.lastIndexOf("/") + 1);
                awsS3Service.deleteFile("profile/" + keyName);
            }

            String originalName = imageFile.getOriginalFilename();
            String safeName = originalName != null ? originalName.replaceAll("\\s+", "_") : "image";
            String imageName = UUID.randomUUID() + "_" + safeName;

            URL newImageUrl = awsS3Service.uploadFile("profile/" + imageName, imageFile);
            user.setProfileUrl(newImageUrl.toString());
        }

        // Update basic fields
        if (userDTO.getName() != null) user.setName(userDTO.getName());
        if (userDTO.getPhoneNumber() != null) user.setPhoneNumber(userDTO.getPhoneNumber());
        if (userDTO.getAddress() != null) user.setAddress(userDTO.getAddress());
        if (userDTO.getIsActive() != null) user.setIsActive(userDTO.getIsActive()); // admin can toggle active

        // Update password if provided
        if (userDTO.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }

        // Update email if changed
        if (userDTO.getEmail() != null && !userDTO.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(userDTO.getEmail())) {
                throw new BadRequestException("Email already exists");
            }
            user.setEmail(userDTO.getEmail());
        }

        // Update roles (admin can change roles)
        if (userDTO.getRoles() != null) {
            List<Role> roles = userDTO.getRoles().stream()
                    .map(roleDTO -> modelMapper.map(roleDTO, Role.class))
                    .toList();
            user.setRoles(roles);
        }

        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("User updated successfully by admin")
                .build();
    }

    @Override
    public Response<UserDTO> getUserById(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

        UserDTO userDTO = modelMapper.map(user, UserDTO.class);

        return Response.<UserDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Success")
                .data(userDTO)
                .build();
    }

}
