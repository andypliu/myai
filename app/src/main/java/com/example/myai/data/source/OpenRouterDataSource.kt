package com.example.myai.data.source

import com.example.myai.data.model.ChatRequest
import com.example.myai.data.model.ChatResponse
import com.example.myai.data.model.ChatResponseMessage
import com.example.myai.data.model.OpenRouterChatRequest
import com.example.myai.data.model.OpenRouterMessage
import com.example.myai.data.remote.OpenRouterApiService
import kotlinx.coroutines.flow.Flow

class OpenRouterDataSource(private val apiService: OpenRouterApiService) {

    suspend fun sendMessage(request: ChatRequest): Result<ChatResponse> {
        val openRouterRequest = OpenRouterChatRequest(
            model = request.model,
            messages = request.messages.map {
                OpenRouterMessage(role = it.role, content = it.content)
            }
        )

        return apiService.chatCompletions(openRouterRequest).map { response ->
            ChatResponse(
                message = ChatResponseMessage(
                    role = "assistant",
                    content = response.choices.firstOrNull()?.message?.content ?: ""
                ),
                done = true
            )
        }
    }
}
