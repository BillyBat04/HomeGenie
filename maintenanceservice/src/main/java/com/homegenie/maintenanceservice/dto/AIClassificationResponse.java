package com.homegenie.maintenanceservice.dto;

import com.homegenie.maintenanceservice.model.Category;
import com.homegenie.maintenanceservice.model.Priority;
import lombok.Data;

@Data
public class AIClassificationResponse {
    private Category category;
    private Priority priority;
    private String reasoning;
}
