package com.app.FoodApp.authUsers.services;

import com.app.FoodApp.authUsers.dtos.UserDTO;
import com.app.FoodApp.authUsers.entities.User;
import com.app.FoodApp.response.Response;

import java.util.List;

public interface UserService {
    User getCurrentLoggedInUser();
    Response<UserDTO> getOwnAccountDetails();
    Response<?> updateOwnAccount(UserDTO userDTO);
    Response<?> deactivateOwnAccount();

    // For admins
    Response<?> updateUserAsAdmin(Long userId, UserDTO userDTO);
    Response<UserDTO> getUserById(Long userId);
    Response<List<UserDTO>> getAllUsers();
}
