import { Home } from 'lucide-react';
import React from 'react';

const AuthForm = ({ view, setView, authForm, setAuthForm, onLogin, onRegister, loading }) => {
    return (
        <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center p-4">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md p-8">
                <div className="text-center mb-8">
                    <Home className="w-16 h-16 text-indigo-600 mx-auto mb-4" />
                    <h1 className="text-3xl font-bold text-gray-800">HomeGenie</h1>
                    <p className="text-gray-600 mt-2">Smart Maintenance Management</p>
                </div>

                {view === 'login' ? (
                    <form onSubmit={onLogin} className="space-y-4">
                        <input
                            type="email"
                            placeholder="Email"
                            className="w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                            value={authForm.email}
                            onChange={(e) => setAuthForm({ ...authForm, email: e.target.value })}
                            required
                        />
                        <input
                            type="password"
                            placeholder="Password"
                            className="w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                            value={authForm.password}
                            onChange={(e) => setAuthForm({ ...authForm, password: e.target.value })}
                            required
                        />
                        <button
                            type="submit"
                            disabled={loading}
                            className="w-full bg-indigo-600 text-white py-3 rounded-lg hover:bg-indigo-700 transition disabled:opacity-50"
                        >
                            {loading ? 'Logging in...' : 'Login'}
                        </button>
                        <p className="text-center text-sm text-gray-600">
                            Don't have an account?{' '}
                            <button
                                type="button"
                                onClick={() => setView('register')}
                                className="text-indigo-600 font-semibold hover:underline"
                            >
                                Register
                            </button>
                        </p>
                    </form>
                ) : (
                    <form onSubmit={onRegister} className="space-y-4">
                        <input
                            type="email"
                            placeholder="Email"
                            className="w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                            value={authForm.email}
                            onChange={(e) => setAuthForm({ ...authForm, email: e.target.value })}
                            required
                        />
                        <input
                            type="password"
                            placeholder="Password (min 6 chars)"
                            className="w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                            value={authForm.password}
                            onChange={(e) => setAuthForm({ ...authForm, password: e.target.value })}
                            required
                        />
                        <input
                            type="text"
                            placeholder="Full Name"
                            className="w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                            value={authForm.fullName}
                            onChange={(e) => setAuthForm({ ...authForm, fullName: e.target.value })}
                            required
                        />
                        <input
                            type="tel"
                            placeholder="Phone Number"
                            className="w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                            value={authForm.phoneNumber}
                            onChange={(e) => setAuthForm({ ...authForm, phoneNumber: e.target.value })}
                            required
                        />
                        <input
                            type="text"
                            placeholder="Flat Number (optional)"
                            className="w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                            value={authForm.flatNumber}
                            onChange={(e) => setAuthForm({ ...authForm, flatNumber: e.target.value })}
                        />
                        <button
                            type="submit"
                            disabled={loading}
                            className="w-full bg-indigo-600 text-white py-3 rounded-lg hover:bg-indigo-700 transition disabled:opacity-50"
                        >
                            {loading ? 'Creating Account...' : 'Register'}
                        </button>
                        <p className="text-center text-sm text-gray-600">
                            Already have an account?{' '}
                            <button
                                type="button"
                                onClick={() => setView('login')}
                                className="text-indigo-600 font-semibold hover:underline"
                            >
                                Login
                            </button>
                        </p>
                    </form>
                )}
            </div>
        </div>
    );
};

export default AuthForm;