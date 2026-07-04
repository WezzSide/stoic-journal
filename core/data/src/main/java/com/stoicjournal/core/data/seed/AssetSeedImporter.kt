package com.stoicjournal.core.data.seed

import android.content.Context
import com.stoicjournal.core.data.dao.ExerciseDao
import com.stoicjournal.core.data.dao.PhilosopherDao
import com.stoicjournal.core.data.dao.PromptDao
import com.stoicjournal.core.data.dao.QuoteDao
import com.stoicjournal.core.data.database.StoicJournalDatabase
import com.stoicjournal.core.data.entity.ExerciseEntity
import com.stoicjournal.core.data.entity.PhilosopherEntity
import com.stoicjournal.core.data.entity.PromptEntity
import com.stoicjournal.core.data.entity.QuoteEntity
import com.stoicjournal.core.domain.model.StoicismLevel
import com.stoicjournal.core.domain.model.Theme
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import javax.inject.Inject

class AssetSeedImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: StoicJournalDatabase,
    private val quoteDao: QuoteDao,
    private val promptDao: PromptDao,
    private val exerciseDao: ExerciseDao,
    private val philosopherDao: PhilosopherDao
) {
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        val hasContent = quoteDao.getAll().first().isNotEmpty() &&
            promptDao.getAll().first().isNotEmpty() &&
            exerciseDao.getAll().first().isNotEmpty() &&
            philosopherDao.getAll().first().isNotEmpty()
        if (hasContent) return@withContext
        val json = Json { ignoreUnknownKeys = true }

        database.withTransaction {
            context.assets.open("quotes.json").use { stream ->
                val dtos: List<QuoteDto> = json.decodeFromStream(stream)
                quoteDao.insertAll(dtos.map { it.toEntity() })
            }
            context.assets.open("prompts.json").use { stream ->
                val dtos: List<PromptDto> = json.decodeFromStream(stream)
                promptDao.insertAll(dtos.map { it.toEntity() })
            }
            context.assets.open("exercises.json").use { stream ->
                val dtos: List<ExerciseDto> = json.decodeFromStream(stream)
                exerciseDao.insertAll(dtos.map { it.toEntity() })
            }
            context.assets.open("philosophers.json").use { stream ->
                val dtos: List<PhilosopherDto> = json.decodeFromStream(stream)
                philosopherDao.insertAll(dtos.map { it.toEntity() })
            }
        }
    }
}

@Serializable
private data class QuoteDto(
    val id: String,
    val text: String,
    val attribution: String,
    val philosopherId: String,
    val theme: String,
    val source: String? = null
) {
    fun toEntity() = QuoteEntity(
        id = id,
        text = text,
        attribution = attribution,
        philosopherId = philosopherId,
        theme = Theme.valueOf(theme),
        source = source
    )
}

@Serializable
private data class PromptDto(
    val id: String,
    val text: String,
    val theme: String,
    val philosopherId: String? = null,
    val difficultyLevel: String = "BEGINNER"
) {
    fun toEntity() = PromptEntity(
        id = id,
        text = text,
        theme = Theme.valueOf(theme),
        philosopherId = philosopherId,
        difficultyLevel = StoicismLevel.valueOf(difficultyLevel)
    )
}

@Serializable
private data class ExerciseDto(
    val id: String,
    val title: String,
    val description: String,
    val steps: List<String>,
    val theme: String,
    val difficultyLevel: String = "BEGINNER"
) {
    fun toEntity() = ExerciseEntity(
        id = id,
        title = title,
        description = description,
        steps = steps.joinToString("|"),
        theme = Theme.valueOf(theme),
        difficultyLevel = StoicismLevel.valueOf(difficultyLevel)
    )
}

@Serializable
private data class PhilosopherDto(
    val id: String,
    val name: String,
    val lived: String,
    val shortBio: String
) {
    fun toEntity() = PhilosopherEntity(
        id = id,
        name = name,
        lived = lived,
        shortBio = shortBio
    )
}
