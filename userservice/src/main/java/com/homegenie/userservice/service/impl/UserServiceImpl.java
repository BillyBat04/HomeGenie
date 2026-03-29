package com.homegenie.userservice.service.impl;


import com.homegenie.userservice.service.UserService;
import com.homegenie.userservice.service.UserEventPublisher;
import com.homegenie.userservice.exception.EmailAlreadyExistsException;
import com.homegenie.userservice.exception.UserNotFoundException;
import com.homegenie.userservice.dto.*;
import com.homegenie.userservice.dto.event.UserRegisteredEvent;
import com.homegenie.userservice.dto.event.UserUpdatedEvent;
import com.homegenie.userservice.model.User;
import com.homegenie.userservice.model.UserRole;
import com.homegenie.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings({"null", "unused"})
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserEventPublisher eventPublisher;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        User user = new User();
        user.setEmail(request.getEmail());
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
        return mapToUserResponse(savedUser);
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

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

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getFlatNumber() != null) {
            user.setFlatNumber(request.getFlatNumber());
        }
        if (user.getRole() == UserRole.TECHNICIAN && request.getSpecialty() != null) {
            user.setSpecialty(request.getSpecialty());
        }
        if (request.getEmailNotificationsEnabled() != null) {
            user.setEmailNotificationsEnabled(request.getEmailNotificationsEnabled());
        }

        User savedUser = userRepository.save(user);
        publishUserUpdatedEvent(savedUser, "PROFILE_UPDATE");
        return mapToUserResponse(savedUser);
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