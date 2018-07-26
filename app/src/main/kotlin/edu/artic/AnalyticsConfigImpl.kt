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

    /**
     * TODO:: update real tracking id
     * Uses dev account for development purpose.
     * Dev account is registered under android@fuzzproductions.com
     */
    override val trackingId: String
        get() = "UA-122937378-1"
}