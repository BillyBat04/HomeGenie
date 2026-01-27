from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
import speech_recognition as sr
from gtts import gTTS
import os
import tempfile
import base64
import logging
from typing import Optional, Dict, Any
import json
import re
import requests
import google.generativeai as genai
from dotenv import load_dotenv
from pydub import AudioSegment

load_dotenv()

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

app = FastAPI(title="HomeGenie Voice Assistant Service")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

AI_PROVIDER = os.getenv("AI_PROVIDER", "gemini")

HUGGINGFACE_API_TOKEN = os.getenv("HUGGINGFACE_API_TOKEN", "YOUR_HUGGINGFACE_API_TOKEN")
HUGGINGFACE_MODEL = "mistralai/Mixtral-8x7B-Instruct-v0.1"

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "YOUR_GEMINI_API_KEY")
if GEMINI_API_KEY:
    try:
        genai.configure(api_key=GEMINI_API_KEY)
        
        generation_config = {
            "temperature": 0.7,
            "top_p": 0.95,
            "top_k": 40,
            "max_output_tokens": 2048,
            "response_mime_type": "application/json",
        }
        
        gemini_model = genai.GenerativeModel(
            model_name='gemini-2.0-flash-exp',
            generation_config=generation_config
        )
        
        logger.info(" Gemini 2.0 Flash initialized successfully")
    except Exception as e:
        logger.error(f" Failed to initialize Gemini 2.0 Flash: {e}")
        gemini_model = None
else:
    gemini_model = None
    logger.warning(" Gemini API key not provided")

class TextToSpeechRequest(BaseModel):
    text: str = Field(..., description="Text to convert to speech")

class AIProcessingRequest(BaseModel):
    query: str = Field(..., description="User query text")
    userId: int = Field(..., description="User ID")
    context: Optional[str] = Field(None, description="Conversation context")

class IntentResult(BaseModel):
    intent: str
    confidence: float
    extractedData: Optional[Dict[str, Any]] = None
    ticketId: Optional[int] = None
    isEmergency: bool = False
    additionalInfo: Optional[str] = None

class AIProcessingResponse(BaseModel):
    response: str
    intent: IntentResult
    success: bool
    error: Optional[str] = None

def convert_audio_to_wav(input_file_content: bytes, input_format: str = None) -> Optional[str]:
        input_suffix = ".webm" if not input_format else f".{input_format}"
        with tempfile.NamedTemporaryFile(delete=False, suffix=input_suffix) as tmp_input:
            tmp_input.write(input_file_content)
            input_path = tmp_input.name
        
        logger.info(f" Saved input audio: {input_path} ({len(input_file_content)} bytes)")
        
        # Load audio with pydub (supports many formats including WebM)
        logger.info(" Converting audio to WAV format...")
        audio = AudioSegment.from_file(input_path)
        
        # Convert to optimal format for speech recognition
        audio = audio.set_frame_rate(16000)  # 16kHz sample rate
        audio = audio.set_channels(1)  # Mono
        audio = audio.set_sample_width(2)  # 16-bit
        
        # Create output WAV file
        with tempfile.NamedTemporaryFile(delete=False, suffix=".wav") as tmp_output:
            output_path = tmp_output.name
        
        # Export as WAV
        audio.export(output_path, format="wav")
        logger.info(f"Audio converted successfully: {output_path}")
        
        # Clean up input file
        if input_path and os.path.exists(input_path):
            os.unlink(input_path)
        
        return output_path
        
    except Exception as e:
        logger.error(f"Audio conversion failed: {e}", exc_info=True)
        
        # Clean up on error
        if input_path and os.path.exists(input_path):
            try:
                os.unlink(input_path)
            except:
                pass
        if output_path and os.path.exists(output_path):
            try:
                os.unlink(output_path)
            except:
                pass
        
        return None

def process_with_huggingface(prompt: str) -> str:
    try:
        API_URL = f"https://api-inference.huggingface.co/models/{HUGGINGFACE_MODEL}"
        headers = {"Authorization": f"Bearer {HUGGINGFACE_API_TOKEN}"}
        
        payload = {
            "inputs": prompt,
            "parameters": {
                "max_new_tokens": 500,
                "temperature": 0.7,
                "top_p": 0.95,
                "return_full_text": False
            }
        }
        
        response = requests.post(API_URL, headers=headers, json=payload, timeout=30)
        response.raise_for_status()
        
        result = response.json()
        if isinstance(result, list) and len(result) > 0:
            return result[0].get("generated_text", "")
        return str(result)
        
    except Exception as e:
        logger.error(f"Hugging Face API error: {e}")
        raise

def process_with_gemini(prompt: str) -> str:
    try:
        if not gemini_model:
            raise Exception("Gemini 2.0 Flash not initialized")
        
        response = gemini_model.generate_content(prompt)
        
        if response.candidates and len(response.candidates) > 0:
            return response.text
        else:
            raise Exception("No response generated from Gemini 2.0 Flash")
            
    except Exception as e:
        logger.error(f"Gemini 2.0 Flash API error: {e}")
        raise

@app.post("/api/speech-to-text")
async def speech_to_text(audio: UploadFile = File(...)):
    """
    Convert speech audio to text using Google Speech Recognition
    Supports WAV, WebM, MP3, and other common audio formats
    """
    wav_path = None
    
    try:
        logger.info(f" Received audio file: {audio.filename}")
        logger.info(f" Content type: {audio.content_type}")
        
        audio_content = await audio.read()
        audio_size = len(audio_content)
        logger.info(f" Audio size: {audio_size} bytes")
        
        if audio_size == 0:
            logger.error(" Audio file is empty")
            return {
                "text": "",
                "success": False,
                "confidence": 0.0,
                "error": "Audio file is empty"
            }
        
        if audio_size < 1000:  # Less than 1KB is suspicious
            logger.warning(f" Audio file is very small: {audio_size} bytes")
        
        # Detect format from filename or content type
        file_extension = os.path.splitext(audio.filename)[1].lower() if audio.filename else ""
        logger.info(f"File extension: {file_extension}")
        
        # Convert to WAV format
        logger.info(" Starting audio conversion...")
        wav_path = convert_audio_to_wav(audio_content, file_extension.replace(".", ""))
        
        if not wav_path:
            logger.error(" Audio conversion failed")
            return {
                "text": "",
                "success": False,
                "confidence": 0.0,
                "error": "Failed to convert audio format. Please ensure you're recording audio properly."
            }
        
        # Verify WAV file was created
        if not os.path.exists(wav_path):
            logger.error(f" WAV file not found: {wav_path}")
            return {
                "text": "",
                "success": False,
                "confidence": 0.0,
                "error": "Audio conversion produced no output"
            }
        
        wav_size = os.path.getsize(wav_path)
        logger.info(f"WAV file created: {wav_size} bytes")
        
        # Initialize speech recognizer with optimized settings
        recognizer = sr.Recognizer()
        recognizer.energy_threshold = 300
        recognizer.dynamic_energy_threshold = True
        recognizer.pause_threshold = 0.8
        
        # Load and process audio
        logger.info("🎧 Loading audio for recognition...")
        with sr.AudioFile(wav_path) as source:
            # Adjust for ambient noise
            recognizer.adjust_for_ambient_noise(source, duration=0.5)
            
            # Record the audio
            audio_data = recognizer.record(source)
            logger.info(" Audio loaded successfully")
            
            # Perform speech recognition
            try:
                logger.info("🔍 Performing speech recognition...")
                text = recognizer.recognize_google(audio_data, language="en-US", show_all=False)
                
                logger.info(f"TRANSCRIPTION SUCCESS: '{text}'")
                
                if not text or text.strip() == "":
                    return {
                        "text": "",
                        "success": False,
                        "confidence": 0.0,
                        "error": "No speech detected in audio"
                    }
                
                return {
                    "text": text.strip(),
                    "success": True,
                    "confidence": 0.9,
                    "error": None
                }
                
            except sr.UnknownValueError:
                logger.warning(" Google Speech Recognition could not understand audio")
                return {
                    "text": "",
                    "success": False,
                    "confidence": 0.0,
                    "error": "I couldn't understand the audio. Please speak clearly and try again."
                }
            except sr.RequestError as e:
                logger.error(f" Google Speech Recognition service error: {e}")
                return {
                    "text": "",
                    "success": False,
                    "confidence": 0.0,
                    "error": f"Speech recognition service error: {str(e)}"
                }
    
    except Exception as e:
        logger.error(f" CRITICAL ERROR in speech_to_text: {e}", exc_info=True)
        return {
            "text": "",
            "success": False,
            "confidence": 0.0,
            "error": f"Error processing audio: {str(e)}"
        }
    
    finally:
        # Clean up WAV file
        if wav_path and os.path.exists(wav_path):
            try:
                os.unlink(wav_path)
                logger.info(f"🧹 Cleaned up: {wav_path}")
            except Exception as e:
                logger.error(f"Failed to clean up WAV file: {e}")

@app.post("/api/text-to-speech")
async def text_to_speech(request: TextToSpeechRequest):
    tmp_path = None
    try:
        logger.info(f"Converting text to speech: {request.text[:50]}...")
        
        tts = gTTS(text=request.text, lang='en', slow=False)
        
        with tempfile.NamedTemporaryFile(delete=False, suffix=".mp3") as tmp_file:
            tmp_path = tmp_file.name
            tts.save(tmp_path)
            
            with open(tmp_path, 'rb') as audio_file:
                audio_content = audio_file.read()
                audio_base64 = base64.b64encode(audio_content).decode('utf-8')
        
        return {
            "audioBase64": audio_base64,
            "success": True,
            "error": None,
            "format": "mp3"
        }
        
    except Exception as e:
        logger.error(f"Text to speech error: {e}")
        return {
            "audioBase64": "",
            "success": False,
            "error": str(e),
            "format": None
        }
    finally:
        if tmp_path and os.path.exists(tmp_path):
            try:
                os.unlink(tmp_path)
            except:
                pass

@app.post("/api/recognize-intent")
async def recognize_intent(request: AIProcessingRequest):
    try:
        logger.info(f"Processing intent for user {request.userId}: {request.query}")
        
        system_instruction = """You are a maintenance assistant AI for HomeGenie residential property management.

Your job:
1. Understand user's intent
2. Extract maintenance request details
3. Detect emergencies
4. Provide helpful responses

Intents:
- CREATE_MAINTENANCE_REQUEST: Report maintenance issue
- QUERY_STATUS: Check request status
- LIST_MY_REQUESTS: List all requests
- EMERGENCY: Urgent situation
- GENERAL_INQUIRY: General questions
- UNKNOWN: Cannot determine

Maintenance Categories: PLUMBING, ELECTRICAL, HVAC, APPLIANCE, STRUCTURAL, PEST_CONTROL, CLEANING, OTHER

Emergency keywords: flood, fire, gas leak, no power, no water, broken window, security breach, injury

Respond ONLY with valid JSON (no markdown, no code blocks):
{
  "intent": "INTENT_NAME",
  "confidence": 0.0-1.0,
  "extractedData": {"title": "brief summary", "description": "detailed description", "category": "CATEGORY"},
  "isEmergency": true/false,
  "response": "natural response to user",
  "ticketId": null
}"""

        user_message = f"User query: {request.query}"
        if request.context:
            user_message += f"\n\nContext:\n{request.context}"
        
        full_prompt = f"{system_instruction}\n\n{user_message}"
        
        ai_response = None
        if AI_PROVIDER == "gemini" and gemini_model:
            try:
                logger.info("Using Google Gemini 2.0 Flash")
                ai_response = process_with_gemini(full_prompt)
            except Exception as e:
                logger.warning(f"Gemini 2.0 Flash failed, falling back: {e}")
        
        if not ai_response:
            try:
                logger.info("Using Hugging Face")
                ai_response = process_with_huggingface(full_prompt)
            except Exception as e:
                logger.warning(f"Hugging Face failed, using rule-based: {e}")
        
        if ai_response:
            logger.info(f"AI response received (length: {len(ai_response)})")
            cleaned_response = ai_response.strip()
            
            if cleaned_response.startswith("```"):
                cleaned_response = re.sub(r'^```(?:json)?\n?', '', cleaned_response)
                cleaned_response = re.sub(r'\n?```$', '', cleaned_response)
            
            json_match = re.search(r'\{.*\}', cleaned_response, re.DOTALL)
            if json_match:
                result_data = json.loads(json_match.group())
            else:
                result_data = json.loads(cleaned_response)
            
            intent_result = IntentResult(
                intent=result_data.get("intent", "UNKNOWN"),
                confidence=result_data.get("confidence", 0.5),
                extractedData=result_data.get("extractedData"),
                ticketId=result_data.get("ticketId"),
                isEmergency=result_data.get("isEmergency", False),
                additionalInfo=result_data.get("response", "")
            )
            
            return AIProcessingResponse(
                response=result_data.get("response", "I'm processing your request."),
                intent=intent_result,
                success=True,
                error=None
            )
        
        raise Exception("All AI providers failed")
        
    except Exception as e:
        logger.warning(f"Using rule-based fallback: {e}")
        
        fallback_result = simple_intent_detection(request.query)
        
        return AIProcessingResponse(
            response=fallback_result["response"],
            intent=IntentResult(
                intent=fallback_result["intent"],
                confidence=fallback_result["confidence"],
                extractedData=fallback_result.get("extractedData"),
                ticketId=None,
                isEmergency=fallback_result.get("isEmergency", False),
                additionalInfo=""
            ),
            success=True,
            error=None
        )

def simple_intent_detection(query: str) -> dict:
    query_lower = query.lower()
    
    emergency_keywords = ["emergency", "urgent", "flood", "fire", "gas leak", "no power", 
                         "no water", "broken", "dangerous", "help", "immediately"]
    is_emergency = any(keyword in query_lower for keyword in emergency_keywords)
    
    status_keywords = ["status", "update", "progress", "when", "check", "where is"]
    if any(keyword in query_lower for keyword in status_keywords):
        return {
            "intent": "QUERY_STATUS",
            "confidence": 0.7,
            "response": "Let me check the status of your request.",
            "isEmergency": False
        }
    
    list_keywords = ["show", "list", "all", "my requests", "what do i have"]
    if any(keyword in query_lower for keyword in list_keywords):
        return {
            "intent": "LIST_MY_REQUESTS",
            "confidence": 0.7,
            "response": "Let me get all your maintenance requests.",
            "isEmergency": False
        }
    
    maintenance_keywords = ["fix", "repair", "broken", "not working", "leak", "problem", 
                           "issue", "need", "help with"]
    
    if any(keyword in query_lower for keyword in maintenance_keywords):
        category = "OTHER"
        if any(word in query_lower for word in ["sink", "pipe", "toilet", "faucet", "drain", "water"]):
            category = "PLUMBING"
        elif any(word in query_lower for word in ["light", "outlet", "electric", "power", "switch"]):
            category = "ELECTRICAL"
        elif any(word in query_lower for word in ["ac", "heat", "hvac", "air", "temperature"]):
            category = "HVAC"
        elif any(word in query_lower for word in ["fridge", "stove", "oven", "dishwasher", "washer"]):
            category = "APPLIANCE"
        
        priority = "CRITICAL" if is_emergency else "HIGH"
        
        return {
            "intent": "CREATE_MAINTENANCE_REQUEST",
            "confidence": 0.8,
            "extractedData": {
                "title": query[:50],
                "description": query,
                "category": category
            },
            "response": f"I'll create a maintenance request for you with {priority} priority.",
            "isEmergency": is_emergency
        }
    
    return {
        "intent": "GENERAL_INQUIRY",
        "confidence": 0.5,
        "response": "I can help you report maintenance issues, check status, or list your requests. What would you like to do?",
        "isEmergency": False
    }

@app.get("/health")
async def health_check():
    return {
        "status": "healthy",
        "service": "HomeGenie Voice Assistant",
        "version": "2.1.0",
        "ai_provider": AI_PROVIDER,
        "ai_model": "gemini-2.0-flash-exp" if gemini_model else "fallback",
        "gemini_available": gemini_model is not None,
        "pydub_available": True,
        "features": {
            "speech_to_text": "Google Speech Recognition with WebM support",
            "text_to_speech": "gTTS",
            "ai_processing": f"{AI_PROVIDER.upper()} 2.0 Flash",
            "audio_formats": ["WAV", "WebM", "MP3", "OGG", "FLAC"]
        }
    }

@app.get("/ai-info")
async def ai_info():
    return {
        "current_provider": AI_PROVIDER,
        "gemini_configured": GEMINI_API_KEY != "",
        "gemini_working": gemini_model is not None,
        "huggingface_configured": HUGGINGFACE_API_TOKEN != "",
        "available_providers": {
            "huggingface": {
                "name": "Hugging Face",
                "model": HUGGINGFACE_MODEL,
                "cost": "FREE",
                "status": "configured" if HUGGINGFACE_API_TOKEN else "not configured"
            },
            "gemini": {
                "name": "Google Gemini 2.0 Flash",
                "model": "gemini-2.0-flash-exp",
                "cost": "FREE",
                "speed": "2x faster",
                "features": ["JSON mode", "optimized latency", "improved reasoning"],
                "status": "working" if gemini_model else ("configured" if GEMINI_API_KEY else "not configured")
            }
        },
        "fallback": "Rule-based detection (always available)"
    }

@app.get("/")
async def root():
    return {
        "service": "HomeGenie Voice Assistant API",
        "version": "2.1.0",
        "status": "running",
        "ai_model": "Gemini 2.0 Flash",
        "audio_support": "WebM/WAV/MP3 with pydub",
        "endpoints": {
            "health": "/health",
            "ai_info": "/ai-info",
            "speech_to_text": "/api/speech-to-text",
            "text_to_speech": "/api/text-to-speech",
            "recognize_intent": "/api/recognize-intent"
        }
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)