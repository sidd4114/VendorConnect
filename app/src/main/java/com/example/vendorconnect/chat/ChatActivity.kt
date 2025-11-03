package com.example.vendorconnect.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import com.example.vendorconnect.BuildConfig
import com.example.vendorconnect.R
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChatActivity : AppCompatActivity() {

    private lateinit var messageContainer: LinearLayout
    private lateinit var scrollView: NestedScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val sendButton = findViewById<MaterialButton>(R.id.sendButton)
        val messageInput = findViewById<EditText>(R.id.messageInput)
        messageContainer = findViewById(R.id.messageContainer)
        scrollView = findViewById(R.id.scrollView)

        sendButton.setOnClickListener {
            val message = messageInput.text.toString().trim()
            if (message.isNotEmpty()) {
                addMessage(message, true)
                messageInput.text.clear()
                callGeminiAPI(message)
            }
        }
        
        // Allow sending on Enter key
        messageInput.setOnEditorActionListener { _, _, _ ->
            val message = messageInput.text.toString().trim()
            if (message.isNotEmpty()) {
                addMessage(message, true)
                messageInput.text.clear()
                callGeminiAPI(message)
            }
            true
        }
    }

    private fun addMessage(text: String, isUser: Boolean) {
        val layoutInflater = LayoutInflater.from(this)
        val messageView = if (isUser) {
            layoutInflater.inflate(R.layout.item_message_user, messageContainer, false)
        } else {
            layoutInflater.inflate(R.layout.item_message_bot, messageContainer, false)
        }
        
        val textView = if (isUser) {
            messageView.findViewById<TextView>(R.id.textUser)
        } else {
            messageView.findViewById<TextView>(R.id.textBot)
        }
        
        textView.text = text
        messageContainer.addView(messageView)
        
        // Scroll to bottom with smooth animation
        scrollView.post {
            scrollView.smoothScrollTo(0, scrollView.getChildAt(0).height)
        }
    }

    private fun callGeminiAPI(userMessage: String) {
        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = userMessage))))
        )

        val apiKey = BuildConfig.GEMINI_API_KEY

        val call = RetrofitClient.instance.generateText(apiKey, request)
        call.enqueue(object : Callback<GeminiResponse> {
            override fun onResponse(call: Call<GeminiResponse>, response: Response<GeminiResponse>) {
                if (response.isSuccessful) {
                    val botMessage = response.body()?.candidates?.firstOrNull()
                        ?.content?.parts?.firstOrNull()?.text
                    addMessage(botMessage ?: "No response from Gemini", false)
                } else {
                    addMessage("Error: ${response.errorBody()?.string()}", false)
                }
            }

            override fun onFailure(call: Call<GeminiResponse>, t: Throwable) {
                addMessage("Failed: ${t.localizedMessage}", false)
            }
        })
    }
}
