package edu.artic

import edu.artic.analytics.AnalyticsConfig

/**
@author Sameer Dhakal (Fuzz)
 */
class AnalyticsConfigImpl : AnalyticsConfig {
    override val screenDimen: String
        get() = if (BuildConfig.DEBUG) "Dev" else "Live"
    override val enabled: Boolean
        get() = true
    override val trackingId: String
        get() = "UA-122937378-1"
}