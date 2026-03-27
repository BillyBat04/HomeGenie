package com.homegenie.marketplaceservice.dto;

import com.homegenie.marketplaceservice.model.PriceUnit;
import com.homegenie.marketplaceservice.model.ServiceCategory;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateServiceRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be less than 255 characters")
    private String name;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Category is required")
    private ServiceCategory category;

    @NotNull(message = "Provider ID is required")
    private Long providerId;

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.01", message = "Base price must be positive")
    private BigDecimal basePrice;

    private PriceUnit priceUnit;
    private String currency;
    private Boolean isFeatured;
    private String imageUrl;
    private Integer durationMinutes;
}
