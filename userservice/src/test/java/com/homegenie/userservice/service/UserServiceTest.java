package com.homegenie.userservice.service;

import com.homegenie.userservice.service.impl.UserServiceImpl;
import com.homegenie.userservice.dto.CreateUserRequest;
import com.homegenie.userservice.dto.UpdateUserRequest;
import com.homegenie.userservice.dto.UserResponse;
import com.homegenie.userservice.model.User;
import com.homegenie.userservice.model.UserRole;
import com.homegenie.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserEventPublisher eventPublisher;

    @InjectMocks
    private UserServiceImpl userService;

    private CreateUserRequest createRequest;
    private User user;

    @BeforeEach
    void setUp() {
        createRequest = new CreateUserRequest();
        createRequest.setEmail("test@example.com");
        createRequest.setFullName("Test User");
        createRequest.setPhoneNumber("1234567890");
        createRequest.setFlatNumber("A-101");
        createRequest.setRole("RESIDENT");

        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setFullName("Test User");
        user.setPhoneNumber("1234567890");
        user.setFlatNumber("A-101");
        user.setRole(UserRole.RESIDENT);
        user.setActive(true);
    }

    @Test
    void testCreateUser_Success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponse response = userService.createUser(createRequest);

        assertNotNull(response);
        assertEquals("test@example.com", response.getEmail());
        assertEquals("Test User", response.getFullName());
        assertEquals("RESIDENT", response.getRole());
        assertEquals(1L, response.getId());

        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository).save(any(User.class));
        verify(eventPublisher).publishUserRegisteredEvent(any());
    }

    @Test
    void testCreateUser_DuplicateEmail_ThrowsException() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                userService.createUser(createRequest));

        assertTrue(exception.getMessage().startsWith("Email already registered"));
        verify(userRepository, never()).save(any(User.class));
        verify(eventPublisher, never()).publishUserRegisteredEvent(any());
    }

    @Test
    void testCreateUser_TechnicianRole_SetsSpecialty() {
        createRequest.setRole("TECHNICIAN");
        createRequest.setSpecialty("Plumbing");

        User techUser = new User();
        techUser.setId(2L);
        techUser.setEmail("tech@example.com");
        techUser.setRole(UserRole.TECHNICIAN);
        techUser.setSpecialty("Plumbing");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(techUser);

        UserResponse response = userService.createUser(createRequest);

        assertEquals("TECHNICIAN", response.getRole());
        assertEquals("Plumbing", response.getSpecialty());
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
        verify(userRepository).findById(1L);
    }

    @Test
    void testGetUserById_NotFound_ThrowsException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                userService.getUserById(999L));

        assertTrue(exception.getMessage().startsWith("User not found"));
        verify(userRepository).findById(999L);
    }

    @Test
    void testUpdateUser_Success() {
        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setFullName("Updated Name");
        updateRequest.setPhoneNumber("0987654321");

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponse response = userService.updateUser(1L, updateRequest);

        assertNotNull(response);
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
        verify(eventPublisher).publishUserUpdatedEvent(any());
    }

    @Test
    void testUpdateUser_NotFound_ThrowsException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                userService.updateUser(999L, new UpdateUserRequest()));

        assertTrue(exception.getMessage().startsWith("User not found"));
    }
}
