package com.homegenie.userservice.dto;

import lombok.Data;

@Data
public class UserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String flatNumber;
    private String role;
    private String specialty;
    private boolean active;
    private boolean emailNotificationsEnabled;
}
