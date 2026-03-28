package com.homegenie.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {
    
    private String eventId;
    private Long userId;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String role;
    private String specialty;
    private String updateType; 
    private String eventType; 
    private Instant timestamp;
}
