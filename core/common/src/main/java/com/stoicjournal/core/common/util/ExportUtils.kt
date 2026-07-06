package com.stoicjournal.core.common.util

import com.stoicjournal.core.domain.model.JournalEntry
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ExportUtils {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(ZoneId.systemDefault())

    fun entriesToMarkdown(entries: List<JournalEntry>): String {
        val sb = StringBuilder()
        sb.append("# Stoic Journal Export\n\n")
        sb.append("Generated on: ${dateFormatter.format(java.time.Instant.now())}\n\n")
        sb.append("---\n\n")

        entries.forEach { entry ->
            val dateStr = dateFormatter.format(entry.createdAt)
            sb.append("## ${entry.title ?: "Untitled Entry"}\n")
            sb.append("**Date:** $dateStr\n")
            sb.append("**Type:** ${entry.type.name}\n\n")
            sb.append("${entry.body}\n\n")
            sb.append("---\n\n")
        }

        return sb.toString()
    }
}
