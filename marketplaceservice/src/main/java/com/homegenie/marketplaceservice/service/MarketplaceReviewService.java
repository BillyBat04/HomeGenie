package com.homegenie.marketplaceservice.service;

import com.homegenie.marketplaceservice.dto.CreateReviewRequest;
import com.homegenie.marketplaceservice.dto.ReviewResponseDTO;

import java.util.List;

public interface MarketplaceReviewService {

    ReviewResponseDTO createReview(CreateReviewRequest request);

    List<ReviewResponseDTO> getReviewsForProvider(Long providerId);

    ReviewResponseDTO getReviewByBookingId(Long bookingId);

    List<ReviewResponseDTO> getReviewsByUser(Long userId);
}
