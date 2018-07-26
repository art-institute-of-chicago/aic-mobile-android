package edu.artic.analytics

import android.content.Context
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Description:
 */
@Module
class AnalyticsModule {

    @Provides
    @Singleton
    fun analyticsTracker(context: Context, analyticsConfig: AnalyticsConfig): AnalyticsTracker =
            AnalyticsTrackerImpl(context, analyticsConfig)
}