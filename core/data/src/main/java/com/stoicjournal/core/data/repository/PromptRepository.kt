package com.stoicjournal.core.data.repository

import com.stoicjournal.core.data.entity.PromptEntity
import com.stoicjournal.core.domain.model.StoicismLevel
import kotlinx.coroutines.flow.Flow

interface PromptRepository {
    fun getAll(): Flow<List<PromptEntity>>
    fun getByDifficulty(level: StoicismLevel): Flow<List<PromptEntity>>
}
