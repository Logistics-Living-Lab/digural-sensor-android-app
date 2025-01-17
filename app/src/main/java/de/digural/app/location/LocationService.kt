package de.digural.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import de.digural.app.AppConstants
import de.digural.app.location.event.LocationUpdateEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.joda.time.DateTime

class LocationService() {
    private val LOG_TAG: String = LocationService::class.java.getName()

    private lateinit var mLocationCallback: LocationCallback
    private var mFusedLocationClient: FusedLocationProviderClient? = null

    private val mLocationRequest = LocationRequest.create()
        .setInterval(de.digural.app.AppConstants.LOCATION_INTERVAL_IN_SECONDS * 1000L)
        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

    private var locations: MutableList<LocationValue> = ArrayList()

    private var coroutineScope: CoroutineScope? = null

    init {
        de.digural.app.AppConstants.LOCATION_MIN_DISTANCE?.let {
            mLocationRequest.setSmallestDisplacement(it)
        }
    }

    fun addLocation(location: LocationValue) {
        this.locations.add(location)
    }

    fun getCurrentLocation(): LocationValue? {
        if (locations.isEmpty()) {
            return null
        }
        return this.locations.last()
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(context: Context) {
        Log.i(LOG_TAG, "startLocationUpdates()")

        coroutineScope = CoroutineScope(Dispatchers.Default)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                coroutineScope?.launch {
                    super.onLocationResult(locationResult)

                    locationResult.lastLocation?.let { lastLocation ->
                        val location = LocationValue(
                            lastLocation.latitude,
                            lastLocation.longitude,
                            lastLocation.provider ?: "null",
                            DateTime(lastLocation.time),
                            lastLocation.accuracy,
                            lastLocation.speed,
                            lastLocation.bearing,
                            lastLocation.altitude,
                        )

                        val locationUpdateEvent = LocationUpdateEvent(location)
                        addLocation(location)
                        EventBus.getDefault().post(locationUpdateEvent)
                        Log.i(
                            LOG_TAG,
                            "${Thread.currentThread().name} | ${lastLocation.provider} [${lastLocation.longitude}; ${lastLocation.latitude}]"
                        )
                    }
                }
            }
        }

        // Don't need looper when using Kotlin coroutines
        mFusedLocationClient?.requestLocationUpdates(
            mLocationRequest,
            mLocationCallback,
            Looper.myLooper()!!
        )
    }

    fun stopLocationUpdates() {
        mFusedLocationClient?.removeLocationUpdates(mLocationCallback)
        coroutineScope?.cancel()
        coroutineScope = null
    }


}