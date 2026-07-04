package com.stoicjournal.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stoicjournal.core.data.entity.PhilosopherEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhilosopherDao {
    @Query("SELECT * FROM philosophers ORDER BY name ASC")
    fun getAll(): Flow<List<PhilosopherEntity>>

    @Query("SELECT * FROM philosophers WHERE id = :id")
    suspend fun getById(id: String): PhilosopherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(philosophers: List<PhilosopherEntity>)
}
