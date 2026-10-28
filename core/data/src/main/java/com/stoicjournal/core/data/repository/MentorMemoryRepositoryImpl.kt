package com.stoicjournal.core.data.repository

import com.stoicjournal.core.data.dao.MentorMemoryDao
import com.stoicjournal.core.data.entity.MentorMemoryEntity
import com.stoicjournal.core.domain.model.MentorMemory
import com.stoicjournal.core.domain.model.MentorMemoryCategory
import com.stoicjournal.core.domain.model.MentorMemoryStatus
import com.stoicjournal.core.domain.repository.MentorMemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MentorMemoryRepositoryImpl @Inject constructor(
    private val memoryDao: MentorMemoryDao
) : MentorMemoryRepository {

    override fun getApprovedMemories(): Flow<List<MentorMemory>> {
        return memoryDao.getApprovedMemories().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getAllMemories(): Flow<List<MentorMemory>> {
        return memoryDao.getAllMemories().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun approveMemory(id: String) {
        memoryDao.approveMemory(id, Instant.now().toEpochMilli())
    }

    override suspend fun saveMemory(memory: MentorMemory) {
        val entity = MentorMemoryEntity(
            id = memory.id,
            category = memory.category.name,
            content = memory.content,
            sourceEntryIds = memory.sourceEntryIds.joinToString(","),
            sourceConversationIds = memory.sourceConversationIds.joinToString(","),
            createdAt = memory.createdAt.toEpochMilli(),
            updatedAt = memory.updatedAt.toEpochMilli(),
            lastReviewedAt = memory.lastReviewedAt?.toEpochMilli(),
            status = memory.status.name,
            userApproved = memory.userApproved
        )
        memoryDao.insertOrUpdate(entity)
    }

    override suspend fun deleteMemory(id: String) {
        memoryDao.deleteMemory(id, Instant.now().toEpochMilli())
    }

    override suspend fun clearAll() {
        memoryDao.deleteAll()
    }

    private fun MentorMemoryEntity.toDomainModel(): MentorMemory {
        return MentorMemory(
            id = id,
            category = try { MentorMemoryCategory.valueOf(category) } catch (_: Exception) { MentorMemoryCategory.REFLECTION_THEME },
            content = content,
            sourceEntryIds = sourceEntryIds.split(",").filter { it.isNotBlank() },
            sourceConversationIds = sourceConversationIds.split(",").filter { it.isNotBlank() },
            createdAt = Instant.ofEpochMilli(createdAt),
            updatedAt = Instant.ofEpochMilli(updatedAt),
            lastReviewedAt = lastReviewedAt?.let { Instant.ofEpochMilli(it) },
            status = try { MentorMemoryStatus.valueOf(status) } catch (_: Exception) { MentorMemoryStatus.APPROVED },
            userApproved = userApproved
        )
    }
}
