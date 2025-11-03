package com.example.vendorconnect.chat

data class ChatMessage(
    val fromUser: Boolean,
    val text: String,
    val streaming: Boolean = false
)
