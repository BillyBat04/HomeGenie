import { AlertCircle } from 'lucide-react';
import React from 'react';
import RequestCard from './RequestCard.jsx';

const RequestsList = ({ requests, loading, currentUser, onAssign, onUpdateStatus, setView, technicians = [] }) => {
    if (loading) {
        return (
            <div className="text-center py-12">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mx-auto"></div>
                <p className="text-gray-600 mt-4">Loading requests...</p>
            </div>
        );
    }

    if (requests.length === 0) {
        return (
            <div className="bg-white rounded-xl shadow-sm p-12 text-center">
                <AlertCircle className="w-16 h-16 text-gray-400 mx-auto mb-4" />
                <p className="text-gray-600 text-lg">No maintenance requests found</p>
                {currentUser.role === 'RESIDENT' && (
                    <button
                        onClick={() => setView('create')}
                        className="mt-4 bg-indigo-600 text-white px-6 py-2 rounded-lg hover:bg-indigo-700 transition"
                    >
                        Create Your First Request
                    </button>
                )}
            </div>
        );
    }

    return (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {requests.map(req => (
                <RequestCard
                    key={req.id}
                    request={req}
                    currentUser={currentUser}
                    onAssign={onAssign}
                    onUpdateStatus={onUpdateStatus}
                    technicians={technicians}
                />
            ))}
        </div>
    );
};

export default RequestsList;