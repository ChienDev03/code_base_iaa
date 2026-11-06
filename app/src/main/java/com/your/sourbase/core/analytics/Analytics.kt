package com.your.sourbase.core.analytics
/** Feature-owned approved event contracts delegate to Firebase or the SDK-free implementation. */
interface AnalyticsProvider { fun approvedEvent(name: String, parameters: Map<String, String>) }
