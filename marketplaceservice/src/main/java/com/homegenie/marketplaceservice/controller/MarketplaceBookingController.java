package com.homegenie.marketplaceservice.controller;

import com.homegenie.marketplaceservice.dto.BookingResponseDTO;
import com.homegenie.marketplaceservice.dto.CreateBookingRequest;
import com.homegenie.marketplaceservice.service.MarketplaceBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Marketplace Booking Controller
 * 
 * REST API for booking management.
 * Mini-App Context: marketplace (via X-MINI-APP header in Gateway)
 * 
 * Key endpoints:
 * - POST /api/marketplace/bookings - Create booking
 * - PATCH /api/marketplace/bookings/{id}/confirm - Confirm after payment
 * - PATCH /api/marketplace/bookings/{id}/start - Start service
 * - PATCH /api/marketplace/bookings/{id}/complete - Complete service
 * - PATCH /api/marketplace/bookings/{id}/cancel - Cancel booking
 */
@RestController
@RequestMapping("/api/marketplace/bookings")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class MarketplaceBookingController {
    
    private final MarketplaceBookingService bookingService;
    
    /**
     * Create new booking
     * 
     * POST /api/marketplace/bookings
     * 
     * After this, frontend should call Payment Platform:
     * POST /api/payments/mini-app with X-MINI-APP: marketplace
     */
    @PostMapping
    public ResponseEntity<BookingResponseDTO> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        log.info("POST /api/marketplace/bookings - Creating booking for serviceId={}", request.getServiceId());
        
        BookingResponseDTO response = bookingService.createBooking(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Get booking by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<BookingResponseDTO> getBookingById(@PathVariable Long id) {
        log.info("GET /api/marketplace/bookings/{} - Getting booking", id);
        
        BookingResponseDTO response = bookingService.getBookingById(id);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all bookings for user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookingResponseDTO>> getBookingsByUserId(@PathVariable Long userId) {
        log.info("GET /api/marketplace/bookings/user/{} - Getting user bookings", userId);
        
        List<BookingResponseDTO> bookings = bookingService.getBookingsByUserId(userId);
        
        return ResponseEntity.ok(bookings);
    }
    
    /**
     * Get all bookings for provider
     */
    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<BookingResponseDTO>> getBookingsByProviderId(@PathVariable Long providerId) {
        log.info("GET /api/marketplace/bookings/provider/{} - Getting provider bookings", providerId);
        
        List<BookingResponseDTO> bookings = bookingService.getBookingsByProviderId(providerId);
        
        return ResponseEntity.ok(bookings);
    }
    
    /**
     * Get upcoming bookings for provider
     */
    @GetMapping("/provider/{providerId}/upcoming")
    public ResponseEntity<List<BookingResponseDTO>> getUpcomingBookingsForProvider(@PathVariable Long providerId) {
        log.info("GET /api/marketplace/bookings/provider/{}/upcoming - Getting upcoming bookings", providerId);
        
        List<BookingResponseDTO> bookings = bookingService.getUpcomingBookingsForProvider(providerId);
        
        return ResponseEntity.ok(bookings);
    }
    
    /**
     * Confirm booking (after payment success)
     * 
     * PATCH /api/marketplace/bookings/{id}/confirm
     * 
     * Called by frontend after Payment Platform confirms payment.
     * Updates status: PENDING → CONFIRMED
     */
    @PatchMapping("/{id}/confirm")
    public ResponseEntity<BookingResponseDTO> confirmBooking(
            @PathVariable Long id,
            @RequestParam Long paymentId) {
        log.info("PATCH /api/marketplace/bookings/{}/confirm - paymentId={}", id, paymentId);
        
        BookingResponseDTO response = bookingService.confirmBooking(id, paymentId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Start service (provider action)
     * 
     * PATCH /api/marketplace/bookings/{id}/start
     * 
     * Updates status: CONFIRMED → IN_PROGRESS
     */
    @PatchMapping("/{id}/start")
    public ResponseEntity<BookingResponseDTO> startBooking(@PathVariable Long id) {
        log.info("PATCH /api/marketplace/bookings/{}/start", id);
        
        BookingResponseDTO response = bookingService.startBooking(id);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Complete service (provider action)
     * 
     * PATCH /api/marketplace/bookings/{id}/complete
     * 
     * Updates status: IN_PROGRESS → COMPLETED
     * Triggers review request notification
     */
    @PatchMapping("/{id}/complete")
    public ResponseEntity<BookingResponseDTO> completeBooking(
            @PathVariable Long id,
            @RequestParam(required = false) BigDecimal finalPrice) {
        log.info("PATCH /api/marketplace/bookings/{}/complete - finalPrice={}", id, finalPrice);
        
        BookingResponseDTO response = bookingService.completeBooking(id, finalPrice);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Cancel booking
     * 
     * PATCH /api/marketplace/bookings/{id}/cancel
     * 
     * Updates status: PENDING/CONFIRMED → CANCELLED
     * Initiates refund if payment was made
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<BookingResponseDTO> cancelBooking(
            @PathVariable Long id,
            @RequestParam String reason) {
        log.info("PATCH /api/marketplace/bookings/{}/cancel - reason={}", id, reason);
        
        BookingResponseDTO response = bookingService.cancelBooking(id, reason);
        
        return ResponseEntity.ok(response);
    }
}
