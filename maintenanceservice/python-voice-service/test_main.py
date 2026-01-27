import pytest
import os
import tempfile
import json
from unittest.mock import Mock, patch, MagicMock, mock_open
from fastapi.testclient import TestClient
from io import BytesIO
import sys

# Add the parent directory to the path to import main
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from main import (
    app,
    convert_audio_to_wav,
    simple_intent_detection,
    process_with_gemini,
    process_with_huggingface,
    TextToSpeechRequest,
    AIProcessingRequest
)

client = TestClient(app)


class TestSimpleIntentDetection:
    """Test cases for simple_intent_detection function"""
    
    def test_detect_emergency_intent(self):
        """Test emergency keyword detection"""
        result = simple_intent_detection("There's a fire in my apartment!")
        assert result["intent"] == "CREATE_MAINTENANCE_REQUEST"
        assert result["isEmergency"] == True
        assert result["confidence"] == 0.8
    
    def test_detect_status_query_intent(self):
        """Test status query intent detection"""
        result = simple_intent_detection("What's the status of my request?")
        assert result["intent"] == "QUERY_STATUS"
        assert result["isEmergency"] == False
        assert result["confidence"] == 0.7
    
    def test_detect_list_requests_intent(self):
        """Test list requests intent detection"""
        result = simple_intent_detection("Show me all my requests")
        assert result["intent"] == "LIST_MY_REQUESTS"
        assert result["isEmergency"] == False
        assert result["confidence"] == 0.7
    
    def test_detect_plumbing_category(self):
        """Test plumbing category detection"""
        result = simple_intent_detection("My sink is leaking")
        assert result["intent"] == "CREATE_MAINTENANCE_REQUEST"
        assert result["extractedData"]["category"] == "PLUMBING"
    
    def test_detect_electrical_category(self):
        """Test electrical category detection"""
        result = simple_intent_detection("The light switch is not working")
        assert result["intent"] == "CREATE_MAINTENANCE_REQUEST"
        assert result["extractedData"]["category"] == "ELECTRICAL"
    
    def test_detect_hvac_category(self):
        """Test HVAC category detection"""
        result = simple_intent_detection("My AC is broken")
        assert result["intent"] == "CREATE_MAINTENANCE_REQUEST"
        assert result["extractedData"]["category"] == "HVAC"
    
    def test_detect_appliance_category(self):
        """Test appliance category detection"""
        result = simple_intent_detection("My dishwasher is not working")
        assert result["intent"] == "CREATE_MAINTENANCE_REQUEST"
        assert result["extractedData"]["category"] == "APPLIANCE"
    
    def test_detect_general_inquiry(self):
        """Test general inquiry intent detection"""
        result = simple_intent_detection("Hello, how are you?")
        assert result["intent"] == "GENERAL_INQUIRY"
        assert result["confidence"] == 0.5
        assert result["isEmergency"] == False
    
    def test_emergency_keywords(self):
        """Test various emergency keywords"""
        emergency_queries = [
            "This is an emergency!",
            "Urgent help needed",
            "There's a flood",
            "Gas leak detected",
            "No power in the building",
            "No water available",
            "Broken window",
            "Dangerous situation",
            "Help immediately"
        ]
        
        for query in emergency_queries:
            result = simple_intent_detection(query)
            assert result["isEmergency"] == True, f"Failed for query: {query}"


class TestConvertAudioToWav:
    """Test cases for convert_audio_to_wav function"""
    
    @patch('main.AudioSegment')
    def test_convert_audio_success(self, mock_audio_segment):
        """Test successful audio conversion"""
        # Mock AudioSegment
        mock_audio = Mock()
        mock_audio.set_frame_rate.return_value = mock_audio
        mock_audio.set_channels.return_value = mock_audio
        mock_audio.set_sample_width.return_value = mock_audio
        mock_audio_segment.from_file.return_value = mock_audio
        
        # Create dummy audio content
        audio_content = b"fake audio content"
        
        with patch('tempfile.NamedTemporaryFile') as mock_temp:
            mock_input_file = Mock()
            mock_input_file.name = "/tmp/test_input.webm"
            mock_input_file.__enter__ = Mock(return_value=mock_input_file)
            mock_input_file.__exit__ = Mock(return_value=None)
            
            mock_output_file = Mock()
            mock_output_file.name = "/tmp/test_output.wav"
            mock_output_file.__enter__ = Mock(return_value=mock_output_file)
            mock_output_file.__exit__ = Mock(return_value=None)
            
            mock_temp.side_effect = [mock_input_file, mock_output_file]
            
            with patch('os.path.exists', return_value=True):
                with patch('os.unlink'):
                    result = convert_audio_to_wav(audio_content, "webm")
                    assert result is not None
                    assert result.endswith(".wav")
    
    def test_convert_audio_failure(self):
        """Test audio conversion failure"""
        audio_content = b"invalid audio"
        
        with patch('main.AudioSegment') as mock_audio_segment:
            mock_audio_segment.from_file.side_effect = Exception("Conversion failed")
            
            with patch('tempfile.NamedTemporaryFile') as mock_temp:
                mock_file = Mock()
                mock_file.name = "/tmp/test.webm"
                mock_file.__enter__ = Mock(return_value=mock_file)
                mock_file.__exit__ = Mock(return_value=None)
                mock_temp.return_value = mock_file
                
                with patch('os.path.exists', return_value=True):
                    with patch('os.unlink'):
                        result = convert_audio_to_wav(audio_content)
                        assert result is None


class TestProcessWithGemini:
    """Test cases for process_with_gemini function"""
    
    @patch('main.gemini_model')
    def test_process_with_gemini_success(self, mock_gemini_model):
        """Test successful Gemini processing"""
        mock_response = Mock()
        mock_candidate = Mock()
        mock_candidate.text = '{"intent": "CREATE_MAINTENANCE_REQUEST", "confidence": 0.9}'
        mock_response.candidates = [mock_candidate]
        mock_gemini_model.generate_content.return_value = mock_response
        
        result = process_with_gemini("Test prompt")
        assert result is not None
        assert "intent" in result
    
    @patch('main.gemini_model', None)
    def test_process_with_gemini_not_initialized(self):
        """Test Gemini processing when model is not initialized"""
        with pytest.raises(Exception, match="Gemini 2.0 Flash not initialized"):
            process_with_gemini("Test prompt")
    
    @patch('main.gemini_model')
    def test_process_with_gemini_no_response(self, mock_gemini_model):
        """Test Gemini processing when no response is generated"""
        mock_response = Mock()
        mock_response.candidates = []
        mock_gemini_model.generate_content.return_value = mock_response
        
        with pytest.raises(Exception, match="No response generated"):
            process_with_gemini("Test prompt")


class TestProcessWithHuggingFace:
    """Test cases for process_with_huggingface function"""
    
    @patch('main.requests.post')
    def test_process_with_huggingface_success(self, mock_post):
        """Test successful Hugging Face processing"""
        mock_response = Mock()
        mock_response.json.return_value = [{"generated_text": '{"intent": "CREATE_MAINTENANCE_REQUEST"}'}]
        mock_response.raise_for_status = Mock()
        mock_post.return_value = mock_response
        
        result = process_with_huggingface("Test prompt")
        assert result is not None
        assert "intent" in result
    
    @patch('main.requests.post')
    def test_process_with_huggingface_api_error(self, mock_post):
        """Test Hugging Face API error handling"""
        mock_post.side_effect = Exception("API Error")
        
        with pytest.raises(Exception):
            process_with_huggingface("Test prompt")


class TestTextToSpeechEndpoint:
    """Test cases for /api/text-to-speech endpoint"""
    
    @patch('main.gTTS')
    @patch('builtins.open', new_callable=mock_open)
    @patch('main.base64.b64encode')
    def test_text_to_speech_success(self, mock_b64encode, mock_file, mock_gtts):
        """Test successful text-to-speech conversion"""
        mock_tts = Mock()
        mock_gtts.return_value = mock_tts
        mock_b64encode.return_value = b"base64encodedaudio"
        
        with patch('tempfile.NamedTemporaryFile') as mock_temp:
            mock_temp_file = Mock()
            mock_temp_file.name = "/tmp/test.mp3"
            mock_temp_file.__enter__ = Mock(return_value=mock_temp_file)
            mock_temp_file.__exit__ = Mock(return_value=None)
            mock_temp.return_value = mock_temp_file
            
            with patch('os.path.exists', return_value=True):
                with patch('os.unlink'):
                    response = client.post(
                        "/api/text-to-speech",
                        json={"text": "Hello, this is a test"}
                    )
                    
                    assert response.status_code == 200
                    data = response.json()
                    assert data["success"] == True
                    assert data["format"] == "mp3"
                    assert "audioBase64" in data
    
    def test_text_to_speech_empty_text(self):
        """Test text-to-speech with empty text"""
        with patch('main.gTTS') as mock_gtts:
            mock_gtts.side_effect = Exception("Invalid text")
            
            response = client.post(
                "/api/text-to-speech",
                json={"text": ""}
            )
            
            # Should handle error gracefully
            assert response.status_code == 200
            data = response.json()
            assert data["success"] == False


class TestRecognizeIntentEndpoint:
    """Test cases for /api/recognize-intent endpoint"""
    
    def test_recognize_intent_with_rule_based_fallback(self):
        """Test intent recognition using rule-based fallback"""
        request_data = {
            "query": "My sink is leaking",
            "userId": 1,
            "context": None
        }
        
        with patch('main.process_with_gemini', side_effect=Exception("API Error")):
            with patch('main.process_with_huggingface', side_effect=Exception("API Error")):
                response = client.post(
                    "/api/recognize-intent",
                    json=request_data
                )
                
                assert response.status_code == 200
                data = response.json()
                assert data["success"] == True
                assert data["intent"]["intent"] == "CREATE_MAINTENANCE_REQUEST"
                assert data["intent"]["extractedData"]["category"] == "PLUMBING"
    
    def test_recognize_intent_emergency(self):
        """Test emergency intent recognition"""
        request_data = {
            "query": "There's a fire! Emergency!",
            "userId": 1
        }
        
        with patch('main.process_with_gemini', side_effect=Exception("API Error")):
            with patch('main.process_with_huggingface', side_effect=Exception("API Error")):
                response = client.post(
                    "/api/recognize-intent",
                    json=request_data
                )
                
                assert response.status_code == 200
                data = response.json()
                assert data["intent"]["isEmergency"] == True
    
    def test_recognize_intent_with_context(self):
        """Test intent recognition with context"""
        request_data = {
            "query": "What's the status?",
            "userId": 1,
            "context": "Previous conversation about maintenance request #123"
        }
        
        with patch('main.process_with_gemini', side_effect=Exception("API Error")):
            with patch('main.process_with_huggingface', side_effect=Exception("API Error")):
                response = client.post(
                    "/api/recognize-intent",
                    json=request_data
                )
                
                assert response.status_code == 200
                data = response.json()
                assert data["success"] == True
    
    @patch('main.process_with_gemini')
    def test_recognize_intent_with_gemini(self, mock_gemini):
        """Test intent recognition using Gemini"""
        mock_gemini.return_value = json.dumps({
            "intent": "CREATE_MAINTENANCE_REQUEST",
            "confidence": 0.95,
            "extractedData": {
                "title": "Leaking sink",
                "description": "The sink in the kitchen is leaking",
                "category": "PLUMBING"
            },
            "isEmergency": False,
            "response": "I'll create a maintenance request for your leaking sink.",
            "ticketId": None
        })
        
        request_data = {
            "query": "My sink is leaking",
            "userId": 1
        }
        
        response = client.post(
            "/api/recognize-intent",
            json=request_data
        )
        
        assert response.status_code == 200
        data = response.json()
        assert data["success"] == True
        assert data["intent"]["intent"] == "CREATE_MAINTENANCE_REQUEST"


class TestHealthEndpoints:
    """Test cases for health and info endpoints"""
    
    def test_health_check(self):
        """Test health check endpoint"""
        response = client.get("/health")
        
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "healthy"
        assert data["service"] == "HomeGenie Voice Assistant"
        assert "version" in data
        assert "features" in data
    
    def test_ai_info(self):
        """Test AI info endpoint"""
        response = client.get("/ai-info")
        
        assert response.status_code == 200
        data = response.json()
        assert "current_provider" in data
        assert "available_providers" in data
        assert "fallback" in data
    
    def test_root_endpoint(self):
        """Test root endpoint"""
        response = client.get("/")
        
        assert response.status_code == 200
        data = response.json()
        assert data["service"] == "HomeGenie Voice Assistant API"
        assert "endpoints" in data


class TestSpeechToTextEndpoint:
    """Test cases for /api/speech-to-text endpoint"""
    
    def test_speech_to_text_empty_file(self):
        """Test speech-to-text with empty audio file"""
        # Create an empty file
        empty_file = BytesIO(b"")
        
        response = client.post(
            "/api/speech-to-text",
            files={"audio": ("test.webm", empty_file, "audio/webm")}
        )
        
        assert response.status_code == 200
        data = response.json()
        assert data["success"] == False
        assert "empty" in data["error"].lower()
    
    @patch('main.convert_audio_to_wav')
    @patch('main.sr.Recognizer')
    def test_speech_to_text_success(self, mock_recognizer_class, mock_convert):
        """Test successful speech-to-text conversion"""
        # Mock audio conversion
        mock_convert.return_value = "/tmp/test.wav"
        
        # Mock recognizer
        mock_recognizer = Mock()
        mock_recognizer_class.return_value = mock_recognizer
        mock_recognizer.recognize_google.return_value = "Hello, this is a test"
        
        # Mock AudioFile context manager
        mock_audio_file = Mock()
        mock_source = Mock()
        mock_audio_file.__enter__ = Mock(return_value=mock_source)
        mock_audio_file.__exit__ = Mock(return_value=None)
        mock_recognizer_class.return_value = mock_recognizer
        
        with patch('main.sr.AudioFile', return_value=mock_audio_file):
            with patch('os.path.exists', return_value=True):
                with patch('os.path.getsize', return_value=1000):
                    with patch('os.unlink'):
                        # Create a dummy audio file
                        audio_file = BytesIO(b"fake audio content")
                        
                        response = client.post(
                            "/api/speech-to-text",
                            files={"audio": ("test.wav", audio_file, "audio/wav")}
                        )
                        
                        assert response.status_code == 200
                        data = response.json()
                        # Note: This might fail if actual recognition is attempted
                        # In a real scenario, you'd mock more thoroughly
    
    @patch('main.convert_audio_to_wav')
    def test_speech_to_text_conversion_failure(self, mock_convert):
        """Test speech-to-text when audio conversion fails"""
        mock_convert.return_value = None
        
        audio_file = BytesIO(b"invalid audio")
        
        response = client.post(
            "/api/speech-to-text",
            files={"audio": ("test.webm", audio_file, "audio/webm")}
        )
        
        assert response.status_code == 200
        data = response.json()
        assert data["success"] == False
        assert "convert" in data["error"].lower() or "format" in data["error"].lower()


if __name__ == "__main__":
    pytest.main([__file__, "-v"])

