package com.app.FoodApp.authUsers.controllers;

import com.app.FoodApp.authUsers.dtos.UserDTO;
import com.app.FoodApp.authUsers.services.UserService;
import com.app.FoodApp.response.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/admin/users")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminUserController {
    private final UserService userService;

    /**
     * Retrieves all registered users.
     */
    @GetMapping
    public ResponseEntity<Response<List<UserDTO>>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Response<UserDTO>> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    /**
     * Updates a user’s account details as an ADMIN.
     * Accepts multipart form data for optional profile image upload.
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Response<?>> updateUser(
            @PathVariable Long id,
            @ModelAttribute UserDTO userDTO,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile
    ) {
        userDTO.setImageFile(imageFile);
        return ResponseEntity.ok(userService.updateUserAsAdmin(id, userDTO));
    }


}

