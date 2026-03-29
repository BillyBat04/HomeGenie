package com.homegenie.platform.identity.model;

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

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String phoneNumber;

    private String flatNumber;

    
    private String specialty;

    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.RESIDENT;

    private LocalDateTime createdAt = LocalDateTime.now();

    private boolean active = true;

    
    private boolean emailNotificationsEnabled = true;
}
