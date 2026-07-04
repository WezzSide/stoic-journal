package com.stoicjournal.core.data.repository

import com.stoicjournal.core.data.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun getAll(): Flow<List<ExerciseEntity>>
    suspend fun getById(id: String): ExerciseEntity?
}
