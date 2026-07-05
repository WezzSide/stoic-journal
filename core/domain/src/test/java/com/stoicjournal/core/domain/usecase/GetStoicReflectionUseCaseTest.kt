package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.model.AiReflectionRequest
import com.stoicjournal.core.domain.model.AiReflectionResponse
import com.stoicjournal.core.domain.model.AiReflectionTheme
import com.stoicjournal.core.domain.repository.AiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class GetStoicReflectionUseCaseTest {

    private val fakeAiRepository = object : AiRepository {
        var shouldReturnError = false
        
        override suspend fun getReflection(request: AiReflectionRequest): Result<AiReflectionResponse> {
            return if (shouldReturnError) {
                Result.failure(Exception("AI Error"))
            } else {
                Result.success(
                    AiReflectionResponse(
                        insight = "Insight",
                        interpretation = "Interpretation",
                        reflectiveQuestion = "Question",
                        actionableStep = "Step",
                        theme = request.theme
                    )
                )
            }
        }

        override fun streamReflection(request: AiReflectionRequest): Flow<AiReflectionResponse> = emptyFlow()
    }

    private val useCase = GetStoicReflectionUseCase(fakeAiRepository)

    @Test
    fun `invoke with non-empty content returns success`() = runTest {
        val result = useCase("Thinking about my career", AiReflectionTheme.FOCUS)
        assertTrue(result.isSuccess)
        assertEquals("Insight", result.getOrNull()?.insight)
        assertEquals(AiReflectionTheme.FOCUS, result.getOrNull()?.theme)
    }

    @Test
    fun `invoke with blank content returns failure`() = runTest {
        val result = useCase("   ", AiReflectionTheme.FOCUS)
        assertTrue(result.isFailure)
        assertEquals("Input content cannot be empty", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke when repository fails returns failure`() = runTest {
        fakeAiRepository.shouldReturnError = true
        val result = useCase("Valid content", AiReflectionTheme.FOCUS)
        assertTrue(result.isFailure)
        assertEquals("AI Error", result.exceptionOrNull()?.message)
    }
}
