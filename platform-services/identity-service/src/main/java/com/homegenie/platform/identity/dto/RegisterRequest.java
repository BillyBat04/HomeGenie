package com.homegenie.platform.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Register Request DTO
 * ✅ IDENTICAL to User Service RegisterRequest
 * Ensures backward compatibility
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).*$",
        message = "Password must contain at least one digit, one lowercase, one uppercase, and one special character"
    )
    private String password;

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String phoneNumber;

    private String flatNumber;

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "RESIDENT|TECHNICIAN|ADMIN", message = "Role must be RESIDENT, TECHNICIAN, or ADMIN")
    private String role;

    private String technicianSpecialty; // For TECHNICIAN role only
}
