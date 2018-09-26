package edu.artic

import android.content.Context
import dagger.Module
import dagger.Provides
import edu.artic.analytics.AnalyticsConfig
import edu.artic.base.utils.customTab.CustomTabManager
import javax.inject.Named
import javax.inject.Singleton

/**
 * Description:
 */
@Module
class AppModule {

    @Provides
    fun provideApplication(): Context = ArticApp.app

    /**
     * This value is used by [edu.artic.info.InformationViewModel].
     */
    @Provides
    @Named(VERSION)
    fun getBuildVersion(): String = BuildConfig.VERSION_NAME_FOR_DISPLAY

    @Provides
    @Named(DISPLAY_CONFIG)
    fun getDisplayConfig(): String = BuildConfig.BUILD_TYPE

    @Provides
    @Singleton
    fun provideAnalyticsConfig(): AnalyticsConfig = AnalyticsConfigImpl()
    @Module
    companion object {

        const val VERSION = "VERSION"
        const val DISPLAY_CONFIG = "DISPLAY_CONFIG"

        @JvmStatic
        @Provides
        fun provideChromeTabManager(): CustomTabManager = CustomTabManager()
    }
}