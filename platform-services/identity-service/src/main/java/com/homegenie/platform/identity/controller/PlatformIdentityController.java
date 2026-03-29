package com.homegenie.platform.identity.controller;

import com.homegenie.platform.identity.dto.*;
import com.homegenie.platform.identity.service.IdentityService;
import com.homegenie.platform.identity.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/platform/identity/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Platform Identity", description = "Platform-level authentication and identity management APIs")
public class PlatformIdentityController {

    private final IdentityService identityService;
    private final TokenService tokenService;

    
    @PostMapping("/register")
    @Operation(
        summary = "Register new user (Platform API)",
        description = "Create a new user account with role-based access. This is the platform-level registration API."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "User registered successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request or email already exists"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Platform Identity API: Register new user - email: {}, role: {}", 
            request.getEmail(), request.getRole());
        
        AuthResponse response = identityService.register(request);
        
        log.info("User registered successfully - userId: {}", response.getUserId());
        return ResponseEntity.ok(response);
    }

    
    
    @PostMapping({"/authenticate", "/login"})
    @Operation(
        summary = "Authenticate user (Platform API)",
        description = "Authenticate user with email and password. Returns JWT access token and refresh token."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Authentication successful",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid credentials or account inactive"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public ResponseEntity<AuthResponse> authenticate(@Valid @RequestBody LoginRequest request) {
        log.info("Platform Identity API: Authenticate user - email: {}", request.getEmail());
        
        AuthResponse response = identityService.authenticate(request);
        
        log.info("Authentication successful - userId: {}", response.getUserId());
        return ResponseEntity.ok(response);
    }

    
    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh access token (Platform API)",
        description = "Get new access token using refresh token. Old refresh token will be rotated."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Token refreshed successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TokenRefreshResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid or expired refresh token"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public ResponseEntity<TokenRefreshResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        log.info("Platform Identity API: Refresh token");
        
        TokenRefreshResponse response = tokenService.refreshToken(request.getRefreshToken());
        
        log.info("Token refreshed successfully");
        return ResponseEntity.ok(response);
    }

    
    @PostMapping("/logout")
    @Operation(
        summary = "Logout user (Platform API)",
        description = "Revoke refresh token and invalidate user session"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Logout successful"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public ResponseEntity<String> logout(@Valid @RequestBody LogoutRequest request) {
        log.info("Platform Identity API: Logout user");
        
        tokenService.revokeRefreshToken(request.getRefreshToken());
        
        log.info("Logout successful");
        return ResponseEntity.ok("Logged out successfully");
    }

    
    @GetMapping("/users/{userId}")
    @Operation(
        summary = "Get user by ID (Platform API)",
        description = "Retrieve user information by user ID"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "User found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        log.info("Platform Identity API: Get user by ID - userId: {}", userId);
        
        UserResponse response = identityService.getUserById(userId);
        
        return ResponseEntity.ok(response);
    }

    
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if Identity Service is healthy")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Identity Platform Service is healthy");
    }
}
