package com.homegenie.platform.identity.service;

import com.homegenie.platform.identity.dto.AuthResponse;
import com.homegenie.platform.identity.dto.LoginRequest;
import com.homegenie.platform.identity.dto.RegisterRequest;
import com.homegenie.platform.identity.dto.UserResponse;
import com.homegenie.platform.identity.model.User;

public interface IdentityService {

    AuthResponse register(RegisterRequest request);

    AuthResponse authenticate(LoginRequest request);

    UserResponse getUserById(Long userId);

    User getUserByEmail(String email);
}
