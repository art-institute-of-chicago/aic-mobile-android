package edu.artic.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.content.ContextCompat
import android.view.Surface
import android.view.WindowManager
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

class FuzedLocationProvider(private val context: Context) : LocationProvider, SensorEventListener {

    private val mSensorManager: SensorManager
    private var mGravity = FloatArray(3)
    private var mGeomagnetic = FloatArray(3)
    private var defaultRotation = FloatArray(9)
    //Rotation after `remapCoordinateSystem` has been invoked to correct for device rotation
    private var correctedRotation = FloatArray(9)

    private var locationRequest: LocationRequest? = null

    private val fuzedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    fun configureDeviceAngle() {
        val windowManager = context .getSystemService(Context.WINDOW_SERVICE) as WindowManager
        when (windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_0 ->
                SensorManager.remapCoordinateSystem(defaultRotation, SensorManager.AXIS_Z,
                        SensorManager.AXIS_Y, correctedRotation)
            Surface.ROTATION_90 ->
                SensorManager.remapCoordinateSystem(defaultRotation, SensorManager.AXIS_Y,
                        SensorManager.AXIS_MINUS_Z, correctedRotation)
            Surface.ROTATION_180 ->
                SensorManager.remapCoordinateSystem(defaultRotation, SensorManager.AXIS_MINUS_Z,
                        SensorManager.AXIS_MINUS_Y, correctedRotation)
            Surface.ROTATION_270 ->
                SensorManager.remapCoordinateSystem(defaultRotation, SensorManager.AXIS_MINUS_Y,
                        SensorManager.AXIS_Z, correctedRotation)
        }
    }

    private val locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            super.onLocationResult(locationResult)
            locationResult?.let {
                val success = SensorManager.getRotationMatrix(defaultRotation, null, mGravity, mGeomagnetic)
                if (success) {
                    val orientation = FloatArray(3)
                    configureDeviceAngle()
                    SensorManager.getOrientation(correctedRotation, orientation)
                    val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    it.lastLocation.bearing = azimuth
                }
                currentLocation.onNext(it.lastLocation)
            }
        }
    }

    override val currentLocation: Subject<Location> = BehaviorSubject.create()

    val bearing: Subject<Location> = BehaviorSubject.create()

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

        mSensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
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
            mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
                mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            }
            mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
                mSensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_GAME)
            }
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
            mSensorManager.unregisterListener(this)
            isTracking = false
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            mGravity = event.values
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            mGeomagnetic = event.values
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }


}