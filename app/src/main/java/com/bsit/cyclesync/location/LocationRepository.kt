package com.bsit.cyclesync.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocationRepository(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Try lastLocation first; if null, request a single high-accuracy update.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { cont ->
        // Try last location
        fusedClient.lastLocation
            .addOnSuccessListener { last ->
                if (last != null) {
                    cont.resume(last)
                } else {
                    // request single update
                    val request = LocationRequest.create().apply {
                        interval = 0
                        fastestInterval = 0
                        numUpdates = 1
                        priority = Priority.PRIORITY_HIGH_ACCURACY
                    }

                    val callback = object : LocationCallback() {
                        override fun onLocationResult(result: LocationResult) {
                            fusedClient.removeLocationUpdates(this)
                            val loc = result.lastLocation
                            cont.resume(loc)
                        }

                        override fun onLocationAvailability(p0: LocationAvailability) {
                            // keep waiting
                        }
                    }

                    fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
                        .addOnFailureListener { ex ->
                            fusedClient.removeLocationUpdates(callback)
                            cont.resumeWithException(ex)
                        }
                }
            }
            .addOnFailureListener { ex ->
                cont.resumeWithException(ex)
            }
    }
}
