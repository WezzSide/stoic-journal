package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.model.Streak
import javax.inject.Inject

class GetStreakInsightsUseCase @Inject constructor() {
    operator fun invoke(streak: Int): String {
        return when {
            streak == 0 -> "A journey of a thousand miles begins with a single step. Start your reflection today."
            streak < 3 -> "Consistency is the soul of character. You are building a new habit."
            streak < 7 -> "Well begun is half done. Your Stoic practice is taking root."
            streak < 14 -> "Self-mastery is the ultimate freedom. A week of consistent reflection is a great achievement."
            else -> "Virtue is a habit of the mind, consistent with nature and reason. Your discipline is inspiring."
        }
    }
}
