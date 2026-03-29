package com.homegenie.maintenanceservice.controller;

import com.homegenie.maintenanceservice.dto.*;
import com.homegenie.maintenanceservice.model.ItemCategory;
import com.homegenie.maintenanceservice.model.ItemStatus;
import com.homegenie.maintenanceservice.service.ItemService;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Item Management", description = "Household item lifecycle management and maintenance tracking")
@SecurityRequirement(name = "bearerAuth")
public class ItemController {

    private final ItemService itemService;

    @PostMapping
    @Operation(
        summary = "Create new household item",
        description = "Register a new household item with warranty and maintenance schedule. " +
                      "Automatically calculates next maintenance date based on frequency."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Item created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ItemResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data (validation errors)"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid JWT token"
        )
    })
    public ResponseEntity<ItemResponse> createItem(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ItemRequest request) {
        log.info("POST /api/items - User: {}, Item: {}", userId, request.getName());
        ItemResponse response = itemService.createItem(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(
        summary = "List user's household items",
        description = "Retrieve all household items for the authenticated user. " +
                      "Supports filtering by status and/or category."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "List of items retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = ItemSummaryDTO.class))
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        )
    })
    public ResponseEntity<List<ItemSummaryDTO>> getUserItems(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) ItemStatus status,
            @RequestParam(required = false) ItemCategory category) {
        log.info("GET /api/items - User: {}, Status: {}, Category: {}", userId, status, category);
        List<ItemSummaryDTO> items = itemService.getUserItems(userId, status, category);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get item details",
        description = "Retrieve full details of a specific household item including calculated fields " +
                      "(warranty status, maintenance due dates, etc.)"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Item found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ItemResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Item not found or access denied"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        )
    })
    public ResponseEntity<ItemResponse> getItem(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        log.info("GET /api/items/{} - User: {}", id, userId);
        ItemResponse item = itemService.getItem(userId, id);
        return ResponseEntity.ok(item);
    }


    @PutMapping("/{id}")
    @Operation(
        summary = "Update household item",
        description = "Update item details including warranty info, maintenance frequency, and status. " +
                      "Changing maintenance frequency will recalculate next maintenance date."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Item updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ItemResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Item not found or access denied"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        )
    })
    public ResponseEntity<ItemResponse> updateItem(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody ItemRequest request) {
        log.info("PUT /api/items/{} - User: {}", id, userId);
        ItemResponse updated = itemService.updateItem(userId, id, request);
        return ResponseEntity.ok(updated);
    }


    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete household item",
        description = "Soft delete an item by marking it as RETIRED. " +
                      "Item data is preserved for historical maintenance records."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "204",
            description = "Item deleted (retired) successfully"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Item not found or access denied"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        )
    })
    public ResponseEntity<Void> deleteItem(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        log.info("DELETE /api/items/{} - User: {}", id, userId);
        itemService.deleteItem(userId, id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/{id}/history")
    @Operation(
        summary = "Get item maintenance history",
        description = "Retrieve complete maintenance history for a specific item, " +
                      "including scheduled and ad-hoc maintenance requests."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Maintenance history retrieved",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = MaintenanceHistoryDTO.class))
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Item not found or access denied"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        )
    })
    public ResponseEntity<List<MaintenanceHistoryDTO>> getItemHistory(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        log.info("GET /api/items/{}/history - User: {}", id, userId);
        List<MaintenanceHistoryDTO> history = itemService.getItemMaintenanceHistory(userId, id);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/due-maintenance")
    @Operation(
        summary = "Get items needing maintenance",
        description = "Retrieve all items that are due for maintenance today or are overdue. " +
                      "Useful for maintenance reminder notifications."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Items due for maintenance retrieved",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = ItemSummaryDTO.class))
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        )
    })
    public ResponseEntity<List<ItemSummaryDTO>> getItemsDueForMaintenance(
            @RequestHeader("X-User-Id") Long userId) {
        log.info("GET /api/items/due-maintenance - User: {}", userId);
        List<ItemSummaryDTO> items = itemService.getItemsDueForMaintenance(userId);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/statistics")
    @Operation(
        summary = "Get item statistics",
        description = "Retrieve dashboard statistics including item counts by status/category, " +
                      "and alerts for maintenance due, warranty expiring, etc."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Statistics retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        )
    })
    public ResponseEntity<Map<String, Object>> getItemStatistics(
            @RequestHeader("X-User-Id") Long userId) {
        log.info("GET /api/items/statistics - User: {}", userId);
        Map<String, Object> stats = itemService.getUserItemStatistics(userId);
        return ResponseEntity.ok(stats);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("Validation error: {}", e.getMessage());
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation Error",
            e.getMessage()
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Internal error", e);
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "An unexpected error occurred"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @Schema(description = "Error response structure")
    public record ErrorResponse(
        @Schema(description = "HTTP status code", example = "400")
        int status,
        
        @Schema(description = "Error type", example = "Validation Error")
        String error,
        
        @Schema(description = "Error message", example = "Warranty expiry date cannot be before purchase date")
        String message
    ) {}
}
