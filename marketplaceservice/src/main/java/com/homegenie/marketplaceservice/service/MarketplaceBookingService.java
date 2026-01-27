package com.homegenie.marketplaceservice.service;

import com.homegenie.marketplaceservice.dto.*;
import com.homegenie.marketplaceservice.event.BookingEvent;
import com.homegenie.marketplaceservice.model.*;
import com.homegenie.marketplaceservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Marketplace Booking Service
 * 
 * Core business logic for booking management.
 * Integrates with Payment Platform v2 and Notification Platform v1.
 * 
 * Key flows:
 * 1. Create booking → PENDING status
 * 2. Confirm booking (after payment) → CONFIRMED status
 * 3. Start service → IN_PROGRESS status
 * 4. Complete service → COMPLETED status → Trigger review request
 * 5. Cancel booking → CANCELLED status → Refund if paid
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarketplaceBookingService {
    
    private final MarketplaceBookingRepository bookingRepository;
    private final MarketplaceServiceRepository serviceRepository;
    private final MarketplaceProviderRepository providerRepository;
    private final RestTemplate restTemplate;
    private final KafkaTemplate<String, BookingEvent> kafkaTemplate;
    
    @Value("${external-services.payment-service.url}")
    private String paymentServiceUrl;
    
    @Value("${external-services.notification-service.url}")
    private String notificationServiceUrl;
    
    @Value("${marketplace.mini-app-id}")
    private String miniAppId;
    
    /**
     * Create new booking (Step 1)
     * Status: PENDING, Payment: PENDING
     * 
     * Frontend will call Payment Platform after this to get paymentId
     */
    @Transactional
    public BookingResponseDTO createBooking(CreateBookingRequest request) {
        log.info("Creating booking for serviceId={}, userId={}", request.getServiceId(), request.getUserId());
        
        // 1. Validate service exists and is available
        MarketplaceServiceEntity service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + request.getServiceId()));
        
        if (!service.isAvailable()) {
            throw new IllegalStateException("Service is not available: " + service.getStatus());
        }
        
        // 2. Validate provider is active
        MarketplaceProvider provider = providerRepository.findById(service.getProviderId())
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + service.getProviderId()));
        
        if (!provider.canAcceptBookings()) {
            throw new IllegalStateException("Provider cannot accept bookings: " + provider.getStatus());
        }
        
        // 3. Create booking entity
        MarketplaceBooking booking = MarketplaceBooking.builder()
                .userId(request.getUserId())
                .serviceId(service.getId())
                .providerId(provider.getId())
                .title(request.getTitle())
                .description(request.getDescription())
                .category(service.getCategory())
                .scheduledAt(request.getScheduledAt())
                .estimatedDurationMinutes(service.getDurationMinutes())
                .serviceAddress(request.getServiceAddress())
                .serviceLocationLat(request.getServiceLocationLat())
                .serviceLocationLng(request.getServiceLocationLng())
                .quotedPrice(service.getBasePrice())
                .currency(service.getCurrency())
                .customerNotes(request.getCustomerNotes())
                .imageUrlsJson(request.getImageUrlsJson())
                .status(BookingStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        
        MarketplaceBooking saved = bookingRepository.save(booking);
        
        log.info("Booking created successfully: id={}, status={}", saved.getId(), saved.getStatus());
        
        // Publish BookingCreatedEvent to Kafka
        publishBookingEvent("CREATED", saved);
        
        // Send booking confirmation email to customer
        sendBookingCreatedNotification(saved, provider, service);
        
        return mapToResponseDTO(saved, provider, service);
    }
    
    /**
     * Confirm booking after payment success (Step 2)
     * Called by Payment Platform webhook or frontend after payment
     * 
     * Status: PENDING → CONFIRMED
     * Payment: PENDING → PAID
     */
    @Transactional
    public BookingResponseDTO confirmBooking(Long bookingId, Long paymentId) {
        log.info("Confirming booking: bookingId={}, paymentId={}", bookingId, paymentId);
        
        MarketplaceBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Booking cannot be confirmed in current status: " + booking.getStatus());
        }
        
        booking.confirm(paymentId);
        MarketplaceBooking saved = bookingRepository.save(booking);
        
        log.info("Booking confirmed successfully: id={}, paymentId={}", saved.getId(), saved.getPaymentId());
        
        // Publish BookingConfirmedEvent to Kafka
        publishBookingEvent("CONFIRMED", saved);
        
        // Send notification to provider
        sendBookingConfirmedNotification(saved);
        
        return mapToResponseDTO(saved);
    }
    
    /**
     * Start service (Step 3 - Provider action)
     * Status: CONFIRMED → IN_PROGRESS
     */
    @Transactional
    public BookingResponseDTO startBooking(Long bookingId) {
        log.info("Starting booking: bookingId={}", bookingId);
        
        MarketplaceBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        
        booking.start();
        MarketplaceBooking saved = bookingRepository.save(booking);
        
        log.info("Booking started successfully: id={}", saved.getId());
        
        // Publish BookingStartedEvent to Kafka
        publishBookingEvent("STARTED", saved);
        
        // Send notification to customer
        sendBookingStartedNotification(saved);
        
        return mapToResponseDTO(saved);
    }
    
    /**
     * Complete service (Step 4 - Provider action)
     * Status: IN_PROGRESS → COMPLETED
     * 
     * Triggers review request notification
     */
    @Transactional
    public BookingResponseDTO completeBooking(Long bookingId, BigDecimal finalPrice) {
        log.info("Completing booking: bookingId={}, finalPrice={}", bookingId, finalPrice);
        
        MarketplaceBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        
        booking.complete(finalPrice);
        MarketplaceBooking saved = bookingRepository.save(booking);
        
        // Update provider metrics
        MarketplaceProvider provider = providerRepository.findById(booking.getProviderId())
                .orElseThrow(() -> new IllegalArgumentException("Provider not found"));
        provider.setCompletedBookings(provider.getCompletedBookings() + 1);
        provider.setTotalBookings(provider.getTotalBookings() + 1);
        providerRepository.save(provider);
        
        log.info("Booking completed successfully: id={}", saved.getId());
        
        // Publish BookingCompletedEvent to Kafka
        publishBookingEvent("COMPLETED", saved);
        
        // Send review request notification to customer
        sendReviewRequestNotification(saved, provider);
        
        return mapToResponseDTO(saved);
    }
    
    /**
     * Cancel booking
     * Status: PENDING/CONFIRMED → CANCELLED
     * 
     * If payment was made, initiate refund via Payment Platform
     */
    @Transactional
    public BookingResponseDTO cancelBooking(Long bookingId, String cancellationReason) {
        log.info("Cancelling booking: bookingId={}, reason={}", bookingId, cancellationReason);
        
        MarketplaceBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        
        booking.cancel(cancellationReason);
        MarketplaceBooking saved = bookingRepository.save(booking);
        
        log.info("Booking cancelled successfully: id={}", saved.getId());
        
        // Publish BookingCancelledEvent to Kafka
        publishBookingEvent("CANCELLED", saved);
        
        // If payment was made, we should refund (but this is handled by Payment Platform webhook)
        // Just send cancellation notifications
        sendBookingCancelledNotification(saved);
        
        return mapToResponseDTO(saved);
    }
    
    /**
     * Get booking by ID
     */
    public BookingResponseDTO getBookingById(Long bookingId) {
        MarketplaceBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        
        return mapToResponseDTO(booking);
    }
    
    /**
     * Get all bookings for user
     */
    public List<BookingResponseDTO> getBookingsByUserId(Long userId) {
        return bookingRepository.findByUserId(userId).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all bookings for provider
     */
    public List<BookingResponseDTO> getBookingsByProviderId(Long providerId) {
        return bookingRepository.findByProviderId(providerId).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get upcoming bookings for provider
     */
    public List<BookingResponseDTO> getUpcomingBookingsForProvider(Long providerId) {
        return bookingRepository.findUpcomingBookingsForProvider(providerId, LocalDateTime.now()).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }
    
    // ============================================================
    // Private Helper Methods
    // ============================================================
    
    /**
     * Publish booking event to Kafka
     */
    private void publishBookingEvent(String eventType, MarketplaceBooking booking) {
        try {
            BookingEvent event = BookingEvent.builder()
                    .eventType(eventType)
                    .bookingId(booking.getId())
                    .userId(booking.getUserId())
                    .providerId(booking.getProviderId())
                    .serviceId(booking.getServiceId())
                    .category(booking.getCategory().toString())
                    .status(booking.getStatus().toString())
                    .paymentStatus(booking.getPaymentStatus().toString())
                    .paymentId(booking.getPaymentId())
                    .amount(booking.getQuotedPrice())
                    .currency(booking.getCurrency())
                    .scheduledAt(booking.getScheduledAt())
                    .eventTimestamp(LocalDateTime.now())
                    .miniAppId(miniAppId)
                    .build();
            
            kafkaTemplate.send("marketplace.booking.events", event);
            log.info("Published {} event for booking {}", eventType, booking.getId());
        } catch (Exception e) {
            log.error("Failed to publish booking event: {}", e.getMessage(), e);
            // Don't fail the transaction due to event publishing error
        }
    }
    
    /**
     * Send booking created notification to customer
     */
    private void sendBookingCreatedNotification(MarketplaceBooking booking, 
                                                  MarketplaceProvider provider,
                                                  MarketplaceServiceEntity service) {
        try {
            Map<String, Object> templateData = new HashMap<>();
            templateData.put("bookingId", booking.getId());
            templateData.put("serviceName", service.getName());
            templateData.put("providerName", provider.getName());
            templateData.put("scheduledAt", booking.getScheduledAt().toString());
            templateData.put("quotedPrice", booking.getQuotedPrice() + " " + booking.getCurrency());
            templateData.put("serviceAddress", booking.getServiceAddress());
            
            NotificationRequest notification = NotificationRequest.builder()
                    .userId(booking.getUserId())
                    .miniAppId(miniAppId)
                    .type("EMAIL")
                    .templateId("booking_created")
                    .subject("Booking Confirmation - " + service.getName())
                    .body("Your booking has been created successfully. Booking ID: " + booking.getId())
                    .templateData(templateData)
                    .priority("NORMAL")
                    .build();
            
            restTemplate.postForEntity(
                    notificationServiceUrl + "/api/notifications/send",
                    notification,
                    String.class
            );
            
            log.info("Sent booking created notification to user {}", booking.getUserId());
        } catch (Exception e) {
            log.error("Failed to send booking created notification: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Send booking confirmed notification to provider
     */
    private void sendBookingConfirmedNotification(MarketplaceBooking booking) {
        try {
            Map<String, Object> templateData = new HashMap<>();
            templateData.put("bookingId", booking.getId());
            templateData.put("title", booking.getTitle());
            templateData.put("scheduledAt", booking.getScheduledAt().toString());
            templateData.put("serviceAddress", booking.getServiceAddress());
            templateData.put("customerNotes", booking.getCustomerNotes());
            
            NotificationRequest notification = NotificationRequest.builder()
                    .userId(booking.getProviderId()) // Provider's user ID
                    .miniAppId(miniAppId)
                    .type("EMAIL")
                    .templateId("booking_confirmed_provider")
                    .subject("New Booking Confirmed - " + booking.getTitle())
                    .body("A customer has confirmed a booking. Please prepare for service delivery.")
                    .templateData(templateData)
                    .priority("HIGH")
                    .build();
            
            restTemplate.postForEntity(
                    notificationServiceUrl + "/api/notifications/send",
                    notification,
                    String.class
            );
            
            log.info("Sent booking confirmed notification to provider {}", booking.getProviderId());
        } catch (Exception e) {
            log.error("Failed to send booking confirmed notification: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Send booking started notification to customer
     */
    private void sendBookingStartedNotification(MarketplaceBooking booking) {
        try {
            Map<String, Object> templateData = new HashMap<>();
            templateData.put("bookingId", booking.getId());
            templateData.put("title", booking.getTitle());
            
            NotificationRequest notification = NotificationRequest.builder()
                    .userId(booking.getUserId())
                    .miniAppId(miniAppId)
                    .type("EMAIL")
                    .templateId("booking_started")
                    .subject("Service Started - " + booking.getTitle())
                    .body("Your service provider has started working on your booking.")
                    .templateData(templateData)
                    .priority("NORMAL")
                    .build();
            
            restTemplate.postForEntity(
                    notificationServiceUrl + "/api/notifications/send",
                    notification,
                    String.class
            );
            
            log.info("Sent booking started notification to user {}", booking.getUserId());
        } catch (Exception e) {
            log.error("Failed to send booking started notification: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Send review request notification to customer after completion
     */
    private void sendReviewRequestNotification(MarketplaceBooking booking, MarketplaceProvider provider) {
        try {
            Map<String, Object> templateData = new HashMap<>();
            templateData.put("bookingId", booking.getId());
            templateData.put("title", booking.getTitle());
            templateData.put("providerName", provider.getName());
            
            NotificationRequest notification = NotificationRequest.builder()
                    .userId(booking.getUserId())
                    .miniAppId(miniAppId)
                    .type("EMAIL")
                    .templateId("review_request")
                    .subject("Rate Your Service - " + booking.getTitle())
                    .body("Your booking has been completed! Please take a moment to rate your experience.")
                    .templateData(templateData)
                    .priority("NORMAL")
                    .build();
            
            restTemplate.postForEntity(
                    notificationServiceUrl + "/api/notifications/send",
                    notification,
                    String.class
            );
            
            log.info("Sent review request notification to user {}", booking.getUserId());
        } catch (Exception e) {
            log.error("Failed to send review request notification: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Send booking cancelled notification
     */
    private void sendBookingCancelledNotification(MarketplaceBooking booking) {
        try {
            // Notify customer
            Map<String, Object> templateData = new HashMap<>();
            templateData.put("bookingId", booking.getId());
            templateData.put("title", booking.getTitle());
            templateData.put("cancellationReason", booking.getCancellationReason());
            
            NotificationRequest customerNotification = NotificationRequest.builder()
                    .userId(booking.getUserId())
                    .miniAppId(miniAppId)
                    .type("EMAIL")
                    .templateId("booking_cancelled")
                    .subject("Booking Cancelled - " + booking.getTitle())
                    .body("Your booking has been cancelled. Refund will be processed if applicable.")
                    .templateData(templateData)
                    .priority("HIGH")
                    .build();
            
            restTemplate.postForEntity(
                    notificationServiceUrl + "/api/notifications/send",
                    customerNotification,
                    String.class
            );
            
            // Notify provider
            NotificationRequest providerNotification = NotificationRequest.builder()
                    .userId(booking.getProviderId())
                    .miniAppId(miniAppId)
                    .type("EMAIL")
                    .templateId("booking_cancelled_provider")
                    .subject("Booking Cancelled - " + booking.getTitle())
                    .body("A booking has been cancelled by the customer.")
                    .templateData(templateData)
                    .priority("HIGH")
                    .build();
            
            restTemplate.postForEntity(
                    notificationServiceUrl + "/api/notifications/send",
                    providerNotification,
                    String.class
            );
            
            log.info("Sent cancellation notifications for booking {}", booking.getId());
        } catch (Exception e) {
            log.error("Failed to send cancellation notifications: {}", e.getMessage(), e);
        }
    }
    
    private BookingResponseDTO mapToResponseDTO(MarketplaceBooking booking) {
        return BookingResponseDTO.builder()
                .id(booking.getId())
                .userId(booking.getUserId())
                .serviceId(booking.getServiceId())
                .providerId(booking.getProviderId())
                .title(booking.getTitle())
                .description(booking.getDescription())
                .category(booking.getCategory())
                .scheduledAt(booking.getScheduledAt())
                .estimatedDurationMinutes(booking.getEstimatedDurationMinutes())
                .serviceAddress(booking.getServiceAddress())
                .serviceLocationLat(booking.getServiceLocationLat())
                .serviceLocationLng(booking.getServiceLocationLng())
                .quotedPrice(booking.getQuotedPrice())
                .finalPrice(booking.getFinalPrice())
                .currency(booking.getCurrency())
                .paymentId(booking.getPaymentId())
                .paymentStatus(booking.getPaymentStatus())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .completedAt(booking.getCompletedAt())
                .build();
    }
    
    private BookingResponseDTO mapToResponseDTO(MarketplaceBooking booking, 
                                                 MarketplaceProvider provider,
                                                 MarketplaceServiceEntity service) {
        BookingResponseDTO dto = mapToResponseDTO(booking);
        
        // Add nested provider info
        dto.setProvider(ProviderSummaryDTO.builder()
                .id(provider.getId())
                .name(provider.getName())
                .companyName(provider.getCompanyName())
                .status(provider.getStatus())
                .isVerified(provider.getIsVerified())
                .averageRating(provider.getAverageRating())
                .totalReviews(provider.getTotalReviews())
                .profilePhotoUrl(provider.getProfilePhotoUrl())
                .build());
        
        // Add nested service info
        dto.setService(ServiceSummaryDTO.builder()
                .id(service.getId())
                .name(service.getName())
                .category(service.getCategory())
                .basePrice(service.getBasePrice())
                .priceUnit(service.getPriceUnit())
                .currency(service.getCurrency())
                .imageUrl(service.getImageUrl())
                .build());
        
        return dto;
    }
}
