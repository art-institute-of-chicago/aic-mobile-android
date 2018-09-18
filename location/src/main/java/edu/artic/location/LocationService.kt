package edu.artic.location

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

interface LocationService {

    sealed class AuthorizationStatus {
        /**
         * User has given permission to use location services for this app
         */
        object LocationAllowed : AuthorizationStatus()

        /**
         * User has denied permissions to use location services for this app
         */
        data class LocationDenied(val shouldRequestRationale: Boolean, val fromRequest: Boolean) : AuthorizationStatus()
    }

    val hasRequestedPermissionAlready: Observable<Boolean>
    val authorizationStatusDistinct: Observable<AuthorizationStatus>
    val deviceLocationEnabledDistinct: Observable<Boolean>

    fun requestLocationPermissions(): Boolean
}

class LocationServiceImpl(
        private val app: Context,
        private val locationPreferenceManager: LocationPreferenceManager)
    : LocationService, ActivityCompat.OnRequestPermissionsResultCallback {

    companion object {
        const val LOCATION_PERMISSION_REQUEST = 1000
    }

    private var permissionRequested: Boolean
        get() = locationPreferenceManager.hasRequestedPermissionOnce
        set(value) {
            locationPreferenceManager.hasRequestedPermissionOnce = value
            (hasRequestedPermissionAlready as BehaviorSubject).onNext(value)
        }

    private val authorizationStatus: Subject<LocationService.AuthorizationStatus> = BehaviorSubject.create()

    private val deviceLocationEnabled: Subject<Boolean> = BehaviorSubject.create()

    override var hasRequestedPermissionAlready: Observable<Boolean> = BehaviorSubject.createDefault(permissionRequested)

    override val authorizationStatusDistinct: Observable<LocationService.AuthorizationStatus> = authorizationStatus.distinctUntilChanged()

    override val deviceLocationEnabledDistinct: Observable<Boolean> = deviceLocationEnabled.distinctUntilChanged()

    var currentActivity: Activity? = null

    private var locationModeChangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            calculateAndUpdateStatus()
        }
    }

    init {
        val app = app as Application
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityPaused(activity: Activity?) {

            }

            override fun onActivityResumed(activity: Activity?) {
                currentActivity = activity
                calculateAndUpdateStatus()
            }

            override fun onActivityStarted(activity: Activity?) {

            }

            override fun onActivityDestroyed(activity: Activity?) {

            }

            override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {

            }

            override fun onActivityStopped(activity: Activity?) {

            }

            override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {

            }

        })
        app.registerReceiver(
                locationModeChangeReceiver,
                IntentFilter("android.location.PROVIDERS_CHANGED")
        )
        calculateAndUpdateStatus(false)
    }

    private fun calculateAndUpdateStatus(fromRequest: Boolean = false) {
        var currentMode = Settings.Secure.LOCATION_MODE_OFF
        try {
            currentMode = Settings.Secure.getInt(
                    app.contentResolver,
                    Settings.Secure.LOCATION_MODE
            )
        } catch (ignored: Exception) {

        }

        val locationEnabled = currentMode != Settings.Secure.LOCATION_MODE_OFF
        deviceLocationEnabled.onNext(locationEnabled)

        val permissionGranted = ContextCompat
                .checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

        var canAsk = true
        if (permissionRequested) {
            currentActivity?.let {
                canAsk = ActivityCompat.shouldShowRequestPermissionRationale(
                        it,
                        Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
        }

        if (!permissionGranted) {
            authorizationStatus.onNext(LocationService.AuthorizationStatus.LocationDenied(canAsk, fromRequest))

        } else {
            authorizationStatus.onNext(LocationService.AuthorizationStatus.LocationAllowed)
        }
    }

    override fun requestLocationPermissions(): Boolean {
        currentActivity?.let { activity ->
            ActivityCompat.requestPermissions(
                    activity,
                    Array(1) { Manifest.permission.ACCESS_FINE_LOCATION },
                    LOCATION_PERMISSION_REQUEST
            )
            return true
        }
        return false
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        permissionRequested = true
        calculateAndUpdateStatus(true)
    }

}