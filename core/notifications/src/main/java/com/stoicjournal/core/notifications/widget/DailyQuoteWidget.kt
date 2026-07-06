package com.stoicjournal.core.notifications.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontStyle
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.stoicjournal.core.domain.repository.QuoteRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

class DailyQuoteWidget : GlanceAppWidget() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun quoteRepository(): QuoteRepository
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context,
            WidgetEntryPoint::class.java
        )
        val quoteRepository = entryPoint.quoteRepository()
        val quote = quoteRepository.getAllQuotes().first().randomOrNull()

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (quote != null) {
                        Text(
                            text = "\"${quote.text}\"",
                            style = TextStyle(
                                textAlign = TextAlign.Center,
                                fontStyle = FontStyle.Italic,
                                color = GlanceTheme.colors.onSurface
                            )
                        )
                        Text(
                            text = "— ${quote.attribution}",
                            modifier = GlanceModifier.padding(top = 8.dp),
                            style = TextStyle(
                                textAlign = TextAlign.Center,
                                color = GlanceTheme.colors.primary
                            )
                        )
                    } else {
                        Text(text = "Focus on what you can control.")
                    }
                }
            }
        }
    }
}
