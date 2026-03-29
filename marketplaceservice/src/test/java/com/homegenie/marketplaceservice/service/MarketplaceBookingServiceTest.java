package com.homegenie.marketplaceservice.service;

import com.homegenie.marketplaceservice.service.impl.MarketplaceBookingServiceImpl;
import com.homegenie.marketplaceservice.dto.BookingResponseDTO;
import com.homegenie.marketplaceservice.dto.CreateBookingRequest;
import com.homegenie.marketplaceservice.dto.PaymentVerificationDTO;
import com.homegenie.marketplaceservice.event.BookingEvent;
import com.homegenie.marketplaceservice.model.*;
import com.homegenie.marketplaceservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class MarketplaceBookingServiceTest {

    @Mock
    private MarketplaceBookingRepository bookingRepository;
    
    @Mock
    private MarketplaceServiceRepository serviceRepository;
    
    @Mock
    private MarketplaceProviderRepository providerRepository;
    
    @Mock
    private RestTemplate restTemplate;
    
    @Mock
    private KafkaTemplate<String, BookingEvent> kafkaTemplate;
    
    @InjectMocks
    private MarketplaceBookingServiceImpl bookingService;
    
    private MarketplaceProvider testProvider;
    private MarketplaceServiceEntity testService;
    private MarketplaceBooking testBooking;
    
    @BeforeEach
    void setUp() {
        
        testProvider = MarketplaceProvider.builder()
                .id(1L)
                .name("Test Plumber")
                .email("plumber@test.com")
                .phone("123456789")
                .status(ProviderStatus.ACTIVE)
                .isVerified(true)
                .totalBookings(0)
                .completedBookings(0)
                .averageRating(BigDecimal.ZERO)
                .totalReviews(0)
                .build();
        
        testService = MarketplaceServiceEntity.builder()
                .id(1L)
                .providerId(1L)
                .name("Emergency Plumbing")
                .description("24/7 plumbing service")
                .category(ServiceCategory.PLUMBING)
                .basePrice(BigDecimal.valueOf(150.00))
                .priceUnit(PriceUnit.PER_JOB)
                .currency("USD")
                .status(ServiceStatus.ACTIVE)
                .durationMinutes(120)
                .build();
        
        testBooking = MarketplaceBooking.builder()
                .id(1L)
                .userId(100L)
                .serviceId(1L)
                .providerId(1L)
                .title("Fix leaking pipe")
                .description("Kitchen sink leaking")
                .category(ServiceCategory.PLUMBING)
                .scheduledAt(LocalDateTime.now().plusDays(1))
                .serviceAddress("123 Test St")
                .quotedPrice(BigDecimal.valueOf(150.00))
                .currency("USD")
                .status(BookingStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        
        
        ReflectionTestUtils.setField(bookingService, "paymentServiceUrl", "http://localhost:8083");
        ReflectionTestUtils.setField(bookingService, "notificationServiceUrl", "http://localhost:8084");
        ReflectionTestUtils.setField(bookingService, "miniAppId", "marketplace");
    }
    
    @Test
    void testCreateBooking_Success() {        
        CreateBookingRequest request = CreateBookingRequest.builder()
                .serviceId(1L)
                .userId(100L)
                .title("Fix leaking pipe")
                .description("Kitchen sink leaking")
                .scheduledAt(LocalDateTime.now().plusDays(1))
                .serviceAddress("123 Test St")
                .build();
        
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(testService));
        when(providerRepository.findById(1L)).thenReturn(Optional.of(testProvider));
        when(bookingRepository.save(any(MarketplaceBooking.class))).thenReturn(testBooking);
        
        when(providerRepository.save(any(MarketplaceProvider.class))).thenReturn(testProvider);        
        
        BookingResponseDTO result = bookingService.createBooking(request);        
        
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(BookingStatus.PENDING, result.getStatus());
        assertEquals(PaymentStatus.PENDING, result.getPaymentStatus());
        assertEquals(BigDecimal.valueOf(150.00), result.getQuotedPrice());        
        
        verify(serviceRepository).findById(1L);
        verify(providerRepository).findById(1L);
        verify(bookingRepository).save(any(MarketplaceBooking.class));        
        
        verify(kafkaTemplate, times(1)).send(eq("marketplace.booking.events"), any(BookingEvent.class));
        
        verify(restTemplate, times(1)).postForEntity(
                contains("/api/notifications/send"),
                any(),
                eq(String.class)
        );
    }
    
    @Test
    void testCreateBooking_ServiceNotFound() {
        CreateBookingRequest request = CreateBookingRequest.builder()
                .serviceId(999L)
                .userId(100L)
                .title("Fix leaking pipe")
                .build();
        
        when(serviceRepository.findById(999L)).thenReturn(Optional.empty());        
        
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(request)
        );
        
        assertEquals("Service not found: 999", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }
    
    @Test
    void testCreateBooking_ServiceNotAvailable() {        
        testService.setStatus(ServiceStatus.INACTIVE);
        
        CreateBookingRequest request = CreateBookingRequest.builder()
                .serviceId(1L)
                .userId(100L)
                .title("Fix leaking pipe")
                .build();
        
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(testService));        
        
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> bookingService.createBooking(request)
        );
        
        assertTrue(exception.getMessage().contains("Service is not available"));
        verify(bookingRepository, never()).save(any());
    }
    
    @Test
    void testCreateBooking_ProviderNotActive() {        
        testProvider.setStatus(ProviderStatus.SUSPENDED);
        
        CreateBookingRequest request = CreateBookingRequest.builder()
                .serviceId(1L)
                .userId(100L)
                .title("Fix leaking pipe")
                .build();
        
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(testService));
        when(providerRepository.findById(1L)).thenReturn(Optional.of(testProvider));        
        
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> bookingService.createBooking(request)
        );
        
        assertTrue(exception.getMessage().contains("Provider cannot accept bookings"));
        verify(bookingRepository, never()).save(any());
    }
    
    @Test
    void testConfirmBooking_Success() {
        Long bookingId = 1L;
        Long paymentId = 888L;

        
        PaymentVerificationDTO paymentDTO = new PaymentVerificationDTO();
        paymentDTO.setId(paymentId);
        paymentDTO.setOrderId(bookingId);
        paymentDTO.setStatus("SUCCESS");
        when(restTemplate.getForObject(
                contains("/api/payments/" + paymentId),
                eq(PaymentVerificationDTO.class)
        )).thenReturn(paymentDTO);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(MarketplaceBooking.class))).thenAnswer(i -> {
            MarketplaceBooking saved = i.getArgument(0);
            saved.setStatus(BookingStatus.CONFIRMED);
            saved.setPaymentId(paymentId);
            saved.setPaymentStatus(PaymentStatus.PAID);
            return saved;
        });        
        
        BookingResponseDTO result = bookingService.confirmBooking(bookingId, paymentId);        
        
        assertNotNull(result);
        assertEquals(paymentId, result.getPaymentId());
        assertEquals(BookingStatus.CONFIRMED, result.getStatus());
        assertEquals(PaymentStatus.PAID, result.getPaymentStatus());        
        
        ArgumentCaptor<BookingEvent> eventCaptor = ArgumentCaptor.forClass(BookingEvent.class);
        verify(kafkaTemplate).send(eq("marketplace.booking.events"), eventCaptor.capture());
        
        BookingEvent capturedEvent = eventCaptor.getValue();
        assertEquals("CONFIRMED", capturedEvent.getEventType());
        assertEquals(bookingId, capturedEvent.getBookingId());
    }
    
    @Test
    void testConfirmBooking_InvalidStatus() {        
        testBooking.setStatus(BookingStatus.COMPLETED);
        
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        
        
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> bookingService.confirmBooking(1L, 888L)
        );
        
        assertTrue(exception.getMessage().contains("cannot be confirmed"));
        verify(bookingRepository, never()).save(any());
    }
    
    @Test
    void testStartBooking_Success() {        
        testBooking.setStatus(BookingStatus.CONFIRMED);
        testBooking.setPaymentId(888L);
        testBooking.setPaymentStatus(PaymentStatus.PAID);
        
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(MarketplaceBooking.class))).thenAnswer(i -> {
            MarketplaceBooking saved = i.getArgument(0);
            saved.setStatus(BookingStatus.IN_PROGRESS);
            saved.setStartedAt(LocalDateTime.now());
            return saved;
        });        
        
        BookingResponseDTO result = bookingService.startBooking(1L);        
        
        assertEquals(BookingStatus.IN_PROGRESS, result.getStatus());        
        
        verify(kafkaTemplate).send(eq("marketplace.booking.events"), any(BookingEvent.class));
    }
    
    @Test
    void testCompleteBooking_Success() {        
        testBooking.setStatus(BookingStatus.IN_PROGRESS);
        testBooking.setPaymentId(888L);
        testBooking.setPaymentStatus(PaymentStatus.PAID);
        
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(providerRepository.findById(1L)).thenReturn(Optional.of(testProvider));
        when(bookingRepository.save(any(MarketplaceBooking.class))).thenAnswer(i -> {
            MarketplaceBooking saved = i.getArgument(0);
            saved.setStatus(BookingStatus.COMPLETED);
            saved.setCompletedAt(LocalDateTime.now());
            saved.setFinalPrice(BigDecimal.valueOf(150.00));
            return saved;
        });
        when(providerRepository.save(any(MarketplaceProvider.class))).thenReturn(testProvider);        
        
        BookingResponseDTO result = bookingService.completeBooking(1L, BigDecimal.valueOf(150.00));        
        
        assertEquals(BookingStatus.COMPLETED, result.getStatus());
        assertEquals(BigDecimal.valueOf(150.00), result.getFinalPrice());        
        
        
        verify(providerRepository).save(argThat(provider ->
            provider.getCompletedBookings() == 1 &&
            provider.getTotalBookings() == 0   
        ));        
        
        verify(kafkaTemplate).send(eq("marketplace.booking.events"), any(BookingEvent.class));
    }
    
    @Test
    void testCancelBooking_Success() {        
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(MarketplaceBooking.class))).thenAnswer(i -> {
            MarketplaceBooking saved = i.getArgument(0);
            saved.setStatus(BookingStatus.CANCELLED);
            saved.setCancellationReason("Changed my mind");
            saved.setCancelledAt(LocalDateTime.now());
            return saved;
        });        
        
        BookingResponseDTO result = bookingService.cancelBooking(1L, "Changed my mind");        
        
        assertEquals(BookingStatus.CANCELLED, result.getStatus());        
        
        verify(kafkaTemplate).send(eq("marketplace.booking.events"), any(BookingEvent.class));
    }
    
    @Test
    void testGetBookingById_Success() {        
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));        
        
        BookingResponseDTO result = bookingService.getBookingById(1L);        
        
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Fix leaking pipe", result.getTitle());
    }
    
    @Test
    void testGetBookingById_NotFound() {        
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());          
        assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.getBookingById(999L)
        );
    }
}
