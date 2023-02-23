package edu.artic.base.utils

import android.app.Activity
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.Event.ON_STOP
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

/**
 *
 * @author Sameer Dhakal (Fuzz)
 */
interface LifecycleAware<T> : LifecycleObserver {

    @OnLifecycleEvent(ON_START)
    fun onStart(host: T)

    @OnLifecycleEvent(ON_STOP)
    fun onStop(host: T)

}

interface LifecycleAwareActivity : LifecycleAware<Activity>
