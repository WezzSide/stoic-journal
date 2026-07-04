package com.stoicjournal.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.stoicjournal.core.data.entity.QuoteEntity
import com.stoicjournal.core.domain.model.Theme
import kotlinx.coroutines.flow.Flow

@Dao
interface QuoteDao {
    @Query("SELECT * FROM quotes ORDER BY id ASC")
    fun getAll(): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes WHERE philosopherId = :philosopherId ORDER BY id ASC")
    fun getByPhilosopher(philosopherId: String): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes WHERE theme = :theme ORDER BY id ASC")
    fun getByTheme(theme: Theme): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes WHERE isFavorite = 1 ORDER BY id ASC")
    fun getFavorites(): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes WHERE id = :id")
    suspend fun getById(id: String): QuoteEntity?

    @Update
    suspend fun update(quote: QuoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(quotes: List<QuoteEntity>)
}
