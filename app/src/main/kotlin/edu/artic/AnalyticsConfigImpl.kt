package edu.artic

import edu.artic.analytics.AnalyticsConfig

/**
@author Sameer Dhakal (Fuzz)
 */
class AnalyticsConfigImpl : AnalyticsConfig {
    override val enabled: Boolean
        get() = true

    override val trackingId: String
        get() = BuildConfig.GA_TRACKING_ID
}