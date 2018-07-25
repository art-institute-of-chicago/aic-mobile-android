package edu.artic.analytics

/**
 * Description:
 */
interface AnalyticsConfig {

    /**
     * Not sure why this is set, but the iOS app adds this custom dimen for events depending on release or not.
     */
    val screenDimen: String

    /**
     * If enabled, we forward. If not, do nothing.
     */
    val enabled: Boolean

    /**
     * Google analytics ID.
     */
    val trackingId: String
}