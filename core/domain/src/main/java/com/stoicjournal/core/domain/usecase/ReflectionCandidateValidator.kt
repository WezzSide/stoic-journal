package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.model.ReflectionCandidate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReflectionCandidateValidator @Inject constructor() {

    fun validate(candidate: ReflectionCandidate): Result<Boolean> {
        if (candidate.title.isBlank()) {
            return Result.failure(IllegalArgumentException("Reflection title cannot be blank."))
        }
        if (candidate.summary.isBlank() && candidate.whatHappened.isBlank()) {
            return Result.failure(IllegalArgumentException("Reflection body content cannot be empty."))
        }
        if (candidate.conversationId.isBlank()) {
            return Result.failure(IllegalArgumentException("Invalid source conversation ID."))
        }
        return Result.success(true)
    }
}
