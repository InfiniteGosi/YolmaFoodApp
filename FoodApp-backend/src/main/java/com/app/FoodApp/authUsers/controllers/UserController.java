package com.app.FoodApp.authUsers.controllers;

import com.app.FoodApp.authUsers.dtos.UserDTO;
import com.app.FoodApp.authUsers.services.UserService;
import com.app.FoodApp.response.Response;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST controller for managing user-related operations.
 * Endpoints include:
 * - Retrieving all users (admin only)
 * - Updating own account details
 * - Viewing own account details
 * - Deactivating own account
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("api/users")
public class UserController {
    private final UserService userService;
    /**
     * Updates the currently authenticated user's account details.
     * Accepts multipart form data for optional profile image upload.
     *
     * @param userDTO   DTO containing updated user details (validated)
     * @param imageFile Optional profile image file
     * @return ResponseEntity with a Response indicating success or failure
     */
    @PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Response<?>> updateOwnAccount(
            @ModelAttribute UserDTO userDTO,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile
    ) {
        // Attach uploaded file to DTO before passing to service
        userDTO.setImageFile(imageFile);
        return ResponseEntity.ok(userService.updateOwnAccount(userDTO));
    }

    /**
     * Retrieves account details of the currently authenticated user.
     *
     * @return ResponseEntity with a Response containing the UserDTO
     */
    @GetMapping("/account")
    public ResponseEntity<Response<UserDTO>> getAccountDetails() {
        return ResponseEntity.ok(userService.getOwnAccountDetails());
    }

    /**
     * Deactivates the currently authenticated user's account.
     *
     * @return ResponseEntity with a Response indicating deactivation success
     */
    @DeleteMapping("/deactivate")
    public ResponseEntity<Response<?>> deactivateOwnAccount() {
        return ResponseEntity.ok(userService.deactivateOwnAccount());
    }
}

