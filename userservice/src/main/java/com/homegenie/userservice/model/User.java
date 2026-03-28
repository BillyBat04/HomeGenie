package com.homegenie.userservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_role_active", columnList = "role, active"),
    @Index(name = "idx_user_specialty", columnList = "specialty")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    // Credentials managed by identity-service — nullable post-migration V2
    @Column(nullable = true)
    private String password;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = true)
    private String phoneNumber;

    private String flatNumber;

    // For technicians - their area of expertise
    private String specialty;

    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.RESIDENT;

    private LocalDateTime createdAt = LocalDateTime.now();

    private boolean active = true;

    // Email notification preferences
    private boolean emailNotificationsEnabled = true;
}