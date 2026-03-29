package com.homegenie.maintenanceservice.service;

import com.homegenie.maintenanceservice.dto.AIClassificationResponse;

public interface AIClassificationService {

    AIClassificationResponse classifyRequest(String title, String description);
}
