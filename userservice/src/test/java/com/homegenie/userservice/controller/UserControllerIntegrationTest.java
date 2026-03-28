package com.homegenie.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homegenie.userservice.dto.CreateUserRequest;
import com.homegenie.userservice.dto.UpdateUserRequest;
import com.homegenie.userservice.model.User;
import com.homegenie.userservice.model.UserRole;
import com.homegenie.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@SuppressWarnings("null")
public class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private User adminUser;
    private User regularUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        adminUser = new User();
        adminUser.setFullName("Admin User");
        adminUser.setEmail("admin@example.com");
        adminUser.setPhoneNumber("1234567890");
        adminUser.setRole(UserRole.ADMIN);
        userRepository.save(adminUser);

        regularUser = new User();
        regularUser.setFullName("Regular User");
        regularUser.setEmail("user@example.com");
        regularUser.setPhoneNumber("0987654321");
        regularUser.setRole(UserRole.RESIDENT);
        userRepository.save(regularUser);
    }

    @Test
    void createUser_NoAuth_Returns201() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("new@example.com");
        request.setFullName("New User");
        request.setRole("RESIDENT");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new@example.com"));
    }

    @Test
    void createUser_DuplicateEmail_Returns500() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("admin@example.com"); // already exists
        request.setFullName("Duplicate");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void getUserById_Authenticated_Success() throws Exception {
        mockMvc.perform(get("/api/users/{id}", regularUser.getId())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RESIDENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    @Test
    void getUserById_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/users/{id}", regularUser.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllUsers_AdminAccess_Success() throws Exception {
        mockMvc.perform(get("/api/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getAllUsers_ResidentAccess_Forbidden() throws Exception {
        mockMvc.perform(get("/api/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RESIDENT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUser_Authenticated_Success() throws Exception {
        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setFullName("Updated Name");

        mockMvc.perform(put("/api/users/{id}", regularUser.getId())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RESIDENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Name"));
    }

    @Test
    void getAllTechnicians_Authenticated_ReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/users/technicians")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RESIDENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
