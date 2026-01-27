import { Camera, TrendingUp } from 'lucide-react';
import React from 'react';
import { toast } from 'react-toastify';

const CreateRequestForm = ({ newRequest, setNewRequest, onSubmit, onCancel, loading }) => {
    const handleImageUpload = (e) => {
        const file = e.target.files[0];
        if (file) {
            if (file.size > 5 * 1024 * 1024) {
                toast.error('Image size should be less than 5MB');
                return;
            }
            const reader = new FileReader();
            reader.onloadend = () => {
                setNewRequest(prev => ({ ...prev, imageBase64: reader.result }));
            };
            reader.readAsDataURL(file);
        }
    };

    return (
        <div className="max-w-2xl mx-auto px-4 py-8">
            <div className="bg-white rounded-xl shadow-lg p-8">
                <h2 className="text-2xl font-bold text-gray-800 mb-6">Create Maintenance Request</h2>

                <form onSubmit={onSubmit} className="space-y-6">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">Issue Title</label>
                        <input
                            type="text"
                            placeholder="e.g., Water leakage in kitchen sink"
                            className="w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                            value={newRequest.title}
                            onChange={(e) => setNewRequest({ ...newRequest, title: e.target.value })}
                            required
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">Description</label>
                        <textarea
                            placeholder="Describe the issue in detail..."
                            rows="4"
                            className="w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                            value={newRequest.description}
                            onChange={(e) => setNewRequest({ ...newRequest, description: e.target.value })}
                            required
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">Upload Photo (Optional, Max 5MB)</label>
                        <div className="border-2 border-dashed border-gray-300 rounded-lg p-6 text-center hover:border-indigo-500 transition">
                            {newRequest.imageBase64 ? (
                                <div className="relative">
                                    <img
                                        src={newRequest.imageBase64}
                                        alt="Preview"
                                        className="max-h-48 mx-auto rounded-lg"
                                    />
                                    <button
                                        type="button"
                                        onClick={() => setNewRequest({ ...newRequest, imageBase64: '' })}
                                        className="mt-4 text-red-600 text-sm hover:text-red-700 font-medium"
                                    >
                                        Remove Image
                                    </button>
                                </div>
                            ) : (
                                <label className="cursor-pointer">
                                    <Camera className="w-12 h-12 text-gray-400 mx-auto mb-2" />
                                    <p className="text-gray-600">Click to upload photo</p>
                                    <p className="text-xs text-gray-500 mt-1">JPG, PNG up to 5MB</p>
                                    <input
                                        type="file"
                                        accept="image/*"
                                        className="hidden"
                                        onChange={handleImageUpload}
                                    />
                                </label>
                            )}
                        </div>
                    </div>

                    <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                        <div className="flex items-start gap-3">
                            <TrendingUp className="w-5 h-5 text-blue-600 mt-0.5" />
                            <div>
                                <p className="text-sm font-medium text-blue-900">AI-Powered Classification & Notifications</p>
                                <p className="text-xs text-blue-700 mt-1">
                                    • AI will categorize and prioritize your request<br />
                                    • Admin will receive instant email notification<br />
                                    • You'll get updates via email at each step
                                </p>
                            </div>
                        </div>
                    </div>

                    <div className="flex gap-4">
                        <button
                            type="submit"
                            disabled={loading}
                            className="flex-1 bg-indigo-600 text-white py-3 rounded-lg hover:bg-indigo-700 transition font-medium disabled:opacity-50"
                        >
                            {loading ? 'Submitting...' : 'Submit Request'}
                        </button>
                        <button
                            type="button"
                            onClick={onCancel}
                            className="px-6 bg-gray-200 text-gray-700 py-3 rounded-lg hover:bg-gray-300 transition"
                        >
                            Cancel
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default CreateRequestForm;