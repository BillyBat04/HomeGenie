import React from 'react';
import RequestsList from './RequestsList.jsx';
import StatisticsCards from './StatisticsCards.jsx';
import VoiceAssistant from './VoiceAssistant.jsx';

const Dashboard = ({
    currentUser,
    stats,
    requests,
    loading,
    onAssign,
    onUpdateStatus,
    setView,
    technicians = [],
    onRefreshRequests
}) => {

    const handleRequestCreated = (newRequest) => {
        console.log('New request created via voice:', newRequest);

        if (onRefreshRequests) {
            onRefreshRequests();
        }
    };

    // Debug: Check if currentUser is passed correctly
    if (!currentUser) {
        return (
            <div className="max-w-7xl mx-auto px-4 py-8">
                <div className="bg-red-50 rounded-lg p-6 border border-red-200">
                    <p className="text-red-800 font-medium">
                        Error: User information is missing
                    </p>
                </div>
            </div>
        );
    }

    const Residentrole = currentUser.role === 'RESIDENT';

    return (
        <div className="max-w-7xl mx-auto px-4 py-8">
            {/* Voice Assistant - Only for RESIDENT */}
            {Residentrole && (
                <VoiceAssistant
                    currentUser={currentUser}
                    onRequestCreated={handleRequestCreated}
                />
            )}

            {/* Statistics (Admin Only) */}
            {currentUser.role === 'ADMIN' && stats.total !== undefined && (
                <StatisticsCards stats={stats} />
            )}

            {/* Requests List */}
            <h2 className="text-2xl font-bold text-gray-800 mb-6">
                {currentUser.role === 'ADMIN'
                    ? 'All Maintenance Requests'
                    : 'My Requests'}
            </h2>

            <RequestsList
                requests={requests}
                loading={loading}
                currentUser={currentUser}
                onAssign={onAssign}
                onUpdateStatus={onUpdateStatus}
                setView={setView}
                technicians={technicians}
            />
        </div>
    );
};

export default Dashboard;
