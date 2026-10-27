package com.stoicjournal.core.data.repository

import com.stoicjournal.core.data.dao.AiReflectionDao
import com.stoicjournal.core.data.entity.AiReflectionEntity
import com.stoicjournal.core.domain.model.AiReflectionRequest
import com.stoicjournal.core.domain.model.AiReflectionResponse
import com.stoicjournal.core.domain.model.AiReflectionTheme
import com.stoicjournal.core.domain.repository.AiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class CachedAiRepository @Inject constructor(
    private val delegate: AiRepository,
    private val aiReflectionDao: AiReflectionDao
) : AiRepository {

    override suspend fun getReflection(request: AiReflectionRequest): Result<AiReflectionResponse> {
        val cached = aiReflectionDao.getReflectionByContent(request.content)
        if (cached != null) {
            return Result.success(cached.toDomainModel())
        }

        val result = delegate.getReflection(request)
        if (result.isSuccess) {
            result.getOrNull()?.let { response ->
                aiReflectionDao.insertReflection(response.toEntity(request.content))
            }
        }
        return result
    }

    override fun streamReflection(request: AiReflectionRequest): Flow<AiReflectionResponse> {
        return delegate.streamReflection(request).onEach { response ->
            // For MVP, we'll just cache each update, overwriting the previous one
            // This ensures the final complete response is cached
            aiReflectionDao.insertReflection(response.toEntity(request.content))
        }
    }

    private fun AiReflectionEntity.toDomainModel(): AiReflectionResponse {
        return AiReflectionResponse(
            insight = insight,
            interpretation = interpretation,
            reflectiveQuestion = reflectiveQuestion,
            actionableStep = actionableStep,
            quote = quote,
            principle = principle,
            timestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()),
            theme = AiReflectionTheme.valueOf(theme)
        )
    }

    private fun AiReflectionResponse.toEntity(content: String): AiReflectionEntity {
        return AiReflectionEntity(
            content = content,
            insight = insight,
            interpretation = interpretation,
            reflectiveQuestion = reflectiveQuestion,
            actionableStep = actionableStep,
            quote = quote,
            principle = principle,
            timestamp = timestamp.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            theme = theme.name
        )
    }
}
