package com.homegenie.platform.identity.service;

import com.homegenie.platform.identity.dto.TokenRefreshResponse;
import com.homegenie.platform.identity.exception.InvalidRefreshTokenException;
import com.homegenie.platform.identity.model.RefreshToken;
import com.homegenie.platform.identity.model.User;
import com.homegenie.platform.identity.model.UserRole;
import com.homegenie.platform.identity.repository.RefreshTokenRepository;
import com.homegenie.platform.identity.repository.UserRepository;
import com.homegenie.platform.identity.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class TokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks
    private TokenService tokenService;

    
    private static final long REFRESH_EXPIRATION_MS = 604_800_000L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tokenService, "refreshTokenExpiration", REFRESH_EXPIRATION_MS);
    }

    

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

    private RefreshToken validToken() {
        return RefreshToken.builder()
                .id(1L)
                .token("valid-refresh-token")
                .userId(1L)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
    }

    
    
    

    @Test
    void createRefreshToken_savesAndReturnsToken() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken result = tokenService.createRefreshToken(1L);

        assertNotNull(result.getToken());
        assertEquals(1L, result.getUserId());
        assertFalse(result.isRevoked());
        
        assertTrue(result.getExpiresAt().isAfter(LocalDateTime.now().plusDays(6)));

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void createRefreshToken_tokenIsUniqueUUID() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken t1 = tokenService.createRefreshToken(1L);
        RefreshToken t2 = tokenService.createRefreshToken(1L);

        
        assertNotEquals(t1.getToken(), t2.getToken());
    }

    
    
    

    @Test
    void refreshToken_success_rotatesTokenAndReturnsNewJwt() {
        RefreshToken oldToken = validToken();
        User user = activeUser();

        when(refreshTokenRepository.findByToken("valid-refresh-token")).thenReturn(Optional.of(oldToken));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(anyString(), anyLong(), anyString())).thenReturn("new-jwt");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(900_000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        TokenRefreshResponse res = tokenService.refreshToken("valid-refresh-token");

        
        assertEquals("new-jwt", res.getToken());
        assertEquals("Bearer", res.getTokenType());
        assertNotNull(res.getRefreshToken()); 

        
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, atLeast(2)).save(captor.capture());
        List<RefreshToken> saved = captor.getAllValues();
        
        assertTrue(saved.get(0).isRevoked());
        assertNotNull(saved.get(0).getRevokedAt());
    }

    @Test
    void refreshToken_tokenNotFound_throwsInvalidRefreshTokenException() {
        when(refreshTokenRepository.findByToken("ghost-token")).thenReturn(Optional.empty());

        assertThrows(InvalidRefreshTokenException.class,
                () -> tokenService.refreshToken("ghost-token"));
    }

    @Test
    void refreshToken_tokenAlreadyRevoked_throwsInvalidRefreshTokenException() {
        RefreshToken revoked = validToken();
        revoked.setRevoked(true);

        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(revoked));

        assertThrows(InvalidRefreshTokenException.class,
                () -> tokenService.refreshToken("revoked-token"));
    }

    @Test
    void refreshToken_tokenExpired_throwsInvalidRefreshTokenException() {
        RefreshToken expired = RefreshToken.builder()
                .id(2L)
                .token("expired-token")
                .userId(1L)
                .expiresAt(LocalDateTime.now().minusHours(1)) 
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expired));

        assertThrows(InvalidRefreshTokenException.class,
                () -> tokenService.refreshToken("expired-token"));
    }

    @Test
    void refreshToken_userNotFound_throwsInvalidRefreshTokenException() {
        when(refreshTokenRepository.findByToken("valid-refresh-token")).thenReturn(Optional.of(validToken()));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(InvalidRefreshTokenException.class,
                () -> tokenService.refreshToken("valid-refresh-token"));
    }

    @Test
    void refreshToken_inactiveUser_throwsInvalidRefreshTokenException() {
        User inactive = activeUser();
        inactive.setActive(false);

        when(refreshTokenRepository.findByToken("valid-refresh-token")).thenReturn(Optional.of(validToken()));
        when(userRepository.findById(1L)).thenReturn(Optional.of(inactive));

        assertThrows(InvalidRefreshTokenException.class,
                () -> tokenService.refreshToken("valid-refresh-token"));
    }

    
    
    

    @Test
    void revokeRefreshToken_marksTokenRevoked() {
        RefreshToken token = validToken();
        when(refreshTokenRepository.findByToken("valid-refresh-token")).thenReturn(Optional.of(token));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        tokenService.revokeRefreshToken("valid-refresh-token");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertTrue(captor.getValue().isRevoked());
        assertNotNull(captor.getValue().getRevokedAt());
    }

    @Test
    void revokeRefreshToken_tokenNotFound_throwsInvalidRefreshTokenException() {
        when(refreshTokenRepository.findByToken("ghost")).thenReturn(Optional.empty());

        assertThrows(InvalidRefreshTokenException.class,
                () -> tokenService.revokeRefreshToken("ghost"));
    }

    
    
    

    @Test
    void revokeAllUserTokens_revokesEveryActiveToken() {
        RefreshToken t1 = validToken();
        RefreshToken t2 = RefreshToken.builder()
                .id(2L).token("another-token").userId(1L)
                .expiresAt(LocalDateTime.now().plusDays(3)).revoked(false).build();

        when(refreshTokenRepository.findByUserIdAndRevokedFalse(1L)).thenReturn(List.of(t1, t2));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        tokenService.revokeAllUserTokens(1L);

        
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(2)).save(captor.capture());
        captor.getAllValues().forEach(t -> {
            assertTrue(t.isRevoked());
            assertNotNull(t.getRevokedAt());
        });
    }

    @Test
    void revokeAllUserTokens_noActiveTokens_doesNothing() {
        when(refreshTokenRepository.findByUserIdAndRevokedFalse(99L)).thenReturn(List.of());

        tokenService.revokeAllUserTokens(99L);

        verify(refreshTokenRepository, never()).save(any());
    }
}
