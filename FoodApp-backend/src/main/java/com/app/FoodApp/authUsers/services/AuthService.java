package com.app.FoodApp.authUsers.services;

import com.app.FoodApp.authUsers.dtos.LoginRequest;
import com.app.FoodApp.authUsers.dtos.LoginResponse;
import com.app.FoodApp.authUsers.dtos.RegistrationRequest;
import com.app.FoodApp.response.Response;

public interface AuthService {
    Response<?> register(RegistrationRequest registrationRequest);
    Response<LoginResponse> login(LoginRequest loginRequest);
}
