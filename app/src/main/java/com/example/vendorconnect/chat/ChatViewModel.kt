package com.example.vendorconnect.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val service: GeminiChatService
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    fun sendMessage(input: String, streaming: Boolean = true) {
        if (input.isBlank()) return
        _messages.value = _messages.value + ChatMessage(true, input)

        if (streaming) {
            viewModelScope.launch {
                _messages.value = _messages.value + ChatMessage(false, "", streaming = true)
                val acc = StringBuilder()
                try {
                    service.sendStream(input).collect { chunk ->
                        acc.append(chunk)
                        _messages.value = _messages.value.dropLast(1) + ChatMessage(false, acc.toString(), streaming = true)
                    }
                } catch (e: Exception) {
                    acc.clear()
                    acc.append("Sorry, I ran into an error. Please try again.")
                } finally {
                    _messages.value = _messages.value.dropLast(1) + ChatMessage(false, acc.toString(), streaming = false)
                }
            }
        } else {
            viewModelScope.launch {
                val reply = try { service.send(input) } catch (e: Exception) { "Sorry, I ran into an error." }
                _messages.value = _messages.value + ChatMessage(false, reply)
            }
        }
    }
}
