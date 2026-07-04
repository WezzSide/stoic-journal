package com.stoicjournal.core.data.repository

import com.stoicjournal.core.data.dao.PhilosopherDao
import com.stoicjournal.core.data.entity.PhilosopherEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PhilosopherRepositoryImpl @Inject constructor(
    private val dao: PhilosopherDao
) : PhilosopherRepository {
    override fun getAll(): Flow<List<PhilosopherEntity>> = dao.getAll()
    override suspend fun getById(id: String): PhilosopherEntity? = dao.getById(id)
}
