package com.stoicjournal.core.domain.repository

import com.stoicjournal.core.domain.model.Philosopher
import kotlinx.coroutines.flow.Flow

interface PhilosopherRepository {
    fun getAllPhilosophers(): Flow<List<Philosopher>>
    suspend fun getPhilosopherById(id: String): Philosopher?
}
