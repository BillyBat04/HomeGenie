package com.homegenie.maintenanceservice.service;

import com.homegenie.maintenanceservice.dto.MaintenanceRequestDTO;
import com.homegenie.maintenanceservice.dto.MaintenanceResponseDTO;
import com.homegenie.maintenanceservice.dto.UpdateRequestDTO;
import com.homegenie.maintenanceservice.dto.UserResponse;

import java.util.List;
import java.util.Map;

public interface MaintenanceService {

    MaintenanceResponseDTO createRequest(Long userId, MaintenanceRequestDTO dto);

    List<MaintenanceResponseDTO> getAllRequests();

    List<MaintenanceResponseDTO> getRequestsByUser(Long userId);

    MaintenanceResponseDTO getRequestById(Long id);

    MaintenanceResponseDTO updateRequest(Long id, UpdateRequestDTO dto);

    UserResponse getUserDetails(Long userId);

    void deleteRequest(Long id);

    Map<String, Long> getStatistics();

    List<UserResponse> getAllTechnicians();

    void retryPendingPayments();
}
