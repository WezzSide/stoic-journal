package com.stoicjournal.feature.ai.mentor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Renders the small Markdown subset LLMs commonly emit in chat replies, so users don't see raw
 * asterisks: **bold** / __bold__, *italic* / _italic_, `# headings` (bold line), and "- " / "* " / "• "
 * bullets (rendered as "•"). Numbered lists pass through unchanged. Anything else is shown literally.
 * Unmatched markers are kept as plain text. The text is treated as untrusted display data only.
 */
fun mentorMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    val lines = text.replace("\r\n", "\n").split("\n")
    lines.forEachIndexed { index, rawLine ->
        val heading = Regex("^\\s{0,3}#{1,6}\\s+(.*)$").find(rawLine)
        val bullet = Regex("^(\\s*)[-*•]\\s+(.*)$").find(rawLine)
        when {
            heading != null -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                appendInline(heading.groupValues[1].trimEnd('#', ' '))
            }
            bullet != null -> {
                append(bullet.groupValues[1])
                append("•  ")
                appendInline(bullet.groupValues[2])
            }
            else -> appendInline(rawLine)
        }
        if (index < lines.lastIndex) append('\n')
    }
}

private fun AnnotatedString.Builder.appendInline(line: String) {
    var i = 0
    while (i < line.length) {
        val bold = when {
            line.startsWith("**", i) -> "**"
            line.startsWith("__", i) -> "__"
            else -> null
        }
        if (bold != null) {
            val end = line.indexOf(bold, i + 2)
            if (end > i + 2) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { appendInline(line.substring(i + 2, end)) }
                i = end + 2
                continue
            }
        }
        val c = line[i]
        if ((c == '*' || c == '_') && i + 1 < line.length && !line[i + 1].isWhitespace() && isWordBoundaryBefore(line, i)) {
            val end = findClosingItalic(line, i + 1, c)
            if (end > i + 1) {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(line.substring(i + 1, end)) }
                i = end + 1
                continue
            }
        }
        append(c)
        i++
    }
}

/** `_` / `*` only opens emphasis at a word start, so snake_case and 2*3*4 stay literal. */
private fun isWordBoundaryBefore(line: String, i: Int) = i == 0 || !line[i - 1].isLetterOrDigit()

private fun findClosingItalic(line: String, from: Int, marker: Char): Int {
    var j = from
    while (j < line.length) {
        if (line[j] == marker && !line[j - 1].isWhitespace() && (j + 1 == line.length || !line[j + 1].isLetterOrDigit())) return j
        j++
    }
    return -1
}
