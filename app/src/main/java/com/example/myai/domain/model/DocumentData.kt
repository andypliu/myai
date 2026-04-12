package com.example.myai.domain.model

data class DocumentData(
    val name: String,
    val content: String,  // Extracted text or base64
    val mimeType: String
)
