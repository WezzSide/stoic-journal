package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.util.StreakCalculator
import com.stoicjournal.core.domain.model.Streak
import com.stoicjournal.core.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetStreakUseCase @Inject constructor(
    private val journalRepository: JournalRepository
) {
    operator fun invoke(): Flow<Streak> =
        journalRepository.getAllEntries().map { StreakCalculator.calculate(it) }
}
