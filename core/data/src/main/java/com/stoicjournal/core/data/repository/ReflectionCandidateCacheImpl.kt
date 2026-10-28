package com.stoicjournal.core.data.repository

import com.stoicjournal.core.domain.model.ReflectionCandidate
import com.stoicjournal.core.domain.repository.ReflectionCandidateCache
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReflectionCandidateCacheImpl @Inject constructor() : ReflectionCandidateCache {
    private val cache = ConcurrentHashMap<String, ReflectionCandidate>()

    override fun put(candidate: ReflectionCandidate) {
        cache[candidate.candidateId] = candidate
    }

    override fun get(candidateId: String): ReflectionCandidate? {
        return cache[candidateId]
    }

    override fun remove(candidateId: String) {
        cache.remove(candidateId)
    }
}
