package com.example.vendorconnect.chat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.emptyFlow

class GeminiChatService(
    apiKey: String,
    modelName: String = "gemini-1.5-flash",
    systemPrompt: String = "You are VendorConnect's helpful customer assistant. Be concise and factual."
) {
    // Removed deprecated Gemini SDK imports and related code

    suspend fun send(message: String): String {
        // TODO: implement new send logic
        return ""
    }

    fun sendStream(message: String): Flow<String> =
        emptyFlow()
}
