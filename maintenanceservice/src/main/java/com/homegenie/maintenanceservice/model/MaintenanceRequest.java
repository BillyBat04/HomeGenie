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

    /**
     * Reference to Item (optional - for scheduled/item-specific maintenance)
     * NULL for ad-hoc requests not related to a specific item
     */
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

    /**
     * Type of request: SCHEDULED (preventive) or AD_HOC (reactive)
     * Default: AD_HOC for backward compatibility
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RequestType requestType = RequestType.AD_HOC;

    private String imageUrl;

    private Long assignedTo;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    private LocalDateTime resolvedAt;

    private String adminNotes;

    /**
     * Tracks payment creation state for COMPLETED requests.
     * NULL for requests that are not yet COMPLETED.
     * Set to PENDING when the request is marked COMPLETED — the async payment call
     * then updates it to SUCCESS or FAILED depending on the result.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private PaymentStatus paymentStatus;

    /**
     * Check if this request is linked to a specific item
     */
    public boolean hasLinkedItem() {
        return itemId != null;
    }

    /**
     * Check if this is a scheduled/preventive maintenance
     */
    public boolean isScheduledMaintenance() {
        return requestType == RequestType.SCHEDULED;
    }

    /**
     * Check if request can be linked to an item
     */
    public boolean canLinkToItem() {
        return status == Status.PENDING || status == Status.IN_PROGRESS;
    }
}