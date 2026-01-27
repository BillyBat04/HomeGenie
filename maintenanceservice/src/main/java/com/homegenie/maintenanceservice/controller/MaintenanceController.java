package com.homegenie.maintenanceservice.controller;

import com.homegenie.maintenanceservice.dto.*;
import com.homegenie.maintenanceservice.service.MaintenanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
@Tag(name = "Maintenance Requests", description = "Maintenance request management operations")
@SecurityRequirement(name = "bearerAuth")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    @PostMapping
    @Operation(
        summary = "Create maintenance request",
        description = "Create new request with AI classification"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Request created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MaintenanceResponseDTO.class)
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<MaintenanceResponseDTO> createRequest(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody MaintenanceRequestDTO request) {
        return ResponseEntity.ok(maintenanceService.createRequest(userId, request));
    }

    @GetMapping
    @Operation(
        summary = "Get all requests",
        description = "Retrieve all maintenance requests"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "List of requests",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = MaintenanceResponseDTO.class))
            )
        )
    })
    public ResponseEntity<List<MaintenanceResponseDTO>> getAllRequests() {
        return ResponseEntity.ok(maintenanceService.getAllRequests());
    }

    @GetMapping("/user/{userId}")
    @Operation(
        summary = "Get user requests",
        description = "Retrieve requests by user ID"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "User requests",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = MaintenanceResponseDTO.class))
            )
        )
    })
    public ResponseEntity<List<MaintenanceResponseDTO>> getUserRequests(@PathVariable Long userId) {
        return ResponseEntity.ok(maintenanceService.getRequestsByUser(userId));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get request by ID",
        description = "Retrieve single maintenance request"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Request found",
            content = @Content(schema = @Schema(implementation = MaintenanceResponseDTO.class))
        ),
        @ApiResponse(responseCode = "404", description = "Request not found")
    })
    public ResponseEntity<MaintenanceResponseDTO> getRequest(@PathVariable Long id) {
        return ResponseEntity.ok(maintenanceService.getRequestById(id));
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Update request",
        description = "Update maintenance request status or assignment"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Request updated",
            content = @Content(schema = @Schema(implementation = MaintenanceResponseDTO.class))
        ),
        @ApiResponse(responseCode = "404", description = "Request not found")
    })
    public ResponseEntity<MaintenanceResponseDTO> updateRequest(
            @PathVariable Long id,
            @RequestBody UpdateRequestDTO request) {
        return ResponseEntity.ok(maintenanceService.updateRequest(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete request",
        description = "Delete maintenance request by ID"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Request deleted"),
        @ApiResponse(responseCode = "404", description = "Request not found")
    })
    public ResponseEntity<Void> deleteRequest(@PathVariable Long id) {
        maintenanceService.deleteRequest(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/statistics")
    @Operation(
        summary = "Get statistics",
        description = "Retrieve maintenance request statistics"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statistics data")
    })
    public ResponseEntity<Map<String, Long>> getStatistics() {
        return ResponseEntity.ok(maintenanceService.getStatistics());
    }

    @GetMapping("/technicians")
    @Operation(
        summary = "Get technicians",
        description = "Retrieve all available technicians"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "List of technicians",
            content = @Content(
                array = @ArraySchema(schema = @Schema(implementation = UserResponse.class))
            )
        )
    })
    public ResponseEntity<List<UserResponse>> getTechnicians() {
        return ResponseEntity.ok(maintenanceService.getAllTechnicians());
    }
}