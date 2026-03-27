package com.homegenie.userservice.service;

import com.homegenie.userservice.dto.*;
import com.homegenie.userservice.dto.event.UserRegisteredEvent;
import com.homegenie.userservice.dto.event.UserUpdatedEvent;
import com.homegenie.userservice.model.User;
import com.homegenie.userservice.model.UserRole;
import com.homegenie.userservice.model.RefreshToken;
import com.homegenie.userservice.repository.UserRepository;
import com.homegenie.userservice.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings({"null", "unused"})
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserEventPublisher eventPublisher;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setFlatNumber(request.getFlatNumber());

        if (request.getRole() != null && !request.getRole().isEmpty()) {
            try {
                user.setRole(UserRole.valueOf(request.getRole().toUpperCase()));
            } catch (IllegalArgumentException e) {
                user.setRole(UserRole.RESIDENT);
            }
        }

        if (user.getRole() == UserRole.TECHNICIAN && request.getSpecialty() != null) {
            user.setSpecialty(request.getSpecialty());
        }

        User savedUser = userRepository.save(user);

        publishUserRegisteredEvent(savedUser);

        String token = jwtUtil.generateToken(
                savedUser.getEmail(),
                savedUser.getId(),
                savedUser.getRole().name()
        );

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(savedUser.getId());

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setRefreshToken(refreshToken.getToken());
        response.setEmail(savedUser.getEmail());
        response.setFullName(savedUser.getFullName());
        response.setRole(savedUser.getRole().name());
        response.setUserId(savedUser.getId());
        response.setSpecialty(savedUser.getSpecialty());

        return response;
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (!user.isActive()) {
            throw new RuntimeException("Account is deactivated");
        }

        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getId(),
                user.getRole().name()
        );

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setRefreshToken(refreshToken.getToken());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRole(user.getRole().name());
        response.setUserId(user.getId());
        response.setSpecialty(user.getSpecialty());

        return response;
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenString) {
        RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenString)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        refreshTokenService.verifyExpiration(refreshToken);

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newAccessToken = jwtUtil.generateToken(
                user.getEmail(),
                user.getId(),
                user.getRole().name()
        );

        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getId());
        refreshTokenService.revokeToken(refreshTokenString);

        AuthResponse response = new AuthResponse();
        response.setToken(newAccessToken);
        response.setRefreshToken(newRefreshToken.getToken());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRole(user.getRole().name());
        response.setUserId(user.getId());
        response.setSpecialty(user.getSpecialty());

        return response;
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return mapToUserResponse(user);
    }

    public List<UserResponse> getAllTechnicians() {
        return userRepository.findByRoleAndActive(UserRole.TECHNICIAN, true)
                .stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setFlatNumber(user.getFlatNumber());
        response.setRole(user.getRole().name());
        response.setSpecialty(user.getSpecialty());
        response.setActive(user.isActive());
        response.setEmailNotificationsEnabled(user.isEmailNotificationsEnabled());
        return response;
    }

    private void publishUserRegisteredEvent(User user) {
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().name())
                .specialty(user.getSpecialty())
                .eventType("USER_REGISTERED")
                .timestamp(Instant.now())
                .build();

        eventPublisher.publishUserRegisteredEvent(event);
    }

    private void publishUserUpdatedEvent(User user, String updateType) {
        UserUpdatedEvent event = UserUpdatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().name())
                .specialty(user.getSpecialty())
                .updateType(updateType)
                .eventType("USER_UPDATED")
                .timestamp(Instant.now())
                .build();

        eventPublisher.publishUserUpdatedEvent(event);
    }
}