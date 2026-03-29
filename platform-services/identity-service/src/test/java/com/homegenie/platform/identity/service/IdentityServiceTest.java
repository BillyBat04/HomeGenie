package com.homegenie.platform.identity.service;

import com.homegenie.platform.identity.service.impl.IdentityServiceImpl;
import com.homegenie.platform.identity.dto.AuthResponse;
import com.homegenie.platform.identity.dto.LoginRequest;
import com.homegenie.platform.identity.dto.RegisterRequest;
import com.homegenie.platform.identity.dto.UserResponse;
import com.homegenie.platform.identity.exception.AccountInactiveException;
import com.homegenie.platform.identity.exception.EmailAlreadyExistsException;
import com.homegenie.platform.identity.exception.InvalidCredentialsException;
import com.homegenie.platform.identity.exception.UserNotFoundException;
import com.homegenie.platform.identity.model.RefreshToken;
import com.homegenie.platform.identity.model.User;
import com.homegenie.platform.identity.model.UserRole;
import com.homegenie.platform.identity.repository.UserRepository;
import com.homegenie.platform.identity.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class IdentityServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private TokenService tokenService;

    @InjectMocks
    private IdentityServiceImpl identityService;



    private User activeUser() {
        User u = new User();
        u.setId(1L);
        u.setEmail("john@example.com");
        u.setPassword("$2a$10$hashed");
        u.setFullName("John Doe");
        u.setPhoneNumber("0901234567");
        u.setRole(UserRole.RESIDENT);
        return u;
    }

    private RefreshToken refreshToken() {
        return RefreshToken.builder()
                .id(1L)
                .token("refresh-uuid-token")
                .userId(1L)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
    }

    
    @Test
    void register_success_returnsAuthResponse() {
        RegisterRequest req = new RegisterRequest(
                "john@example.com", "P@ssw0rd!", "John Doe",
                "0901234567", "A101", "RESIDENT", null);

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("P@ssw0rd!")).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(jwtUtil.generateToken(anyString(), anyLong(), anyString())).thenReturn("jwt-token");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(900000L);
        when(tokenService.createRefreshToken(1L)).thenReturn(refreshToken());

        AuthResponse res = identityService.register(req);

        assertNotNull(res);
        assertEquals("jwt-token", res.getToken());
        assertEquals("refresh-uuid-token", res.getRefreshToken());
        assertEquals("Bearer", res.getTokenType());
        assertEquals("john@example.com", res.getEmail());
        assertEquals("RESIDENT", res.getRole());

        verify(passwordEncoder).encode("P@ssw0rd!"); 
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_emailAlreadyExists_throwsEmailAlreadyExistsException() {
        RegisterRequest req = new RegisterRequest(
                "john@example.com", "P@ssw0rd!", "John Doe",
                "0901234567", null, "RESIDENT", null);

        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> identityService.register(req));

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void register_invalidRole_defaultsToResident() {
        RegisterRequest req = new RegisterRequest(
                "jane@example.com", "P@ssw0rd!", "Jane",
                "0901234567", null, "UNKNOWN", null);

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        when(jwtUtil.generateToken(anyString(), anyLong(), anyString())).thenReturn("jwt");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(900000L);
        when(tokenService.createRefreshToken(2L)).thenReturn(
                RefreshToken.builder().token("rt").userId(2L).expiresAt(LocalDateTime.now().plusDays(7)).revoked(false).build());

        AuthResponse res = identityService.register(req);

        assertEquals("RESIDENT", res.getRole());
    }

    @Test
    void register_technicianWithSpecialty_storesSpecialty() {
        RegisterRequest req = new RegisterRequest(
                "tech@example.com", "P@ssw0rd!", "Tech Nick",
                "0901234567", null, "TECHNICIAN", "Plumbing");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            saved.setId(3L);
            return saved;
        });
        when(jwtUtil.generateToken(anyString(), anyLong(), anyString())).thenReturn("jwt");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(900000L);
        when(tokenService.createRefreshToken(3L)).thenReturn(
                RefreshToken.builder().token("rt").userId(3L).expiresAt(LocalDateTime.now().plusDays(7)).revoked(false).build());

        identityService.register(req);

        
        verify(userRepository).save(argThat(u ->
                u.getRole() == UserRole.TECHNICIAN && "Plumbing".equals(u.getSpecialty())
        ));
    }

    
    @Test
    void authenticate_success_returnsAuthResponse() {
        User user = activeUser();
        LoginRequest req = new LoginRequest("john@example.com", "P@ssw0rd!");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("P@ssw0rd!", user.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(anyString(), anyLong(), anyString())).thenReturn("jwt-token");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(900000L);
        when(tokenService.createRefreshToken(1L)).thenReturn(refreshToken());

        AuthResponse res = identityService.authenticate(req);

        assertNotNull(res);
        assertEquals("jwt-token", res.getToken());
        assertEquals("john@example.com", res.getEmail());
        assertEquals("RESIDENT", res.getRole());
    }

    @Test
    void authenticate_userNotFound_throwsInvalidCredentialsException() {

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> identityService.authenticate(new LoginRequest("ghost@example.com", "pwd")));
    }

    @Test
    void authenticate_wrongPassword_throwsInvalidCredentialsException() {
        User user = activeUser();
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpwd", user.getPassword())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> identityService.authenticate(new LoginRequest("john@example.com", "wrongpwd")));
    }

    @Test
    void authenticate_inactiveAccount_throwsAccountInactiveException() {
        User user = activeUser();
        user.setActive(false);

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThrows(AccountInactiveException.class,
                () -> identityService.authenticate(new LoginRequest("john@example.com", "P@ssw0rd!")));
    }


    

    @Test
    void getUserById_found_returnsUserResponse() {
        User user = activeUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse res = identityService.getUserById(1L);

        assertEquals(1L, res.getId());
        assertEquals("john@example.com", res.getEmail());
        assertEquals("John Doe", res.getFullName());
        assertEquals("RESIDENT", res.getRole());
        assertTrue(res.getActive());
    }

    @Test
    void getUserById_notFound_throwsUserNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> identityService.getUserById(99L));
    }

    

    @Test
    void getUserByEmail_found_returnsUser() {
        User user = activeUser();
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        User result = identityService.getUserByEmail("john@example.com");

        assertEquals(user.getId(), result.getId());
    }

    @Test
    void getUserByEmail_notFound_throwsUserNotFoundException() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> identityService.getUserByEmail("ghost@example.com"));
    }
}
