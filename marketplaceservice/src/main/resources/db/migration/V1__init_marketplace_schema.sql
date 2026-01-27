-- ================================================================
-- Flyway Migration V1: Initial Marketplace Service Schema
-- ================================================================
-- Creates tables: marketplace_providers, marketplace_services,
--                marketplace_bookings, marketplace_reviews
-- Author: HomeGenie Platform Team
-- Date: 2026-01-17
-- Phase: B1 - Marketplace Mini-App
-- ================================================================

-- ================================================================
-- Table 1: marketplace_providers
-- External service provider profiles
-- ================================================================
CREATE TABLE marketplace_providers (
    id BIGSERIAL PRIMARY KEY,
    
    -- Basic Info
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    
    -- Business Details
    company_name VARCHAR(255),
    license_number VARCHAR(100),
    
    -- Profile
    profile_photo_url TEXT,
    bio TEXT,
    
    -- Status
    status VARCHAR(30) DEFAULT 'PENDING_VERIFICATION',
    is_verified BOOLEAN DEFAULT FALSE,
    
    -- Performance Metrics (denormalized for quick access)
    total_bookings INT DEFAULT 0,
    completed_bookings INT DEFAULT 0,
    average_rating DECIMAL(3, 2), -- 0.00 to 5.00
    total_reviews INT DEFAULT 0,
    
    -- Availability (stored as JSON)
    service_areas_json TEXT, -- JSON array: ["District 1", "District 3"]
    working_hours_json TEXT, -- JSON object: {"monday": "09:00-17:00"}
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verified_at TIMESTAMP,
    last_active_at TIMESTAMP
);

-- Indexes for marketplace_providers
CREATE INDEX idx_mp_status ON marketplace_providers(status);
CREATE INDEX idx_mp_verified ON marketplace_providers(is_verified) WHERE is_verified = TRUE;
CREATE INDEX idx_mp_rating ON marketplace_providers(average_rating DESC, total_reviews DESC);
CREATE INDEX idx_mp_email ON marketplace_providers(email);

COMMENT ON TABLE marketplace_providers IS 'External service providers (plumbers, electricians, cleaners, etc.)';
COMMENT ON COLUMN marketplace_providers.status IS 'PENDING_VERIFICATION → ACTIVE → SUSPENDED → BLOCKED';
COMMENT ON COLUMN marketplace_providers.average_rating IS 'Calculated from marketplace_reviews, updated on each new review';

-- ================================================================
-- Table 2: marketplace_services
-- Catalog of services offered by providers
-- ================================================================
CREATE TABLE marketplace_services (
    id BIGSERIAL PRIMARY KEY,
    
    -- Service Details
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(100) NOT NULL, -- PLUMBING, ELECTRICAL, CLEANING, etc.
    
    -- Provider Reference
    provider_id BIGINT NOT NULL,
    
    -- Pricing
    base_price DECIMAL(10, 2) NOT NULL,
    price_unit VARCHAR(50) DEFAULT 'PER_JOB', -- PER_JOB, PER_HOUR, PER_SQFT, PER_DAY
    currency VARCHAR(3) DEFAULT 'USD',
    
    -- Availability
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, SUSPENDED
    is_featured BOOLEAN DEFAULT FALSE,
    
    -- Metadata
    image_url TEXT,
    duration_minutes INT, -- Estimated duration
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign Key
    CONSTRAINT fk_ms_provider FOREIGN KEY (provider_id) 
        REFERENCES marketplace_providers(id) ON DELETE CASCADE
);

-- Indexes for marketplace_services
CREATE INDEX idx_ms_provider ON marketplace_services(provider_id);
CREATE INDEX idx_ms_category_status ON marketplace_services(category, status);
CREATE INDEX idx_ms_featured ON marketplace_services(is_featured) WHERE is_featured = TRUE;
CREATE INDEX idx_ms_status ON marketplace_services(status);

COMMENT ON TABLE marketplace_services IS 'Catalog of services offered by external providers';
COMMENT ON COLUMN marketplace_services.provider_id IS 'External provider (NOT internal technician)';
COMMENT ON COLUMN marketplace_services.base_price IS 'Base price, actual price may vary per booking';

-- ================================================================
-- Table 3: marketplace_bookings
-- User bookings with providers (analogous to maintenance_requests)
-- ================================================================
CREATE TABLE marketplace_bookings (
    id BIGSERIAL PRIMARY KEY,
    
    -- Customer Reference
    user_id BIGINT NOT NULL, -- HomeGenie user
    
    -- Service Reference
    service_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    
    -- Booking Details
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(100) NOT NULL, -- Denormalized from service
    
    -- Scheduling
    scheduled_at TIMESTAMP NOT NULL,
    estimated_duration_minutes INT,
    
    -- Location
    service_address TEXT NOT NULL,
    service_location_lat DECIMAL(10, 8),
    service_location_lng DECIMAL(11, 8),
    
    -- Pricing
    quoted_price DECIMAL(10, 2) NOT NULL,
    final_price DECIMAL(10, 2),
    currency VARCHAR(3) DEFAULT 'USD',
    
    -- Payment Integration (Mini-App Aware!)
    payment_id BIGINT, -- Reference to payments table (Payment Platform v2)
    payment_status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, PAID, FAILED, REFUNDED
    
    -- Status Tracking
    status VARCHAR(30) DEFAULT 'PENDING',
    -- PENDING → CONFIRMED → IN_PROGRESS → COMPLETED → CANCELLED → REFUNDED
    
    -- Assignment Timestamps
    assigned_at TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    
    -- Additional Info
    image_urls_json TEXT, -- JSON array of image URLs
    provider_notes TEXT,
    customer_notes TEXT,
    cancellation_reason TEXT,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign Keys
    CONSTRAINT fk_mb_service FOREIGN KEY (service_id) 
        REFERENCES marketplace_services(id) ON DELETE RESTRICT,
    CONSTRAINT fk_mb_provider FOREIGN KEY (provider_id) 
        REFERENCES marketplace_providers(id) ON DELETE RESTRICT
);

-- Indexes for marketplace_bookings (pattern from maintenance_requests)
CREATE INDEX idx_mb_user_status ON marketplace_bookings(user_id, status);
CREATE INDEX idx_mb_provider_status ON marketplace_bookings(provider_id, status);
CREATE INDEX idx_mb_service ON marketplace_bookings(service_id);
CREATE INDEX idx_mb_created_at ON marketplace_bookings(created_at DESC);
CREATE INDEX idx_mb_scheduled_at ON marketplace_bookings(scheduled_at);
CREATE INDEX idx_mb_payment ON marketplace_bookings(payment_id);
CREATE INDEX idx_mb_status ON marketplace_bookings(status);

COMMENT ON TABLE marketplace_bookings IS 'User bookings with external service providers';
COMMENT ON COLUMN marketplace_bookings.payment_id IS 'Links to Payment Platform v2 with miniAppId=marketplace';
COMMENT ON COLUMN marketplace_bookings.status IS 'Booking lifecycle: PENDING → CONFIRMED → IN_PROGRESS → COMPLETED';
COMMENT ON COLUMN marketplace_bookings.payment_status IS 'Separate payment tracking for mini-app integration';

-- ================================================================
-- Table 4: marketplace_reviews
-- Provider rating & review system
-- ================================================================
CREATE TABLE marketplace_reviews (
    id BIGSERIAL PRIMARY KEY,
    
    -- Relationships
    booking_id BIGINT NOT NULL UNIQUE, -- One review per booking
    user_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    
    -- Rating
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    
    -- Review Content
    title VARCHAR(255),
    comment TEXT,
    
    -- Media
    photo_urls_json TEXT, -- JSON array
    
    -- Status
    is_verified BOOLEAN DEFAULT FALSE, -- Admin verified
    is_visible BOOLEAN DEFAULT TRUE,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign Keys
    CONSTRAINT fk_mr_booking FOREIGN KEY (booking_id) 
        REFERENCES marketplace_bookings(id) ON DELETE CASCADE,
    CONSTRAINT fk_mr_provider FOREIGN KEY (provider_id)
        REFERENCES marketplace_providers(id) ON DELETE CASCADE
);

-- Indexes for marketplace_reviews
CREATE INDEX idx_mr_provider_visible ON marketplace_reviews(provider_id, is_visible);
CREATE INDEX idx_mr_rating ON marketplace_reviews(rating DESC);
CREATE INDEX idx_mr_created_at ON marketplace_reviews(created_at DESC);
CREATE INDEX idx_mr_user ON marketplace_reviews(user_id);
CREATE INDEX idx_mr_booking ON marketplace_reviews(booking_id);

COMMENT ON TABLE marketplace_reviews IS 'Customer reviews for service providers';
COMMENT ON COLUMN marketplace_reviews.booking_id IS 'One review per booking (business rule enforced via UNIQUE constraint)';
COMMENT ON COLUMN marketplace_reviews.rating IS '1-5 stars rating (1=poor, 5=excellent)';

-- ================================================================
-- Initial Data (Optional - for testing)
-- ================================================================

-- Sample provider for testing
INSERT INTO marketplace_providers (name, email, phone, status, is_verified, company_name) VALUES
('ABC Plumbing Services', 'contact@abcplumbing.com', '+84901234567', 'ACTIVE', TRUE, 'ABC Plumbing Co.'),
('XYZ Electrical', 'info@xyzelectrical.com', '+84902345678', 'ACTIVE', TRUE, 'XYZ Electric Ltd.');

-- Sample services
INSERT INTO marketplace_services (name, description, category, provider_id, base_price, price_unit, status) VALUES
('Emergency Plumbing Repair', 'Fast response for urgent plumbing issues', 'PLUMBING', 1, 150.00, 'PER_JOB', 'ACTIVE'),
('Electrical Wiring Installation', 'Professional electrical wiring services', 'ELECTRICAL', 2, 200.00, 'PER_JOB', 'ACTIVE'),
('AC Cleaning Service', 'Deep cleaning for air conditioning units', 'HVAC', 1, 80.00, 'PER_JOB', 'ACTIVE');

-- ================================================================
-- End of Migration V1
-- ================================================================
