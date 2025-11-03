package com.example.vendorconnect.chat

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface GeminiApiService {

    @Headers("Content-Type: application/json")
    // Change is here: v1beta and gemini-1.5-flash
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    fun generateText(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): Call<GeminiResponse>
}