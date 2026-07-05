package com.stoicjournal.core.data.repository

import com.stoicjournal.core.domain.model.AiFailureReason
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
            val result = delegate.getReflection(request)
            if (result.isSuccess) {
                val response = result.getOrNull()
                if (response != null) {
                    Result.success(response)
                } else {
                    Result.failure(Exception("Empty response from AI"))
                }
            } else {
                result
            }
        } catch (e: Exception) {
            // Here we would map specific SDK exceptions to domain exceptions
            Result.failure(e)
        }
    }

    override fun streamReflection(request: AiReflectionRequest): Flow<AiReflectionResponse> {
        return delegate.streamReflection(request)
            .catch { e ->
                // Handle stream errors
                throw e
            }
    }
}
