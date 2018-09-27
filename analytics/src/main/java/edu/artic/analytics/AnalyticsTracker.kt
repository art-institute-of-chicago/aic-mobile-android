package edu.artic.analytics

import android.content.Context
import com.google.android.gms.analytics.GoogleAnalytics
import com.google.android.gms.analytics.HitBuilders
import edu.artic.localization.LanguageSelector
import edu.artic.localization.nameOfLanguageForAnalytics
import edu.artic.location.LocationService
import edu.artic.location.isLocationInMuseum
import edu.artic.membership.MemberInfoPreferencesManager
import java.util.*

/**
 * Description:
 */
interface AnalyticsTracker {

    fun clearSession()

    fun reportEvent(category: String, action: String = "", label: String = "")

    fun reportEvent(categoryName: ScreenCategoryName, action: String = "", label: String = "") =
            reportEvent(categoryName.screenName, action, label)

    fun reportEvent(categoryName: EventCategoryName, action: String = "", label: String = "") =
            reportEvent(categoryName.eventCategoryName, action, label)

    fun reportScreenView(name: String)

    fun reportScreenView(categoryName: ScreenCategoryName) = reportScreenView(categoryName.screenName)
}

class AnalyticsTrackerImpl(context: Context,
                           private val languageSelector: LanguageSelector,
                           private val membershipPrefs: MemberInfoPreferencesManager,
                           private val locationService: LocationService,
                           private val analyticsConfig: AnalyticsConfig) : AnalyticsTracker {

    private val analytics = GoogleAnalytics.getInstance(context)
    private val tracker = analytics.newTracker(analyticsConfig.trackingId)
    private var atMusuem = false
    private var reportLocationAnalytic = true

    init {
        locationService.requestTrackingUserLocation()
        locationService.currentUserLocation
                .subscribe() {
                    atMusuem = isLocationInMuseum(it)
                    if (atMusuem && reportLocationAnalytic) {
                        reportLocationAnalytic = false
                        reportEvent(EventCategoryName.Location, AnalyticsAction.locationOnSite)
                    }
                }.dispose()
    }

    override fun clearSession() {
        reportLocationAnalytic = true
    }

    override fun reportEvent(category: String, action: String, label: String) {
        val memberId = membershipPrefs.memberID
        tracker.send(HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .setCustomDimension(2, (if (memberId != null) "Member" else "None"))
                .setCustomDimension(3, languageSelector.getAppLocale().nameOfLanguageForAnalytics())
                .setCustomDimension(4, Locale.getDefault().toLanguageTag())
                .setCustomDimension(5, atMusuem.toString())
                .setLabel(label).build())
    }


    override fun reportScreenView(name: String) {
        val memberId = membershipPrefs.memberID
        tracker.apply {
            setScreenName(name)
            send(HitBuilders.ScreenViewBuilder()
                    .setCustomDimension(2, (if (memberId != null) "Member" else "None"))
                    .setCustomDimension(3, languageSelector.getAppLocale().nameOfLanguageForAnalytics())
                    .setCustomDimension(4, Locale.getDefault().toLanguageTag())
                    .setCustomDimension(5, atMusuem.toString())
                    .build())
        }
    }
}