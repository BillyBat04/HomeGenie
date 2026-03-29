package com.homegenie.userservice.service;

import com.homegenie.userservice.dto.CreateUserRequest;
import com.homegenie.userservice.dto.UpdateUserRequest;
import com.homegenie.userservice.dto.UserResponse;

import java.util.List;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    UserResponse getUserById(Long id);

    List<UserResponse> getAllTechnicians();

    List<UserResponse> getAllUsers();

    UserResponse updateUser(Long id, UpdateUserRequest request);
}
