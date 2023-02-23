package edu.artic

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.jakewharton.threetenabp.AndroidThreeTen
import dagger.android.DaggerApplication
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.EventCategoryName
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
        } else {
            Timber.plant(CrashlyticsTree())
        }

        AndroidThreeTen.init(this)
        FirebaseApp.initializeApp(this, firebaseOptions())
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun applicationInjector() = seedBuilder(DaggerAppComponent.builder())

    fun firebaseOptions(): FirebaseOptions {
        return FirebaseOptions.Builder()
            .setApiKey(BuildConfig.FB_API_KEY)
            .setApplicationId(BuildConfig.FB_APPLICATION_ID)
            .setGaTrackingId(BuildConfig.GA_TRACKING_ID)
            .setGcmSenderId(BuildConfig.GCM_SENDER_ID)
            .setProjectId(BuildConfig.FB_PROJECT_ID)
            .setStorageBucket(BuildConfig.FB_STORAGE_BUCKET)
            .build()
    }

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