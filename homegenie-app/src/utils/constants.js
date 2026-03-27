// constants.js
// All API calls are routed through the API Gateway (port 8080).
// The gateway handles service discovery, JWT validation, and rate limiting.
export const API_GATEWAY_URL = import.meta.env.VITE_GATEWAY_URL || 'http://localhost:8080';
export const API_BASE_USER = `${API_GATEWAY_URL}/api`;
export const API_BASE_MAINTENANCE = `${API_GATEWAY_URL}/api`;

export const PRIORITY_COLORS = {
    CRITICAL: 'bg-red-100 text-red-800 border-red-300',
    HIGH: 'bg-orange-100 text-orange-800 border-orange-300',
    MODERATE: 'bg-yellow-100 text-yellow-800 border-yellow-300',
    LOW: 'bg-green-100 text-green-800 border-green-300'
};

export const CATEGORY_ICONS = {
    PLUMBING: '🚰',
    ELECTRICAL: '⚡',
    CLEANING: '🧹',
    SECURITY: '🔒',
    CARPENTRY: '🔨',
    PAINTING: '🎨',
    HVAC: '❄️',
    OTHERS: '📦'
};