package com.example.myai.data.source

import com.example.myai.data.model.ChatRequest
import com.example.myai.data.model.ChatResponse
import com.example.myai.data.model.ChatResponseMessage
import com.example.myai.data.model.GoogleChatRequest
import com.example.myai.data.model.GoogleContent
import com.example.myai.data.model.GooglePart
import com.example.myai.data.remote.GoogleApiService

/**
 * Google-specific implementation of ChatDataSource.
 * Delegates HTTP communication to GoogleApiService and maps requests/responses.
 */
class GoogleDataSource(
    private val apiService: GoogleApiService
) : ChatDataSource {

    override suspend fun sendMessage(request: ChatRequest): Result<ChatResponse> {
        val googleRequest = mapToGoogleRequest(request)
        // Use gemini-1.5-flash as default if not specified or override if it's a known google model
        val model = if (request.model.contains("gemini")) request.model else "gemini-1.5-flash"
        
        return apiService.generateContent(model, googleRequest).map { googleResponse ->
            val content = googleResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            ChatResponse(
                message = ChatResponseMessage(
                    role = "assistant",
                    content = content
                ),
                done = true
            )
        }
    }

    private fun mapToGoogleRequest(request: ChatRequest): GoogleChatRequest {
        val contents = request.messages.map { message ->
            GoogleContent(
                role = if (message.role == "assistant") "model" else "user",
                parts = listOf(GooglePart(text = message.content))
            )
        }
        return GoogleChatRequest(contents = contents)
    }
}
