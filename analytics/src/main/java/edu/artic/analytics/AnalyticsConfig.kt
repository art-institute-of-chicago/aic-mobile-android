package edu.artic.analytics

/**
 * Description:
 */
interface AnalyticsConfig {
    /**
     * If enabled, we forward. If not, do nothing.
     */
    val enabled: Boolean

    /**
     * Google analytics ID.
     */
    val trackingId: String
}