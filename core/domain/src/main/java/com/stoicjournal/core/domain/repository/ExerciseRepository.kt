package com.stoicjournal.core.domain.repository

import com.stoicjournal.core.domain.model.Exercise
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun getAllExercises(): Flow<List<Exercise>>
    suspend fun getExerciseById(id: String): Exercise?
}
