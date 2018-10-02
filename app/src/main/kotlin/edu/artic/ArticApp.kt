package edu.artic

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.arch.lifecycle.ProcessLifecycleOwner
import com.crashlytics.android.Crashlytics
import com.jakewharton.threetenabp.AndroidThreeTen
import dagger.android.DaggerApplication
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.EventCategoryName
import io.fabric.sdk.android.Fabric
import timber.log.Timber
import javax.inject.Inject



class ArticApp : DaggerApplication(), LifecycleObserver {

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    override fun onCreate() {
        app = this
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        AndroidThreeTen.init(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun applicationInjector() = seedBuilder(DaggerAppComponent.builder())

    fun seedBuilder(builder: AppComponent.Builder): AppComponent {
        builder.seedInstance(this)
        val component = builder.build()
        ArticComponent.setInternalAppComponent(component)
        return component
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onAppOpened() {
        analyticsTracker.reportEvent(EventCategoryName.App, AnalyticsAction.APP_OPENED)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        analyticsTracker.reportEvent(EventCategoryName.App, AnalyticsAction.APP_BACKGROUNDED)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        analyticsTracker.reportEvent(EventCategoryName.App, AnalyticsAction.APP_FOREGROUNDED)
    }


    companion object {

        @JvmStatic
        lateinit var app: ArticApp
            private set

    }
}