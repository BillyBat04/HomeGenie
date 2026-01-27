-- =====================================================
-- Marketplace Service - Test Data Seeding (Schema-Aligned)
-- Purpose: Seed database with test provider and services for E2E testing
-- Version: V999 (High version to run after all schema migrations)
-- =====================================================

-- Clean up existing test data (idempotent)
DELETE FROM marketplace_reviews WHERE booking_id IN (
    SELECT id FROM marketplace_bookings WHERE service_id IN (
        SELECT id FROM marketplace_services WHERE provider_id IN (
            SELECT id FROM marketplace_providers WHERE email LIKE '%e2e-test%'
        )
    )
);

DELETE FROM marketplace_bookings WHERE service_id IN (
    SELECT id FROM marketplace_services WHERE provider_id IN (
        SELECT id FROM marketplace_providers WHERE email LIKE '%e2e-test%'
    )
);

DELETE FROM marketplace_services WHERE provider_id IN (
    SELECT id FROM marketplace_providers WHERE email LIKE '%e2e-test%'
);

DELETE FROM marketplace_providers WHERE email LIKE '%e2e-test%';

-- =====================================================
-- Test Provider: Plumbing Company
-- =====================================================
INSERT INTO marketplace_providers (
    name, email, phone, company_name, license_number, bio,
    status, is_verified, total_bookings, completed_bookings,
    average_rating, total_reviews, service_areas_json, working_hours_json,
    created_at, updated_at
) VALUES (
    'E2E Test Plumbing Co.',
    'e2e-test-plumber@homegenie.com',
    '+84901234567',
    'E2E Test Plumbing Company Ltd.',
    'LICENSE-PLUMBING-2026-001',
    'Professional plumbing services with 10+ years experience. Available for emergencies 24/7.',
    'ACTIVE',
    TRUE,
    50,
    45,
    4.5,
    12,
    '["District 1", "District 2", "District 3"]',
    '{"monday": "08:00-18:00", "tuesday": "08:00-18:00", "wednesday": "08:00-18:00", "thursday": "08:00-18:00", "friday": "08:00-18:00", "saturday": "08:00-18:00"}',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- =====================================================
-- Test Services for Provider
-- =====================================================
INSERT INTO marketplace_services (
    provider_id, name, category, description, base_price, price_unit,
    currency, status, duration_minutes, image_url, created_at, updated_at
) VALUES
-- Service 1: Emergency Plumbing
(
    (SELECT id FROM marketplace_providers WHERE email = 'e2e-test-plumber@homegenie.com'),
    'Emergency Plumbing Repair',
    'PLUMBING',
    '24/7 emergency plumbing repair service. Fast response within 1 hour. Covers pipe leaks, clogs, and urgent repairs.',
    500000,
    'PER_HOUR',
    'VND',
    'ACTIVE',
    120,
    'https://example.com/images/plumbing-emergency.jpg',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
-- Service 2: Water Heater
(
    (SELECT id FROM marketplace_providers WHERE email = 'e2e-test-plumber@homegenie.com'),
    'Water Heater Installation',
    'PLUMBING',
    'Professional water heater installation service. Includes removal of old unit and testing.',
    1500000,
    'PER_JOB',
    'VND',
    'ACTIVE',
    240,
    'https://example.com/images/water-heater.jpg',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
