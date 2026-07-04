package com.stoicjournal.core.domain.repository

import com.stoicjournal.core.domain.model.Prompt
import com.stoicjournal.core.domain.model.StoicismLevel
import kotlinx.coroutines.flow.Flow

interface PromptRepository {
    fun getAllPrompts(): Flow<List<Prompt>>
    fun getPromptsByDifficulty(level: StoicismLevel): Flow<List<Prompt>>
}
