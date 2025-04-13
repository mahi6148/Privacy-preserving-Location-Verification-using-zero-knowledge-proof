package com.example.majorapplication.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume


suspend fun fetchLocationAndCoordinates(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient
): Pair<Double?, Double?> = withContext(Dispatchers.IO) {
    return@withContext suspendCancellableCoroutine { continuation ->
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .apply {
                setMinUpdateIntervalMillis(5000)
                setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                setWaitForAccurateLocation(true)
            }.build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                location?.let {
                    fusedLocationClient.removeLocationUpdates(this)
                    continuation.resume(Pair(it.latitude, it.longitude))
                }
            }
        }

        // Fallback if no location update received
        val timeoutJob = CoroutineScope(Dispatchers.IO).launch {
            delay(5000) // 15 seconds timeout
            if (!continuation.isCompleted) {
                fusedLocationClient.removeLocationUpdates(locationCallback)
                continuation.resume(Pair(null, null))
            }
        }

        continuation.invokeOnCancellation {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            timeoutJob.cancel()
        }

        try {
            if (ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    context.mainLooper
                )
            }
        } catch (e: Exception) {
            continuation.resume(Pair(null, null))
        }
    }
}
