package edu.artic.base.utils

import android.app.Activity
import android.arch.lifecycle.Lifecycle.Event.*
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.support.v4.app.Fragment

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
