package com.homegenie.userservice.service;

import com.homegenie.userservice.dto.AuthResponse;
import com.homegenie.userservice.dto.LoginRequest;
import com.homegenie.userservice.dto.RegisterRequest;
import com.homegenie.userservice.dto.UserResponse;
import com.homegenie.userservice.model.User;
import com.homegenie.userservice.model.UserRole;
import com.homegenie.userservice.repository.UserRepository;
import com.homegenie.userservice.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for UserService
 *
 * Test Coverage:
 * - User Registration (success, duplicate email)
 * - User Login (success, invalid credentials, inactive account)
 * - Get User by ID (success, not found)
 * - Role assignment
 * - Password encryption
 * - JWT token generation
 * - Event publishing
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserEventPublisher eventPublisher;

    @InjectMocks
    private UserService userService;

    private RegisterRequest registerRequest;
    private User user;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFullName("Test User");
        registerRequest.setPhoneNumber("1234567890");
        registerRequest.setFlatNumber("A-101");
        registerRequest.setRole("RESIDENT");

        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setFullName("Test User");
        user.setPhoneNumber("1234567890");
        user.setFlatNumber("A-101");
        user.setRole(UserRole.RESIDENT);
        user.setActive(true);

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");
    }

    @Test
    void testRegister_Success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtUtil.generateToken(anyString(), anyLong(), anyString())).thenReturn("jwt-token");

        // When
        AuthResponse response = userService.register(registerRequest);

        // Then
        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("Test User", response.getFullName());
        assertEquals("RESIDENT", response.getRole());
        assertEquals(1L, response.getUserId());

        verify(userRepository, times(1)).existsByEmail("test@example.com");
        verify(passwordEncoder, times(1)).encode("password123");
        verify(userRepository, times(1)).save(any(User.class));
        verify(jwtUtil, times(1)).generateToken(anyString(), anyLong(), anyString());
        verify(eventPublisher, times(1)).publishUserRegisteredEvent(any());
    }

    @Test
    void testRegister_DuplicateEmail_ThrowsException() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.register(registerRequest);
        });

        assertEquals("Email already registered", exception.getMessage());
        verify(userRepository, times(1)).existsByEmail("test@example.com");
        verify(userRepository, never()).save(any(User.class));
        verify(eventPublisher, never()).publishUserRegisteredEvent(any());
    }

    @Test
    void testRegister_TechnicianRole_SetsSpecialty() {
        registerRequest.setRole("TECHNICIAN");
        registerRequest.setSpecialty("Plumbing");

        User technicianUser = new User();
        technicianUser.setId(2L);
        technicianUser.setEmail("tech@example.com");
        technicianUser.setRole(UserRole.TECHNICIAN);
        technicianUser.setSpecialty("Plumbing");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(technicianUser);
        when(jwtUtil.generateToken(anyString(), anyLong(), anyString())).thenReturn("jwt-token");

        // When
        AuthResponse response = userService.register(registerRequest);

        // Then
        assertNotNull(response);
        assertEquals("TECHNICIAN", response.getRole());
        assertEquals("Plumbing", response.getSpecialty());
    }

    @Test
    void testLogin_Success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtil.generateToken(anyString(), anyLong(), anyString())).thenReturn("jwt-token");

        AuthResponse response = userService.login(loginRequest);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("Test User", response.getFullName());

        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(passwordEncoder, times(1)).matches("password123", "encodedPassword");
        verify(jwtUtil, times(1)).generateToken(anyString(), anyLong(), anyString());
    }

    @Test
    void testLogin_InvalidEmail_ThrowsException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.login(loginRequest);
        });

        assertEquals("Invalid credentials", exception.getMessage());
        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void testLogin_InvalidPassword_ThrowsException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.login(loginRequest);
        });

        assertEquals("Invalid credentials", exception.getMessage());
        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(passwordEncoder, times(1)).matches("password123", "encodedPassword");
        verify(jwtUtil, never()).generateToken(anyString(), anyLong(), anyString());
    }

    @Test
    void testLogin_InactiveAccount_ThrowsException() {
        user.setActive(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.login(loginRequest);
        });

        assertEquals("Account is deactivated", exception.getMessage());
        verify(jwtUtil, never()).generateToken(anyString(), anyLong(), anyString());
    }

    @Test
    void testGetUserById_Success() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));

        UserResponse response = userService.getUserById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("Test User", response.getFullName());
        assertEquals("1234567890", response.getPhoneNumber());
        assertEquals("A-101", response.getFlatNumber());
        assertEquals("RESIDENT", response.getRole());
        assertTrue(response.isActive());

        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void testGetUserById_NotFound_ThrowsException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.getUserById(999L);
        });

        assertEquals("User not found", exception.getMessage());
        verify(userRepository, times(1)).findById(999L);
    }
}

