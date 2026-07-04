package com.stoicjournal.core.data.repository

import com.stoicjournal.core.data.dao.PromptDao
import com.stoicjournal.core.data.entity.PromptEntity
import com.stoicjournal.core.domain.model.StoicismLevel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PromptRepositoryImpl @Inject constructor(
    private val dao: PromptDao
) : PromptRepository {
    override fun getAll(): Flow<List<PromptEntity>> = dao.getAll()
    override fun getByDifficulty(level: StoicismLevel): Flow<List<PromptEntity>> = dao.getByDifficulty(level)
}
