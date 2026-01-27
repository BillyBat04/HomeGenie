package com.homegenie.userservice.dto.event;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdatedEvent {
    
    private String eventId;
    private Long userId;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String role;
    private String specialty;
    private String updateType; // PROFILE_UPDATE, ROLE_CHANGE, STATUS_CHANGE
    private String eventType;
    private Instant timestamp;
}
