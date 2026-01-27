import { Users } from 'lucide-react';
import React, { useState } from 'react';
import { getCategoryIcon, getPriorityColor, getStatusIcon } from '../../src/utils/utils';

const RequestCard = ({ request, currentUser, onAssign, onUpdateStatus, technicians = [] }) => {
    const [isCompleting, setIsCompleting] = useState(false);

    const handleComplete = async () => {
        setIsCompleting(true);
        try {
            await onUpdateStatus(request.id, 'COMPLETED');
        } catch (error) {
            console.error('Failed to complete request:', error);
        } finally {
            setIsCompleting(false);
        }
    };

    // Find the assigned technician details
    const assignedTechnician = technicians.find(tech => tech.userId === request.assignedTo);

    return (
        <div className="bg-white rounded-xl shadow-sm hover:shadow-md transition p-6">
            <div className="flex justify-between items-start mb-4">
                <div className="flex items-center gap-2">
                    {getStatusIcon(request.status)}
                    <span className="text-sm font-medium text-gray-700">
                        {request.status.replace('_', ' ')}
                    </span>
                </div>
                <span className={`px-3 py-1 rounded-full text-xs font-semibold border ${getPriorityColor(request.priority)}`}>
                    {request.priority}
                </span>
            </div>

            <div className="flex items-center gap-2 mb-2">
                <span className="text-2xl">{getCategoryIcon(request.category)}</span>
                <h3 className="text-lg font-bold text-gray-800">{request.title}</h3>
            </div>
            <p className="text-sm text-gray-600 mb-4 line-clamp-2">{request.description}</p>

            {request.imageUrl && (
                <img
                    src={request.imageUrl}
                    alt="Issue"
                    className="w-full h-32 object-cover rounded-lg mb-4"
                    onError={(e) => e.target.style.display = 'none'}
                />
            )}

            <div className="flex items-center justify-between text-xs text-gray-500 mb-4">
                <span className="bg-gray-100 px-2 py-1 rounded">{request.category}</span>
                <span>{new Date(request.createdAt).toLocaleDateString()}</span>
            </div>

            {request.assignedTo && (
                <div className="mb-4 p-3 bg-blue-50 rounded-lg">
                    <div className="flex items-start gap-2">
                        <Users className="w-4 h-4 text-blue-600 mt-0.5" />
                        <div className="flex-1">
                            <p className="text-xs font-medium text-blue-900">Assigned Technician</p>
                            {assignedTechnician ? (
                                <>
                                    <p className="text-sm font-semibold text-blue-800">{assignedTechnician.fullName}</p>
                                    <p className="text-xs text-blue-600">{assignedTechnician.email}</p>
                                    {assignedTechnician.phoneNumber && (
                                        <p className="text-xs text-blue-600">📞 {assignedTechnician.phoneNumber}</p>
                                    )}
                                </>
                            ) : (
                                <p className="text-xs text-blue-700">Technician ID: #{request.assignedTo}</p>
                            )}
                        </div>
                    </div>
                </div>
            )}

            {/* Loading indicator when completing */}
            {isCompleting && (
                <div className="mb-4 p-3 bg-green-50 border border-green-200 rounded-lg">
                    <div className="flex items-center gap-2">
                        <Loader2 className="w-4 h-4 text-green-600 animate-spin" />
                        <p className="text-sm text-green-800">Marking as complete and notifying resident...</p>
                    </div>
                </div>
            )}

            {currentUser.role === 'ADMIN' && request.status !== 'COMPLETED' && (
                <div className="flex gap-2">
                    {request.status === 'PENDING' && !request.assignedTo && (
                        <button
                            onClick={() => onAssign(request)}
                            disabled={isCompleting}
                            className={`flex-1 bg-indigo-100 text-indigo-700 py-2 rounded-lg text-sm font-medium transition ${isCompleting
                                ? 'opacity-50 cursor-not-allowed'
                                : 'hover:bg-indigo-200'
                                }`}
                        >
                            Assign Technician
                        </button>
                    )}
                    {request.assignedTo && request.status === 'IN_PROGRESS' && (
                        <button
                            onClick={handleComplete}
                            disabled={isCompleting}
                            className={`flex-1 bg-green-100 text-green-700 py-2 rounded-lg text-sm font-medium transition flex items-center justify-center gap-2 ${isCompleting
                                ? 'opacity-50 cursor-not-allowed'
                                : 'hover:bg-green-200'
                                }`}
                        >
                            {isCompleting ? (
                                <>
                                    <Loader2 className="w-4 h-4 animate-spin" />
                                    <span>Completing...</span>
                                </>
                            ) : (
                                'Mark Complete'
                            )}
                        </button>
                    )}
                </div>
            )}
        </div>
    );
};

export default RequestCard;