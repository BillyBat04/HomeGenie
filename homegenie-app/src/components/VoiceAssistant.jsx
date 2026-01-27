import { CheckCircle, Loader, Mic, MicOff, Send, Volume2, VolumeX, XCircle } from 'lucide-react';
import React, { useRef, useState } from 'react';

const VoiceAssistant = ({ currentUser, onRequestCreated }) => {
    const [isRecording, setIsRecording] = useState(false);
    const [isProcessing, setIsProcessing] = useState(false);
    const [transcribedText, setTranscribedText] = useState('');
    const [response, setResponse] = useState(null);
    const [error, setError] = useState(null);
    const [useTextInput, setUseTextInput] = useState(false);
    const [textInput, setTextInput] = useState('');
    const [isPlayingAudio, setIsPlayingAudio] = useState(false);

    const mediaRecorderRef = useRef(null);
    const audioChunksRef = useRef([]);
    const audioRef = useRef(null);

    if (!currentUser) {
        return (
            <div className="bg-red-50 rounded-xl shadow-lg p-6 mb-8 border border-red-200">
                <div className="flex items-center gap-2">
                    <XCircle className="w-5 h-5 text-red-600" />
                    <p className="text-red-800 font-medium">User information is missing. Please log in again.</p>
                </div>
            </div>
        );
    }

    const startRecording = async () => {
        try {
            const stream = await navigator.mediaDevices.getUserMedia({
                audio: {
                    echoCancellation: true,
                    noiseSuppression: true,
                    sampleRate: 16000
                }
            });

            const mimeType = MediaRecorder.isTypeSupported('audio/webm;codecs=opus')
                ? 'audio/webm;codecs=opus'
                : 'audio/webm';

            mediaRecorderRef.current = new MediaRecorder(stream, { mimeType });
            audioChunksRef.current = [];

            mediaRecorderRef.current.ondataavailable = (event) => {
                if (event.data.size > 0) {
                    audioChunksRef.current.push(event.data);
                }
            };

            mediaRecorderRef.current.onstop = async () => {
                const audioBlob = new Blob(audioChunksRef.current, { type: mimeType });
                console.log('Audio recorded:', audioBlob.size, 'bytes');
                await sendAudioToBackend(audioBlob);
                stream.getTracks().forEach(track => track.stop());
            };

            mediaRecorderRef.current.start(100);
            setIsRecording(true);
            setError(null);
            setResponse(null);
        } catch (err) {
            setError('Microphone access denied. Please enable microphone permissions.');
            console.error('Error accessing microphone:', err);
        }
    };

    const stopRecording = () => {
        if (mediaRecorderRef.current && isRecording) {
            mediaRecorderRef.current.stop();
            setIsRecording(false);
            setIsProcessing(true);
        }
    };

    const sendAudioToBackend = async (audioBlob) => {
        // Validate currentUser - check both 'id' and 'userId'
        const userId = currentUser?.id || currentUser?.userId;
        if (!userId) {
            setError('User information is missing. Please refresh the page.');
            setIsProcessing(false);
            return;
        }

        const formData = new FormData();
        formData.append('audio', audioBlob, 'recording.wav');

        try {
            const res = await fetch('http://localhost:8082/api/maintenance/voice/interact', {
                method: 'POST',
                headers: {
                    'X-User-Id': userId.toString()
                },
                body: formData
            });

            if (!res.ok) throw new Error('Failed to process audio');

            const data = await res.json();
            handleVoiceResponse(data);
        } catch (err) {
            setError('Failed to process voice input. Please try again.');
            console.error('Error sending audio:', err);
        } finally {
            setIsProcessing(false);
        }
    };

    // Send text to backend
    const sendTextToBackend = async () => {
        if (!textInput.trim()) return;

        const userId = currentUser?.id || currentUser?.userId;
        if (!userId) {
            setError('User information is missing. Please refresh the page.');
            return;
        }

        setIsProcessing(true);
        setError(null);
        setResponse(null);

        try {
            const res = await fetch('http://localhost:8082/api/maintenance/voice/interact-text', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-User-Id': userId.toString()
                },
                body: JSON.stringify({
                    transcribedText: textInput,
                    conversationId: response?.conversationId || null
                })
            });

            if (!res.ok) throw new Error('Failed to process text');

            const data = await res.json();
            handleVoiceResponse(data);
            setTextInput('');
        } catch (err) {
            setError('Failed to process your request. Please try again.');
            console.error('Error sending text:', err);
        } finally {
            setIsProcessing(false);
        }
    };

    // Handle voice response
    const handleVoiceResponse = (data) => {
        setTranscribedText(data.transcribedText || textInput);
        setResponse(data);

        // Notify parent component if a ticket was created
        if (data.createdTicket && onRequestCreated) {
            onRequestCreated(data.createdTicket);
        }

        // Auto-play audio response if available
        if (data.audioResponseBase64) {
            playAudioResponse(data.audioResponseBase64);
        }
    };

    // Play audio response
    const playAudioResponse = (base64Audio) => {
        try {
            const audio = new Audio(`data:audio/mp3;base64,${base64Audio}`);
            audioRef.current = audio;
            audio.onplay = () => setIsPlayingAudio(true);
            audio.onended = () => setIsPlayingAudio(false);
            audio.onerror = () => {
                console.error('Error playing audio');
                setIsPlayingAudio(false);
            };
            audio.play().catch(err => {
                console.error('Failed to play audio:', err);
                setIsPlayingAudio(false);
            });
        } catch (err) {
            console.error('Error creating audio:', err);
        }
    };

    // Stop audio playback
    const stopAudio = () => {
        if (audioRef.current) {
            audioRef.current.pause();
            audioRef.current.currentTime = 0;
            setIsPlayingAudio(false);
        }
    };

    // Handle example click
    const handleExampleClick = (example) => {
        setUseTextInput(true);
        setTextInput(example);
    };

    return (
        <div className="bg-gradient-to-br from-blue-50 to-indigo-50 rounded-xl shadow-lg p-6 mb-8">
            <div className="flex items-center justify-between mb-4">
                <h3 className="text-xl font-bold text-gray-800 flex items-center gap-2">
                    <Mic className="w-6 h-6 text-blue-600" />
                    Voice Assistant
                </h3>
                <button
                    onClick={() => setUseTextInput(!useTextInput)}
                    className="text-sm text-blue-600 hover:text-blue-800 font-medium transition-colors"
                >
                    {useTextInput ? '🎤 Use Microphone' : '⌨️ Type Instead'}
                </button>
            </div>

            {/* Voice/Text Input Area */}
            <div className="space-y-4">
                {!useTextInput ? (
                    // Voice Recording
                    <div className="flex flex-col items-center gap-4">
                        <button
                            onClick={isRecording ? stopRecording : startRecording}
                            disabled={isProcessing}
                            className={`relative w-20 h-20 rounded-full flex items-center justify-center transition-all transform hover:scale-105 disabled:opacity-50 disabled:cursor-not-allowed shadow-lg ${isRecording
                                ? 'bg-red-500 hover:bg-red-600 animate-pulse'
                                : 'bg-blue-500 hover:bg-blue-600'
                                }`}
                        >
                            {isRecording ? (
                                <MicOff className="w-10 h-10 text-white" />
                            ) : (
                                <Mic className="w-10 h-10 text-white" />
                            )}
                        </button>
                        {isRecording && (
                            <span className="text-sm font-medium text-red-600 animate-pulse">
                                🔴 Recording... Tap to stop
                            </span>
                        )}
                        {!isRecording && !isProcessing && (
                            <p className="text-sm text-gray-600 text-center">
                                Tap the microphone to start recording your maintenance request
                            </p>
                        )}
                    </div>
                ) : (
                    // Text Input
                    <div className="flex gap-2">
                        <input
                            type="text"
                            value={textInput}
                            onChange={(e) => setTextInput(e.target.value)}
                            onKeyPress={(e) => e.key === 'Enter' && sendTextToBackend()}
                            placeholder="Type your maintenance request here..."
                            className="flex-1 px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                            disabled={isProcessing}
                        />
                        <button
                            onClick={sendTextToBackend}
                            disabled={isProcessing || !textInput.trim()}
                            className="px-6 py-3 bg-blue-500 text-white rounded-lg hover:bg-blue-600 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2 transition-colors shadow-md hover:shadow-lg"
                        >
                            <Send className="w-5 h-5" />
                            Send
                        </button>
                    </div>
                )}

                {/* Processing Indicator */}
                {isProcessing && (
                    <div className="flex items-center justify-center gap-2 text-blue-600 py-2">
                        <Loader className="w-5 h-5 animate-spin" />
                        <span className="font-medium">Processing your request...</span>
                    </div>
                )}

                {/* Transcribed Text */}
                {transcribedText && (
                    <div className="bg-white rounded-lg p-4 border border-gray-200 shadow-sm">
                        <p className="text-sm font-semibold text-gray-700 mb-1">You said:</p>
                        <p className="text-gray-900">{transcribedText}</p>
                    </div>
                )}

                {/* Response */}
                {response && (
                    <div className="bg-white rounded-lg p-4 border border-gray-200 shadow-sm space-y-3">
                        <div className="flex items-start justify-between gap-4">
                            <div className="flex-1">
                                <p className="text-sm font-semibold text-gray-700 mb-1">Assistant:</p>
                                <p className="text-gray-900">{response.textResponse}</p>
                            </div>
                            {response.audioResponseBase64 && (
                                <button
                                    onClick={isPlayingAudio ? stopAudio : () => playAudioResponse(response.audioResponseBase64)}
                                    className="flex-shrink-0 p-2 rounded-lg hover:bg-gray-100 transition-colors"
                                    title={isPlayingAudio ? "Stop audio" : "Play audio"}
                                >
                                    {isPlayingAudio ? (
                                        <VolumeX className="w-5 h-5 text-red-500" />
                                    ) : (
                                        <Volume2 className="w-5 h-5 text-blue-500" />
                                    )}
                                </button>
                            )}
                        </div>

                        {/* Created Ticket Info */}
                        {response.createdTicket && (
                            <div className="bg-green-50 rounded-lg p-4 border border-green-200">
                                <div className="flex items-center gap-2 mb-3">
                                    <CheckCircle className="w-5 h-5 text-green-600" />
                                    <span className="font-semibold text-green-800">Request Created Successfully!</span>
                                </div>
                                <div className="grid grid-cols-2 gap-3 text-sm">
                                    <div className="flex flex-col">
                                        <span className="text-gray-600 text-xs">Ticket Number</span>
                                        <span className="font-semibold text-gray-900">#{response.createdTicket.id}</span>
                                    </div>
                                    <div className="flex flex-col">
                                        <span className="text-gray-600 text-xs">Category</span>
                                        <span className="font-semibold text-gray-900">{response.createdTicket.category}</span>
                                    </div>
                                    <div className="flex flex-col">
                                        <span className="text-gray-600 text-xs">Priority</span>
                                        <span className={`font-semibold ${response.createdTicket.priority === 'CRITICAL' ? 'text-red-600' :
                                            response.createdTicket.priority === 'HIGH' ? 'text-orange-600' :
                                                response.createdTicket.priority === 'MEDIUM' ? 'text-yellow-600' :
                                                    'text-green-600'
                                            }`}>
                                            {response.createdTicket.priority}
                                        </span>
                                    </div>
                                    <div className="flex flex-col">
                                        <span className="text-gray-600 text-xs">Status</span>
                                        <span className="font-semibold text-blue-600">{response.createdTicket.status}</span>
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>
                )}

                {/* Error */}
                {error && (
                    <div className="bg-red-50 rounded-lg p-4 border border-red-200 shadow-sm">
                        <div className="flex items-center gap-2">
                            <XCircle className="w-5 h-5 text-red-600" />
                            <p className="text-red-800 font-medium">{error}</p>
                        </div>
                    </div>
                )}
            </div>

            {/* Examples */}
            <div className="mt-6 pt-4 border-t border-gray-200">
                <p className="text-xs font-semibold text-gray-700 mb-3">💡 Try these examples:</p>
                <div className="flex flex-wrap gap-2">
                    {[
                        "My AC is not working",
                        "There's a water leak in the bathroom",
                        "The lights in the hallway are out",
                        "Emergency! Gas smell in kitchen"
                    ].map((example, idx) => (
                        <button
                            key={idx}
                            onClick={() => handleExampleClick(example)}
                            className="px-3 py-2 bg-blue-100 text-blue-700 rounded-lg text-xs hover:bg-blue-200 transition-colors font-medium"
                        >
                            "{example}"
                        </button>
                    ))}
                </div>
            </div>
        </div>
    );
};

export default VoiceAssistant;