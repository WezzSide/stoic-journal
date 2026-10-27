package com.stoicjournal.core.data.repository

import com.stoicjournal.core.domain.model.AiReflectionRequest
import com.stoicjournal.core.domain.model.AiReflectionResponse
import com.stoicjournal.core.domain.model.AiReflectionTheme
import com.stoicjournal.core.domain.repository.AiRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeAiRepository @Inject constructor() : AiRepository {

    override suspend fun getReflection(request: AiReflectionRequest): Result<AiReflectionResponse> {
        delay(1000) // Simulate network delay
        return Result.success(createFakeResponse(request.theme))
    }

    override fun streamReflection(request: AiReflectionRequest): Flow<AiReflectionResponse> = flow {
        delay(500)
        emit(createFakeResponse(request.theme))
    }

    private fun createFakeResponse(theme: AiReflectionTheme): AiReflectionResponse {
        return AiReflectionResponse(
            insight = "Stoicism teaches us that we suffer more in imagination than in reality.",
            interpretation = "Your concern about '${theme.displayName}' is a product of your judgments, not the event itself.",
            reflectiveQuestion = "What part of this is actually within your control?",
            actionableStep = "Identify one small action you can take right now that is within your control.",
            quote = "We are more often frightened than hurt; and we suffer more from imagination than from reality. — Seneca",
            principle = "Dichotomy of Control",
            theme = theme
        )
    }
}
