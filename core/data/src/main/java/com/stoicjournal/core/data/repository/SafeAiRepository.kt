package com.stoicjournal.core.data.repository

import com.stoicjournal.core.domain.model.AiReflectionRequest
import com.stoicjournal.core.domain.model.AiReflectionResponse
import com.stoicjournal.core.domain.repository.AiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import javax.inject.Inject

class SafeAiRepository @Inject constructor(
    private val delegate: AiRepository
) : AiRepository {

    override suspend fun getReflection(request: AiReflectionRequest): Result<AiReflectionResponse> {
        return try {
            delegate.getReflection(request)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun streamReflection(request: AiReflectionRequest): Flow<AiReflectionResponse> {
        return delegate.streamReflection(request)
            .catch { e ->
                // Map or handle exceptions in the stream
                throw e
            }
    }
}
