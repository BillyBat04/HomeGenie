package com.homegenie.marketplaceservice.service;

import com.homegenie.marketplaceservice.dto.BookingResponseDTO;
import com.homegenie.marketplaceservice.dto.CreateBookingRequest;

import java.math.BigDecimal;
import java.util.List;

public interface MarketplaceBookingService {

    BookingResponseDTO createBooking(CreateBookingRequest request);

    BookingResponseDTO confirmBooking(Long bookingId, Long paymentId);

    BookingResponseDTO startBooking(Long bookingId);

    BookingResponseDTO completeBooking(Long bookingId, BigDecimal finalPrice);

    BookingResponseDTO cancelBooking(Long bookingId, String cancellationReason);

    BookingResponseDTO getBookingById(Long bookingId);

    List<BookingResponseDTO> getBookingsByUserId(Long userId);

    List<BookingResponseDTO> getBookingsByProviderId(Long providerId);

    List<BookingResponseDTO> getUpcomingBookingsForProvider(Long providerId);
}
