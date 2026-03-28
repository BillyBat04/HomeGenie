package com.homegenie.maintenanceservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "maintenance_requests", indexes = {
    @Index(name = "idx_mr_user_status", columnList = "userId, status"),
    @Index(name = "idx_mr_created_at", columnList = "createdAt"),
    @Index(name = "idx_mr_assigned_to", columnList = "assignedTo, status"),
    @Index(name = "idx_mr_category_priority", columnList = "category, priority"),
    @Index(name = "idx_mr_item", columnList = "itemId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    
    private Long itemId;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    private Category category;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RequestType requestType = RequestType.AD_HOC;

    private String imageUrl;

    private Long assignedTo;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    private LocalDateTime resolvedAt;

    private String adminNotes;

    
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private PaymentStatus paymentStatus;

    
    public boolean hasLinkedItem() {
        return itemId != null;
    }

    
    public boolean isScheduledMaintenance() {
        return requestType == RequestType.SCHEDULED;
    }

    
    public boolean canLinkToItem() {
        return status == Status.PENDING || status == Status.IN_PROGRESS;
    }
}