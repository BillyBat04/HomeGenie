import { Loader2 } from 'lucide-react';
import React, { useEffect, useState } from 'react';

const AssignmentModal = ({ request, technicians, onAssign, onClose }) => {
    const [loading, setLoading] = useState(false);
    const [selectedTechId, setSelectedTechId] = useState(null);

    // Debug logging
    useEffect(() => {
        console.log('🔧 Technicians data:', technicians);
        if (technicians.length > 0) {
            console.log('📋 First technician structure:', technicians[0]);
        }
    }, [technicians]);

    if (!request) return null;

    const handleAssign = async (techId) => {
        console.log('🎯 Assigning technician:', {
            requestId: request.id,
            technicianId: techId,
            technicianIdType: typeof techId,
            technicianIdValue: techId
        });

        setLoading(true);
        setSelectedTechId(techId);

        try {
            await onAssign(request.id, techId);
            console.log('✅ Assignment successful');
        } catch (error) {
            console.error('❌ Assignment failed:', error);
        } finally {
            setLoading(false);
            setSelectedTechId(null);
        }
    };

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-xl shadow-2xl max-w-md w-full p-6">
                <h3 className="text-xl font-bold text-gray-800 mb-4">Assign Technician</h3>

                <div className="mb-4 p-4 bg-gray-50 rounded-lg">
                    <p className="text-sm text-gray-600">Request #{request.id}</p>
                    <p className="font-semibold text-gray-800">{request.title}</p>
                    <p className="text-sm text-gray-600 mt-1">Category: {request.category}</p>
                    <p className="text-sm text-gray-600">Priority: {request.priority}</p>
                </div>

                <div className="space-y-2 mb-6 max-h-64 overflow-y-auto">
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                        Select Technician (Email notification will be sent)
                    </label>

                    {technicians.length === 0 ? (
                        <div className="text-center py-8">
                            <p className="text-gray-500 text-sm">No technicians available</p>
                            <p className="text-gray-400 text-xs mt-1">Please add technicians to the system</p>
                        </div>
                    ) : (
                        technicians.map(tech => {
                            // Try multiple possible ID fields
                            const techId = tech.userId || tech.id;

                            return (
                                <button
                                    key={techId}
                                    onClick={() => handleAssign(techId)}
                                    disabled={loading}
                                    className={`w-full text-left px-4 py-3 border rounded-lg transition relative ${loading
                                            ? 'opacity-50 cursor-not-allowed'
                                            : 'hover:bg-indigo-50 hover:border-indigo-500'
                                        } ${selectedTechId === techId && loading
                                            ? 'bg-indigo-50 border-indigo-500'
                                            : ''
                                        }`}
                                >
                                    <div className="flex items-center justify-between">
                                        <div>
                                            <p className="font-medium text-gray-800">{tech.fullName}</p>
                                            <p className="text-xs text-gray-500">{tech.email}</p>
                                            {tech.phoneNumber && (
                                                <p className="text-xs text-gray-400">📞 {tech.phoneNumber}</p>
                                            )}
                                            {/* Debug info - remove in production */}
                                            <p className="text-xs text-red-400">ID: {techId}</p>
                                        </div>
                                        {selectedTechId === techId && loading && (
                                            <Loader2 className="w-5 h-5 text-indigo-600 animate-spin" />
                                        )}
                                    </div>
                                </button>
                            );
                        })
                    )}
                </div>

                {loading && (
                    <div className="mb-4 p-3 bg-blue-50 border border-blue-200 rounded-lg">
                        <div className="flex items-center gap-2">
                            <Loader2 className="w-4 h-4 text-blue-600 animate-spin" />
                            <p className="text-sm text-blue-800">Assigning technician and sending notification...</p>
                        </div>
                    </div>
                )}

                <button
                    onClick={onClose}
                    disabled={loading}
                    className={`w-full bg-gray-200 text-gray-700 py-2 rounded-lg transition ${loading
                            ? 'opacity-50 cursor-not-allowed'
                            : 'hover:bg-gray-300'
                        }`}
                >
                    Cancel
                </button>
            </div>
        </div>
    );
};

export default AssignmentModal;