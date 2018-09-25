package edu.artic.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

class FuzedLocationProvider(private val context: Context) : LocationProvider {

    private var locationRequest: LocationRequest? = null

    private val fuzedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    private val locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            super.onLocationResult(locationResult)
            locationResult?.let {
                currentLocation.onNext(it.lastLocation)
            }
        }
    }

    override val currentLocation: Subject<Location> = BehaviorSubject.create()

    override val isTrackingLocationChanges: Subject<Boolean> = BehaviorSubject.createDefault(false)

    private var isTracking: Boolean = false
        set(value) {
            field = value
            isTrackingLocationChanges.onNext(value)
        }

    private val handler = Handler(Looper.getMainLooper())
    private var trackingRequested = false

    init {
        val app = context as Application

        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {

            override fun onActivityPaused(activity: Activity?) {
                if (trackingRequested) {
                    stopLocationTracking()
                }
            }

            override fun onActivityResumed(activity: Activity?) {
                if (trackingRequested) {
                    startLocationTracking()
                }
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
        locationRequest = LocationRequest().apply {
            this.interval = 1000
            this.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    override fun startLocationTracking() {
        trackingRequested = true
        handler.removeCallbacks(stopTrackingRunnable)
        if (!isTracking && isPermissionGranted()) {
            fuzedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
            isTracking = true
        }
    }

    override fun stopLocationTracking() {
        handler.postDelayed(stopTrackingRunnable, 500)
    }

    private val stopTrackingRunnable: Runnable = Runnable {
        trackingRequested = false
        if (isTracking) {
            fuzedLocationClient.removeLocationUpdates(locationCallback)
            isTracking = false
        }
    }


}