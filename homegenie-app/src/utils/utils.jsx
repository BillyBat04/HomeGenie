// utils.jsx
import { AlertCircle, CheckCircle, Clock } from 'lucide-react';
import React from 'react';
import { CATEGORY_ICONS, PRIORITY_COLORS } from './constants.js';

export const getPriorityColor = (priority) => {
    return PRIORITY_COLORS[priority] || 'bg-gray-100 text-gray-800';
};

export const getStatusIcon = (status) => {
    if (status === 'COMPLETED') return <CheckCircle className="w-5 h-5 text-green-600" />;
    if (status === 'IN_PROGRESS') return <Clock className="w-5 h-5 text-blue-600" />;
    return <AlertCircle className="w-5 h-5 text-orange-600" />;
};

export const getCategoryIcon = (category) => {
    return CATEGORY_ICONS[category] || '📦';
};