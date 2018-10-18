package edu.artic.analytics

import android.content.Context
import dagger.Module
import dagger.Provides
import edu.artic.localization.LanguageSelector
import edu.artic.location.LocationService
import edu.artic.membership.MemberInfoPreferencesManager
import javax.inject.Singleton

/**
 * Description:
 */
@Module
class AnalyticsModule {

    @Provides
    @Singleton
    fun analyticsTracker(context: Context,
                         languageSelector: LanguageSelector,
                         analyticsConfig: AnalyticsConfig,
                         locationService: LocationService):
            AnalyticsTracker = AnalyticsTrackerImpl(context,
                languageSelector,
                MemberInfoPreferencesManager(context),
                locationService,
                analyticsConfig)
}