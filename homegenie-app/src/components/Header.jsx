import { Bell, Home, LogOut } from 'lucide-react';
import React from 'react';

const Header = ({ currentUser, onLogout, setView }) => {
    return (
        <header className="bg-white shadow-sm sticky top-0 z-40">
            <div className="max-w-7xl mx-auto px-4 py-4 flex justify-between items-center">
                <div className="flex items-center gap-3">
                    <Home className="w-8 h-8 text-indigo-600" />
                    <div>
                        <h1 className="text-xl font-bold text-gray-800">HomeGenie</h1>
                        <p className="text-sm text-gray-600">{currentUser.fullName} ({currentUser.role})</p>
                    </div>
                </div>
                <div className="flex gap-3">
                    {currentUser.role === 'RESIDENT' && (
                        <button
                            onClick={() => setView('create')}
                            className="bg-indigo-600 text-white px-4 py-2 rounded-lg hover:bg-indigo-700 transition flex items-center gap-2"
                        >
                            <Bell className="w-4 h-4" />
                            New Request
                        </button>
                    )}
                    <button
                        onClick={() => setView('dashboard')}
                        className="bg-gray-200 text-gray-700 px-4 py-2 rounded-lg hover:bg-gray-300 transition"
                    >
                        Dashboard
                    </button>
                    <button
                        onClick={onLogout}
                        className="flex items-center gap-2 text-red-600 hover:text-red-700 px-3 py-2 rounded-lg hover:bg-red-50"
                    >
                        <LogOut className="w-5 h-5" />
                    </button>
                </div>
            </div>
        </header>
    );
};

export default Header;