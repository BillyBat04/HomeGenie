package com.homegenie.maintenanceservice.service;

public interface EmailNotificationService {

    void notifyAdminNewRequest(String userName, String requestTitle,
                               String category, String priority, Long requestId);

    void notifyTechnicianAssignment(String technicianEmail, String technicianName,
                                    String requestTitle, String category,
                                    String priority, Long requestId);

    void notifyResidentStatusChange(String residentEmail, String residentName,
                                    String requestTitle, String oldStatus,
                                    String newStatus, Long requestId);
}
