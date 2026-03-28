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


@RestController
@RequestMapping("/api/marketplace/bookings")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class MarketplaceBookingController {
    
    private final MarketplaceBookingService bookingService;
    
    
    @PostMapping
    public ResponseEntity<BookingResponseDTO> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        log.info("POST /api/marketplace/bookings - Creating booking for serviceId={}", request.getServiceId());
        
        BookingResponseDTO response = bookingService.createBooking(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    
    @GetMapping("/{id}")
    public ResponseEntity<BookingResponseDTO> getBookingById(@PathVariable Long id) {
        log.info("GET /api/marketplace/bookings/{} - Getting booking", id);
        
        BookingResponseDTO response = bookingService.getBookingById(id);
        
        return ResponseEntity.ok(response);
    }
    
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookingResponseDTO>> getBookingsByUserId(@PathVariable Long userId) {
        log.info("GET /api/marketplace/bookings/user/{} - Getting user bookings", userId);
        
        List<BookingResponseDTO> bookings = bookingService.getBookingsByUserId(userId);
        
        return ResponseEntity.ok(bookings);
    }
    
    
    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<BookingResponseDTO>> getBookingsByProviderId(@PathVariable Long providerId) {
        log.info("GET /api/marketplace/bookings/provider/{} - Getting provider bookings", providerId);
        
        List<BookingResponseDTO> bookings = bookingService.getBookingsByProviderId(providerId);
        
        return ResponseEntity.ok(bookings);
    }
    
    
    @GetMapping("/provider/{providerId}/upcoming")
    public ResponseEntity<List<BookingResponseDTO>> getUpcomingBookingsForProvider(@PathVariable Long providerId) {
        log.info("GET /api/marketplace/bookings/provider/{}/upcoming - Getting upcoming bookings", providerId);
        
        List<BookingResponseDTO> bookings = bookingService.getUpcomingBookingsForProvider(providerId);
        
        return ResponseEntity.ok(bookings);
    }
    
    
    @PatchMapping("/{id}/confirm")
    public ResponseEntity<BookingResponseDTO> confirmBooking(
            @PathVariable Long id,
            @RequestParam Long paymentId) {
        log.info("PATCH /api/marketplace/bookings/{}/confirm - paymentId={}", id, paymentId);
        
        BookingResponseDTO response = bookingService.confirmBooking(id, paymentId);
        
        return ResponseEntity.ok(response);
    }
    
    
    @PatchMapping("/{id}/start")
    public ResponseEntity<BookingResponseDTO> startBooking(@PathVariable Long id) {
        log.info("PATCH /api/marketplace/bookings/{}/start", id);
        
        BookingResponseDTO response = bookingService.startBooking(id);
        
        return ResponseEntity.ok(response);
    }
    
    
    @PatchMapping("/{id}/complete")
    public ResponseEntity<BookingResponseDTO> completeBooking(
            @PathVariable Long id,
            @RequestParam(required = false) BigDecimal finalPrice) {
        log.info("PATCH /api/marketplace/bookings/{}/complete - finalPrice={}", id, finalPrice);
        
        BookingResponseDTO response = bookingService.completeBooking(id, finalPrice);
        
        return ResponseEntity.ok(response);
    }
    
    
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<BookingResponseDTO> cancelBooking(
            @PathVariable Long id,
            @RequestParam String reason) {
        log.info("PATCH /api/marketplace/bookings/{}/cancel - reason={}", id, reason);
        
        BookingResponseDTO response = bookingService.cancelBooking(id, reason);
        
        return ResponseEntity.ok(response);
    }
}
