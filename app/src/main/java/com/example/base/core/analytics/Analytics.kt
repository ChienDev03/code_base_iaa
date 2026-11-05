package com.example.base.core.analytics
/** Feature-owned approved event contracts may delegate to installed providers. None is installed by default. */
interface AnalyticsProvider { fun approvedEvent(name: String, parameters: Map<String, String>) }
