package com.stoicjournal.core.common.notifications

/**
 * Contract between notification producers (core:notifications) and the single-activity shell
 * (app) for in-app deep links carried on the launch Intent. Only route *names* travel through
 * the Intent; the shell decides what to render, and the app-lock gate stays in front of it.
 */
object NotificationRoutes {
    const val EXTRA_ROUTE = "com.stoicjournal.extra.ROUTE"
    const val ROUTE_MENTOR_CHAT = "mentor_chat"
}
