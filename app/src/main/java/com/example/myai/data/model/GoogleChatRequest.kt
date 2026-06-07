package com.example.myai.data.model

data class GoogleChatRequest(
    val contents: List<GoogleContent>
)

data class GoogleContent(
    val role: String? = null,
    val parts: List<GooglePart>
)

data class GooglePart(
    val text: String? = null,
    val inline_data: GoogleInlineData? = null
)

data class GoogleInlineData(
    val mime_type: String,
    val data: String
)
