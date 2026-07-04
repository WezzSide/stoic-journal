package com.stoicjournal.core.data.repository

import com.stoicjournal.core.data.dao.ExerciseDao
import com.stoicjournal.core.data.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ExerciseRepositoryImpl @Inject constructor(
    private val dao: ExerciseDao
) : ExerciseRepository {
    override fun getAll(): Flow<List<ExerciseEntity>> = dao.getAll()
    override suspend fun getById(id: String): ExerciseEntity? = dao.getById(id)
}
