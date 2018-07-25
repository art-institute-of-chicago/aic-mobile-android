package edu.artic.analytics

import android.content.Context
import com.google.android.gms.analytics.GoogleAnalytics
import com.google.android.gms.analytics.HitBuilders

/**
 * Description:
 */
interface AnalyticsTracker {

    fun reportEvent(category: String, action: String = "", label: String = "")

    fun reportEvent(categoryName: ScreenCategoryName, action: String = "", label: String = "") =
            reportEvent(categoryName.screenName, action, label)

    fun reportScreenView(name: String)

    fun reportScreenView(categoryName: ScreenCategoryName) = reportScreenView(categoryName.screenName)
}

class AnalyticsTrackerImpl(context: Context,
                           private val analyticsConfig: AnalyticsConfig) : AnalyticsTracker {

    private val analytics = GoogleAnalytics.getInstance(context)
    private val tracker = analytics.newTracker(analyticsConfig.trackingId)

    override fun reportEvent(category: String, action: String, label: String) {
        tracker.send(HitBuilders.EventBuilder()
                .setCategory(category)
                .setCustomDimension(1, analyticsConfig.screenDimen)
                .setAction(action)
                .setLabel(label).build())
    }


    override fun reportScreenView(name: String) {
        tracker.apply {
            setScreenName(name)
            send(HitBuilders.ScreenViewBuilder()
                    .setCustomDimension(1, analyticsConfig.screenDimen)
                    .build())
        }
    }
}