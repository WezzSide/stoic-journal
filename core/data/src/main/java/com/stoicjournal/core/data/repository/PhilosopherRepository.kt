package com.stoicjournal.core.data.repository

import com.stoicjournal.core.data.entity.PhilosopherEntity
import kotlinx.coroutines.flow.Flow

interface PhilosopherRepository {
    fun getAll(): Flow<List<PhilosopherEntity>>
    suspend fun getById(id: String): PhilosopherEntity?
}
