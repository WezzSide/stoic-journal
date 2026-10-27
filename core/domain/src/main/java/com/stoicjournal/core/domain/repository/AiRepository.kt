package com.stoicjournal.core.domain.repository

import com.stoicjournal.core.domain.model.AiReflectionRequest
import com.stoicjournal.core.domain.model.AiReflectionResponse
import kotlinx.coroutines.flow.Flow

interface AiRepository {
    suspend fun getReflection(request: AiReflectionRequest): Result<AiReflectionResponse>
    fun streamReflection(request: AiReflectionRequest): Flow<AiReflectionResponse>
}
