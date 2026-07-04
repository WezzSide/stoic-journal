package com.stoicjournal.core.data.repository

import com.stoicjournal.core.data.dao.PhilosopherDao
import com.stoicjournal.core.data.entity.PhilosopherEntity
import com.stoicjournal.core.domain.model.Philosopher
import com.stoicjournal.core.domain.repository.PhilosopherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PhilosopherRepositoryImpl @Inject constructor(
    private val philosopherDao: PhilosopherDao
) : PhilosopherRepository {

    override fun getAllPhilosophers(): Flow<List<Philosopher>> =
        philosopherDao.getAll().map { it.map(PhilosopherEntity::toDomain) }

    override suspend fun getPhilosopherById(id: String): Philosopher? =
        philosopherDao.getById(id)?.toDomain()
}

private fun PhilosopherEntity.toDomain() = Philosopher(
    id = id,
    name = name,
    lived = lived,
    shortBio = shortBio
)
