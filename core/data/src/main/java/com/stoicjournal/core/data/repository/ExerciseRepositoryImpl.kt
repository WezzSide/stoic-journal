package com.stoicjournal.core.data.repository

import com.stoicjournal.core.data.dao.ExerciseDao
import com.stoicjournal.core.data.entity.ExerciseEntity
import com.stoicjournal.core.domain.model.Exercise
import com.stoicjournal.core.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ExerciseRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao
) : ExerciseRepository {

    override fun getAllExercises(): Flow<List<Exercise>> =
        exerciseDao.getAll().map { it.map(ExerciseEntity::toDomain) }

    override suspend fun getExerciseById(id: String): Exercise? =
        exerciseDao.getById(id)?.toDomain()
}

private fun ExerciseEntity.toDomain() = Exercise(
    id = id,
    title = title,
    description = description,
    steps = steps.split("|"),
    theme = theme,
    difficultyLevel = difficultyLevel
)
