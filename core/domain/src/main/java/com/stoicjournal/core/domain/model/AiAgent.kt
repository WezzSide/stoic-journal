package com.stoicjournal.core.domain.model

data class AiAgent(
    val id: String,
    val name: String,
    val systemPrompt: String,
    val tools: List<AiTool> = emptyList()
)

data class AiTool(
    val id: String,
    val name: String,
    val description: String,
    val parameters: Map<String, String> = emptyMap()
)
