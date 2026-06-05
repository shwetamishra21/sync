package com.jsac.sync.utils

import android.Manifest
import android.content.Context
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.jsac.sync.data.remote.dto.GpsLocation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Helper for getting device GPS location
 * Used to include GPS coordinates with form submissions
 *
 * DEPENDENCIES:
 * Add to build.gradle:
 * implementation 'com.google.android.gms:play-services-location:21.0.1'
 *
 * PERMISSIONS:
 * Add to AndroidManifest.xml:
 * <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
 * <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
 *
 * NOTE: Request runtime permissions using RequestPermission launcher in Activity/Fragment
 */
class LocationHelper(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) {

    /**
     * Get current device location
     * Returns null if unable to determine location
     *
     * @return GpsLocation with latitude/longitude or null
     */
    suspend fun getCurrentLocation(): GpsLocation? = suspendCancellableCoroutine { continuation ->
        try {
            // Check for location permissions
            if (!hasLocationPermission()) {
                Log.w("LocationHelper", "⚠️ Location permissions not granted")
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            // Create location request with high accuracy
            val locationRequest = LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                numUpdates = 1  // Get only one location update
                maxWaitTime = 10000  // Max 10 seconds
            }

            // Request location updates
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        super.onLocationResult(locationResult)

                        val location = locationResult.lastLocation

                        if (location != null) {
                            Log.d(
                                "LocationHelper",
                                "📍 Location obtained: ${location.latitude}, ${location.longitude}"
                            )

                            val gpsLocation = GpsLocation(
                                latitude = location.latitude,
                                longitude = location.longitude
                            )

                            continuation.resume(gpsLocation)
                        } else {
                            Log.w("LocationHelper", "⚠️ Location is null")
                            continuation.resume(null)
                        }

                        // Stop location updates
                        fusedLocationClient.removeLocationUpdates(this)
                    }
                },
                Looper.getMainLooper()
            )

            // Timeout after 15 seconds
            continuation.invokeOnCancellation {
                fusedLocationClient.removeLocationUpdates { }
            }

        } catch (e: SecurityException) {
            Log.e("LocationHelper", "❌ Security exception: ${e.message}")
            continuation.resume(null)
        } catch (e: Exception) {
            Log.e("LocationHelper", "❌ Exception: ${e.message}", e)
            continuation.resume(null)
        }
    }

    /**
     * Check if location permissions are granted
     */
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}