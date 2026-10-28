package com.stoicjournal.core.domain.repository

import com.stoicjournal.core.domain.model.ReflectionCandidate

interface ReflectionCandidateCache {
    fun put(candidate: ReflectionCandidate)
    fun get(candidateId: String): ReflectionCandidate?
    fun remove(candidateId: String)
}
