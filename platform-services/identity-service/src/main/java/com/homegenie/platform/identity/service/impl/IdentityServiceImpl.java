package com.homegenie.platform.identity.service.impl;


import com.homegenie.platform.identity.service.IdentityService;
import com.homegenie.platform.identity.service.*;
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


@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class IdentityServiceImpl implements IdentityService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenService tokenService;


    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: email={}, role={}", request.getEmail(), request.getRole());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
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
                log.warn("Invalid role provided: {}, defaulting to RESIDENT", request.getRole());
                user.setRole(UserRole.RESIDENT);
            }
        }

        if (user.getRole() == UserRole.TECHNICIAN && request.getTechnicianSpecialty() != null) {
            user.setSpecialty(request.getTechnicianSpecialty());
        }

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: userId={}, email={}", savedUser.getId(), savedUser.getEmail());

        String token = jwtUtil.generateToken(
                savedUser.getEmail(),
                savedUser.getId(),
                savedUser.getRole().name()
        );

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


    public AuthResponse authenticate(LoginRequest request) {
        log.info("Authenticating user: email={}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException());

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Invalid password attempt for user: email={}", request.getEmail());
            throw new InvalidCredentialsException();
        }

        if (!user.isActive()) {
            log.warn("Inactive account login attempt: email={}", request.getEmail());
            throw new AccountInactiveException();
        }

        log.info("User authenticated successfully: userId={}, email={}", user.getId(), user.getEmail());

        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getId(),
                user.getRole().name()
        );

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


    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
    }
}
