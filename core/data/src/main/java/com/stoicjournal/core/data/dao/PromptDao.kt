package com.stoicjournal.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stoicjournal.core.data.entity.PromptEntity
import com.stoicjournal.core.domain.model.StoicismLevel
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptDao {
    @Query("SELECT * FROM prompts ORDER BY id ASC")
    fun getAll(): Flow<List<PromptEntity>>

    @Query("SELECT * FROM prompts WHERE difficultyLevel = :level ORDER BY id ASC")
    fun getByDifficulty(level: StoicismLevel): Flow<List<PromptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(prompts: List<PromptEntity>)
}
