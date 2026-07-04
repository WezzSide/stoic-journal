package com.stoicjournal.core.data.repository

import com.stoicjournal.core.data.dao.PromptDao
import com.stoicjournal.core.data.entity.PromptEntity
import com.stoicjournal.core.domain.model.Prompt
import com.stoicjournal.core.domain.model.StoicismLevel
import com.stoicjournal.core.domain.repository.PromptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PromptRepositoryImpl @Inject constructor(
    private val promptDao: PromptDao
) : PromptRepository {

    override fun getAllPrompts(): Flow<List<Prompt>> =
        promptDao.getAll().map { it.map(PromptEntity::toDomain) }

    override fun getPromptsByDifficulty(level: StoicismLevel): Flow<List<Prompt>> =
        promptDao.getByDifficulty(level).map { it.map(PromptEntity::toDomain) }
}

private fun PromptEntity.toDomain() = Prompt(
    id = id,
    text = text,
    theme = theme,
    philosopherId = philosopherId,
    difficultyLevel = difficultyLevel
)
