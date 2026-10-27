package com.stoicjournal.core.domain.util

import com.stoicjournal.core.domain.model.AiReflectionRequest
import com.stoicjournal.core.domain.model.AiReflectionTheme

object StoicPromptTemplate {
    fun buildSystemPrompt(): String {
        return """
            You are a Stoic philosopher and guide, deeply versed in the works of Marcus Aurelius, Epictetus, and Seneca.
            Your mission is to help the user navigate their daily life using Stoic principles.
            
            When providing a reflection:
            1. Be objective and calm.
            2. Focus on the 'Dichotomy of Control' — distinguish what is within the user's control and what is not.
            3. Encourage virtue, resilience, and rational judgment.
            4. Provide practical, actionable advice.
            
            Return the response strictly in JSON format as specified in the request.
        """.trimIndent()
    }

    fun buildUserPrompt(request: AiReflectionRequest): String {
        val themeContext = when (request.theme) {
            AiReflectionTheme.ANXIETY -> "Focus on the present moment and the realization that future troubles have not yet arrived."
            AiReflectionTheme.DISCIPLINE -> "Focus on consistency, self-mastery, and doing what duty requires regardless of mood."
            AiReflectionTheme.ANGER -> "Focus on the destructive nature of anger and the power of patience and understanding."
            AiReflectionTheme.GRIEF -> "Focus on the natural cycle of life and death, and cherishing what we have while we have it."
            AiReflectionTheme.FOCUS -> "Focus on eliminating distractions and performing the task at hand with full attention."
            AiReflectionTheme.GRATITUDE -> "Focus on appreciating what nature has provided and the simple joy of existence."
            AiReflectionTheme.DECISION_MAKING -> "Focus on rational deliberation and aligning choices with virtue."
            AiReflectionTheme.SELF_CONTROL -> "Focus on resisting impulsive desires and acting according to reason."
        }

        val moodContext = if (request.mood != null) "The user's current mood is: ${request.mood}." else ""
        val journalContext = if (request.journalContext != null) "Previous context: ${request.journalContext}" else ""

        return """
            Please reflect on the following user thought:
            "${request.content}"
            
            Theme of reflection: ${request.theme.displayName}
            Specific guidance for this theme: $themeContext
            $moodContext
            $journalContext
            
            Return the response in the following JSON format:
            {
              "insight": "A short Stoic insight (max 2 sentences)",
              "interpretation": "A practical interpretation of how Stoicism applies to this thought",
              "reflectiveQuestion": "One deep question for the user to contemplate",
              "actionableStep": "One concrete small action to take today",
              "quote": "A relevant quote from a Stoic philosopher",
              "principle": "The name of the core Stoic principle applied (e.g., Amor Fati, Memento Mori)"
            }
        """.trimIndent()
    }
}
