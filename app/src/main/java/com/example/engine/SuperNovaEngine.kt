package com.example.engine

fun interface TokenCallback {
    fun onToken(token: String)
}

object SuperNovaEngine {
    
    init {
        // Load the C++ library compiled by CMake
        try {
            System.loadLibrary("ultronai-lib")
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
        }
    }

    /**
     * Pass the absolute paths of the downloaded models to the C++ engine.
     */
    external fun initEngine(qwenModelPath: String, whisperModelPath: String, piperModelPath: String): Boolean

    /**
     * Generate a response using the initialized engine.
     */
    external fun generateRealReply(userInput: String): String

    /**
     * Generate streaming response with real-time token callback.
     */
    external fun generateRealReplyStream(userInput: String, callback: TokenCallback?): String
}
