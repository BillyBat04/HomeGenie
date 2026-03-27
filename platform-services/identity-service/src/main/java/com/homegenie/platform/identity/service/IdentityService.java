package com.homegenie.platform.identity.service;

import com.homegenie.platform.identity.dto.*;
import com.homegenie.platform.identity.exception.*;
import com.homegenie.platform.identity.model.RefreshToken;
import com.homegenie.platform.identity.model.User;
import com.homegenie.platform.identity.model.UserRole;
import com.homegenie.platform.identity.repository.UserRepository;
import com.homegenie.platform.identity.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Identity Service - Core authentication and user management
 * ✅ IDENTICAL logic to User Service
 * 
 * Responsibilities:
 * - User registration
 * - User authentication
 * - User information retrieval
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class IdentityService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenService tokenService;

    /**
     * Register new user
     * ✅ Compatible with User Service register logic
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: email={}, role={}", request.getEmail(), request.getRole());
        
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        // Create new user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setFlatNumber(request.getFlatNumber());

        // Set role
        if (request.getRole() != null && !request.getRole().isEmpty()) {
            try {
                user.setRole(UserRole.valueOf(request.getRole().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid role provided: {}, defaulting to RESIDENT", request.getRole());
                user.setRole(UserRole.RESIDENT);
            }
        }

        // Set specialty for technicians
        if (user.getRole() == UserRole.TECHNICIAN && request.getTechnicianSpecialty() != null) {
            user.setSpecialty(request.getTechnicianSpecialty());
        }

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: userId={}, email={}", savedUser.getId(), savedUser.getEmail());

        // Generate JWT token
        String token = jwtUtil.generateToken(
                savedUser.getEmail(),
                savedUser.getId(),
                savedUser.getRole().name()
        );

        // Generate refresh token
        RefreshToken refreshToken = tokenService.createRefreshToken(savedUser.getId());

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .role(savedUser.getRole().name())
                .expiresIn(jwtUtil.getAccessTokenExpiration())
                .build();
    }

    /**
     * Authenticate user (login)
     * ✅ Compatible with User Service login logic
     */
    public AuthResponse authenticate(LoginRequest request) {
        log.info("Authenticating user: email={}", request.getEmail());
        
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException());

        // Check password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Invalid password attempt for user: email={}", request.getEmail());
            throw new InvalidCredentialsException();
        }

        // Check if account is active
        if (!user.isActive()) {
            log.warn("Inactive account login attempt: email={}", request.getEmail());
            throw new AccountInactiveException();
        }

        log.info("User authenticated successfully: userId={}, email={}", user.getId(), user.getEmail());

        // Generate JWT token
        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getId(),
                user.getRole().name()
        );

        // Generate refresh token
        RefreshToken refreshToken = tokenService.createRefreshToken(user.getId());

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .expiresIn(jwtUtil.getAccessTokenExpiration())
                .build();
    }

    /**
     * Get user by ID
     * ✅ Compatible with User Service getUserById logic
     */
    public UserResponse getUserById(Long userId) {
        log.info("Fetching user by ID: userId={}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .flatNumber(user.getFlatNumber())
                .role(user.getRole().name())
                .technicianSpecialty(user.getSpecialty())
                .active(user.isActive())
                .build();
    }

    /**
     * Get user by email
     * For internal use (e.g., token validation)
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
    }
}
