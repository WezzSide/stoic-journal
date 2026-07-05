package com.stoicjournal.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stoicjournal.core.data.entity.AiReflectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiReflectionDao {
    @Query("SELECT * FROM ai_reflections ORDER BY timestamp DESC")
    fun getAllReflections(): Flow<List<AiReflectionEntity>>

    @Query("SELECT * FROM ai_reflections WHERE content = :content LIMIT 1")
    suspend fun getReflectionByContent(content: String): AiReflectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReflection(reflection: AiReflectionEntity)

    @Query("DELETE FROM ai_reflections")
    suspend fun clearCache()
}
