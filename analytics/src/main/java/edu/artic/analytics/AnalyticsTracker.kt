package edu.artic.analytics

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import edu.artic.localization.LanguageSelector
import edu.artic.localization.nameOfLanguageForAnalytics
import edu.artic.location.LocationService
import edu.artic.location.isLocationInMuseum
import edu.artic.membership.MemberInfoPreferencesManager
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import java.util.*

/**
 * Description:
 */

interface AnalyticsTracker {

    fun clearSession()

    fun reportCustomEvent(category: String, parameterMap: Map<String, String>)

    fun reportCustomEvent(category: EventCategoryName, parameterMap: Map<String, String>) =
            reportCustomEvent(category.eventCategoryName, parameterMap)

    fun reportEvent(category: String, action: String = "", label: String = "")

    fun reportEvent(categoryName: ScreenName, action: String = "", label: String = "") =
            reportEvent(categoryName.screenName, action, label)

    fun reportEvent(categoryName: EventCategoryName, action: String = "", label: String = "") =
            reportEvent(categoryName.eventCategoryName, action, label)

    fun reportScreenView(activity: Activity, name: String)

    fun reportScreenView(activity: Activity, categoryName: ScreenName) = reportScreenView(activity, categoryName.screenName)
}

class AnalyticsTrackerImpl(context: Context,
                           private val languageSelector: LanguageSelector,
                           private val membershipPrefs: MemberInfoPreferencesManager,
                           private val locationService: LocationService) : AnalyticsTracker {

    private var atMusuem = false
    private var reportLocationAnalytic: Subject<Boolean> = BehaviorSubject.createDefault(true)
    private val firebaseAnalytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(context)

    init {
        locationService.requestTrackingUserLocation()
        val isInMuseum = locationService.currentUserLocation.map{isLocationInMuseum(it)}
        isInMuseum
                .distinctUntilChanged()
                .subscribe {
                    atMusuem = it
                }

        Observables.combineLatest(isInMuseum, reportLocationAnalytic)
                .filter { (inMuseum, shouldReport) ->
                    inMuseum && shouldReport
                }.subscribe {
                    reportLocationAnalytic.onNext(false)
                    reportEvent(EventCategoryName.Location, AnalyticsAction.locationOnSite)
                }
    }

    override fun clearSession() {
        reportLocationAnalytic.onNext(true)
    }

    override fun reportCustomEvent(category: String, parameterMap: Map<String, String>) {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_CATEGORY, category)

        for((paramName, paramValue) in parameterMap) {
            if(paramValue.isBlank()) {
                Log.w(AnalyticsTracker::class.qualifiedName, "reportCustomEvent(): value for parameter \"$paramName\" is blank, not adding to Bundle.")
            } else {
                bundle.putString(paramName, paramValue)
            }
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
    }

    /*
        Commenting out old Google Analytics code below but leaving here
        because we will need to reference it for our new Firebase Analytics
        custom events.
     */
    override fun reportEvent(category: String, action: String, label: String) {
        Log.d(AnalyticsTracker::class.qualifiedName, "reportEvent(): Logging No-op for this function.")
//        val memberId = membershipPrefs.memberID
//        tracker.send(HitBuilders.EventBuilder()
//                .setCategory(category)
//                .setAction(action)
//                .setCustomDimension(2, (if (memberId.isNullOrBlank()) "None" else "Member"))
//                .setCustomDimension(3, languageSelector.getAppLocale().nameOfLanguageForAnalytics())
//                .setCustomDimension(4, Locale.getDefault().toLanguageTag())
//                .setCustomDimension(5, atMusuem.toString())
//                .setLabel(label).build())
    }

    /*
        Commenting out old Google Analytics code below but leaving here
        because we will need to reference it for our new Firebase Analytics
        custom events.
     */
    override fun reportScreenView(activity: Activity, name: String) {
        firebaseAnalytics.setCurrentScreen(activity, name, null)
//        val memberId = membershipPrefs.memberID
//        tracker.apply {
//            setScreenName(name)
//            send(HitBuilders.ScreenViewBuilder()
//                    .setCustomDimension(2, (if (memberId.isNullOrBlank()) "None" else "Member"))
//                    .setCustomDimension(3, languageSelector.getAppLocale().nameOfLanguageForAnalytics())
//                    .setCustomDimension(4, Locale.getDefault().toLanguageTag())
//                    .setCustomDimension(5, atMusuem.toString())
//                    .build())
//        }
    }
}