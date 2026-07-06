package com.stoicjournal.core.notifications.widget

import androidx.glance.appwidget.GlanceAppWidgetReceiver

class DailyQuoteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = DailyQuoteWidget()
}
