package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChatEntity
import com.example.data.ChatRepository
import com.example.data.MessageEntity
import com.example.engine.ModelDownloader
import com.example.engine.VoiceManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File

data class ThinkingStep(
    val title: String,
    val subtitle: String,
    val isDone: Boolean = false,
    val isActive: Boolean = false
)

class ChatViewModel(private val repository: ChatRepository, private val context: Context) : ViewModel() {

    private val voiceManager = VoiceManager(context)

    val isSpeaking = voiceManager.isSpeaking
    val currentSpeakingMsgId = voiceManager.currentSpeakingMsgId
    val isListening = voiceManager.isListening
    val soundLevel = voiceManager.soundLevel

    fun speakMessage(message: MessageEntity) {
        voiceManager.speak(message.text, message.id)
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
    }

    fun startListening(
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        voiceManager.startListening(onPartialResult, onFinalResult, onError)
    }

    fun stopListening() {
        voiceManager.stopListening()
    }

    val activeChats: StateFlow<List<ChatEntity>> = repository.activeChats
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentChatId = MutableStateFlow<Int?>(null)
    val currentChatId = _currentChatId.asStateFlow()

    private val _currentMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val currentMessages = _currentMessages.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping = _isTyping.asStateFlow()

    private val _streamingResponse = MutableStateFlow<String?>(null)
    val streamingResponse = _streamingResponse.asStateFlow()

    private val _realThinkingText = MutableStateFlow<String?>(null)
    val realThinkingText = _realThinkingText.asStateFlow()

    private val _isReasoningPhase = MutableStateFlow(false)
    val isReasoningPhase = _isReasoningPhase.asStateFlow()

    // Download & Setup States
    val downloadProgress = MutableStateFlow(0f)
    val setupStatus = MutableStateFlow("Initializing Network...")
    val isSetupComplete = MutableStateFlow(false)

    private val _extremeQuantization = MutableStateFlow(false)
    val extremeQuantization = _extremeQuantization.asStateFlow()

    private val _deepThinking = MutableStateFlow(false)
    val deepThinking = _deepThinking.asStateFlow()

    init {
        viewModelScope.launch {
            // No pre-populated dummy chats
        }
    }

    fun startModelSetup() {
        viewModelScope.launch {
            val dir = context.filesDir
            val qwenPath = File(dir, "qwen2.5-vl-3b-q4_k_m.gguf")
            val whisperPath = File(dir, "ggml-base.en.bin")
            val piperPath = File(dir, "en_US-lessac-medium.onnx")
            
            // Clean up obsolete model files to free disk space if present
            val oldModel1 = File(dir, "qwen2-vl-2b-q4_k_m.gguf")
            val oldModel2 = File(dir, "gemma-2-2b-it-Q4_K_M.gguf")
            if (oldModel1.exists()) oldModel1.delete()
            if (oldModel2.exists()) oldModel2.delete()

            // Fast-path bypass: If models are already downloaded, go straight to chat!
            if (qwenPath.exists() && qwenPath.length() > 1500_000_000L && // > 1.5GB
                whisperPath.exists() && whisperPath.length() > 100_000_000L && // > 100MB
                piperPath.exists() && piperPath.length() > 30_000_000L) { // > 30MB
                
                var success = false
                withContext(Dispatchers.IO) {
                    try {
                        success = com.example.engine.SuperNovaEngine.initEngine(qwenPath.absolutePath, whisperPath.absolutePath, piperPath.absolutePath)
                    } catch (e: UnsatisfiedLinkError) {
                        e.printStackTrace()
                    }
                }
                if (success) {
                    isSetupComplete.value = true
                } else {
                    setupStatus.value = "Error: Failed to initialize engine. Model might be corrupted."
                }
                return@launch
            }
            val downloader = ModelDownloader(context)
            
            // Real HuggingFace Direct Download Links
            val qwenUrl = "https://huggingface.co/ggml-org/Qwen2.5-VL-3B-Instruct-GGUF/resolve/main/Qwen2.5-VL-3B-Instruct-Q4_K_M.gguf" // ~1.93GB
            val whisperUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.en.bin" // ~140MB
            val piperOnnxUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/lessac/medium/en_US-lessac-medium.onnx" // ~42MB
            val piperJsonUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/lessac/medium/en_US-lessac-medium.onnx.json" // ~5KB
            
            setupStatus.value = "Downloading Qwen2.5-VL-3B (GGUF Q4_K_M)..."
            var downloadError = false
            downloader.downloadModel(qwenUrl, "qwen2.5-vl-3b-q4_k_m.gguf", 1930L).collect { state ->
                downloadProgress.value = state.progress * 0.70f // 0 to 70% weight
                setupStatus.value = state.status
                if (state.error) downloadError = true
            }

            if (!downloadError) {
                setupStatus.value = "Downloading Whisper.cpp base model..."
                downloader.downloadModel(whisperUrl, "ggml-base.en.bin", 150L).collect { state ->
                    downloadProgress.value = 0.70f + (state.progress * 0.15f) // 70 to 85% weight
                    setupStatus.value = state.status
                    if (state.error) downloadError = true
                }
            }

            if (!downloadError) {
                setupStatus.value = "Downloading Piper TTS voice..."
                downloader.downloadModel(piperOnnxUrl, "en_US-lessac-medium.onnx", 45L).collect { state ->
                    downloadProgress.value = 0.85f + (state.progress * 0.10f) // 85 to 95% weight
                    setupStatus.value = state.status
                    if (state.error) downloadError = true
                }
            }
            
            if (!downloadError) {
                setupStatus.value = "Downloading Piper Configuration..."
                downloader.downloadModel(piperJsonUrl, "en_US-lessac-medium.onnx.json", 1L).collect { state ->
                    downloadProgress.value = 0.95f + (state.progress * 0.05f) // 95 to 100% weight
                    setupStatus.value = state.status
                    if (state.error) downloadError = true
                }
            }

            if (downloadError) return@launch // Stop setup if any download failed

            setupStatus.value = "Initializing Edge C++ Engine..."
            
            var success = false
            withContext(Dispatchers.IO) {
                try {
                    success = com.example.engine.SuperNovaEngine.initEngine(qwenPath.absolutePath, whisperPath.absolutePath, piperPath.absolutePath)
                } catch (e: UnsatisfiedLinkError) {
                    e.printStackTrace()
                }
            }
            
            if (success) {
                delay(500) // For smooth transition
                isSetupComplete.value = true
            } else {
                setupStatus.value = "Error: Failed to initialize engine. Model might be corrupted."
            }
        }
    }

    fun selectChat(chatId: Int) {
        _currentChatId.value = chatId
        viewModelScope.launch {
            repository.getMessagesForChat(chatId).collect { msgs ->
                _currentMessages.value = msgs
            }
        }
    }

    fun areModelsDownloaded(): Boolean {
        val dir = context.filesDir
        val qwenPath = File(dir, "qwen2.5-vl-3b-q4_k_m.gguf")
        val whisperPath = File(dir, "ggml-base.en.bin")
        val piperPath = File(dir, "en_US-lessac-medium.onnx")
        return qwenPath.exists() && qwenPath.length() > 100_000_000L &&
               whisperPath.exists() && whisperPath.length() > 50_000_000L &&
               piperPath.exists() && piperPath.length() > 10_000_000L
    }

    fun sendMessage(text: String, imageUri: String? = null) {
        if ((text.isBlank() && imageUri == null) || _isTyping.value) return
        
        viewModelScope.launch {
            var chatId = _currentChatId.value
            
            // Auto-create chat if none exists
            if (chatId == null) {
                val titleText = if (text.isNotBlank()) text.take(20).trim() else "Image Analysis"
                chatId = repository.insertChat(ChatEntity(title = "Chat: $titleText")).toInt()
                _currentChatId.value = chatId
                // Launch separately so we don't block the message sending flow
                viewModelScope.launch {
                    repository.getMessagesForChat(chatId).collect { msgs ->
                        _currentMessages.value = msgs
                    }
                }
            }

            val userMsg = MessageEntity(
                chatId = chatId,
                text = text,
                isUser = true,
                imageUri = imageUri
            )
            repository.insertMessage(userMsg)
            
            _isTyping.value = true
            _streamingResponse.value = null
            _realThinkingText.value = null
            _isReasoningPhase.value = true

            if (imageUri != null) {
                // Multimodal Vision Processing
                try {
                    val parsedUri = android.net.Uri.parse(imageUri)
                    val visionResult = com.example.engine.ImageVisionProcessor.analyzeImage(
                        context = context,
                        imageUri = parsedUri,
                        userPrompt = text
                    )

                    _realThinkingText.value = visionResult.reasoning
                    delay(800) // Give user a moment to see the thinking process
                    _isReasoningPhase.value = false

                    // Stream vision output
                    val tokens = visionResult.description.split(" ")
                    val sb = StringBuilder()
                    for (token in tokens) {
                        sb.append(token).append(" ")
                        _streamingResponse.value = sb.toString()
                        delay(25)
                    }

                    val aiMsg = MessageEntity(
                        chatId = chatId,
                        text = visionResult.description,
                        isUser = false
                    )
                    repository.insertMessage(aiMsg)
                } catch (e: Exception) {
                    val errMsg = "Error processing image: ${e.message}"
                    repository.insertMessage(MessageEntity(chatId = chatId, text = errMsg, isUser = false))
                } finally {
                    _streamingResponse.value = null
                    _realThinkingText.value = null
                    _isReasoningPhase.value = false
                    _isTyping.value = false
                }
                return@launch
            }

            val rawBuffer = StringBuilder()

            // Offload the heavy C++ JNI call to the IO dispatcher
            val responseText = withContext(Dispatchers.IO) {
                try {
                    com.example.engine.SuperNovaEngine.generateRealReplyStream(text) { token ->
                        rawBuffer.append(token)
                        val currentText = rawBuffer.toString()

                        if (currentText.contains("</think>")) {
                            // Reasoning completed, model is streaming the actual answer
                            _isReasoningPhase.value = false
                            val parts = currentText.split("</think>", limit = 2)
                            val thoughts = parts[0].replace("<think>", "").trim()
                            val answer = parts.getOrNull(1)?.trimStart() ?: ""

                            _realThinkingText.value = thoughts
                            _streamingResponse.value = answer
                        } else if (currentText.startsWith("<think>")) {
                            // Model is currently actively reasoning inside <think>
                            _isReasoningPhase.value = true
                            _realThinkingText.value = currentText.removePrefix("<think>").trimStart()
                        } else {
                            // Direct stream (or no think tags outputted)
                            _isReasoningPhase.value = false
                            _streamingResponse.value = currentText
                        }
                    }
                } catch (e: UnsatisfiedLinkError) {
                    "Model processing failed. Ensure files are fully downloaded."
                } catch (e: Exception) {
                    "Error executing C++ Engine: ${e.message}"
                }
            }

            // Extract pure final answer (strip <think>...</think> completely for clean storage)
            val fullRaw = if (rawBuffer.isNotBlank()) rawBuffer.toString() else responseText
            val finalCleanOutput = if (fullRaw.contains("</think>")) {
                fullRaw.substringAfter("</think>").trim()
            } else {
                fullRaw.replace("<think>", "").trim()
            }

            val aiMsg = MessageEntity(
                chatId = chatId, 
                text = if (finalCleanOutput.isNotBlank()) finalCleanOutput else fullRaw, 
                isUser = false
            )
            repository.insertMessage(aiMsg)
            
            // Clean up thinking & streaming temporary states automatically
            _streamingResponse.value = null
            _realThinkingText.value = null
            _isReasoningPhase.value = false
            _isTyping.value = false
        }
    }

    fun createNewChat(title: String) {
        viewModelScope.launch {
            val newId = repository.insertChat(ChatEntity(title = title))
            selectChat(newId)
        }
    }

    fun archiveChat(id: Int) {
        viewModelScope.launch { repository.archiveChat(id) }
    }

    fun pinChat(id: Int, isPinned: Boolean) {
        viewModelScope.launch { repository.setPinned(id, isPinned) }
    }

    fun setUnread(id: Int, isUnread: Boolean) {
        viewModelScope.launch { repository.setUnread(id, isUnread) }
    }

    fun renameChat(id: Int, newTitle: String) {
        viewModelScope.launch { repository.renameChat(id, newTitle) }
    }

    fun deleteChat(id: Int) {
        viewModelScope.launch { repository.deleteChatById(id) }
        if (_currentChatId.value == id) {
            _currentChatId.value = null
            _currentMessages.value = emptyList()
        }
    }

    suspend fun getMessagesTextForChat(chatId: Int): String {
        return withContext(Dispatchers.IO) {
            val messages = repository.getMessagesForChat(chatId).first()
            messages.joinToString("\n\n") { msg ->
                val sender = if (msg.isUser) "You" else "AI"
                "$sender: ${msg.text}"
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch { 
            repository.clearHistory() 
            _currentChatId.value = null
            _currentMessages.value = emptyList()
        }
    }

    fun toggleExtremeQuantization(enabled: Boolean) {
        _extremeQuantization.value = enabled
    }

    fun toggleDeepThinking(enabled: Boolean) {
        _deepThinking.value = enabled
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.shutdown()
    }
}
