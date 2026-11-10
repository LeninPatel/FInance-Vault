package com.example.domain.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class CapturedLocation(
    val latitude: Double,
    val longitude: Double,
    val placeName: String
)

object LocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocation(context: Context): CapturedLocation? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission(context)) return@withContext null

        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val location: Location? = runCatching {
            suspendCancellableCoroutine<Location?> { cont ->
                val cts = CancellationTokenSource()
                fusedClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cts.token
                ).addOnSuccessListener { loc ->
                    if (cont.isActive) cont.resume(loc)
                }.addOnFailureListener {
                    if (cont.isActive) cont.resume(null)
                }
                cont.invokeOnCancellation { cts.cancel() }
            }
        }.getOrNull() ?: runCatching {
            suspendCancellableCoroutine<Location?> { cont ->
                fusedClient.lastLocation
                    .addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) }
                    .addOnFailureListener { if (cont.isActive) cont.resume(null) }
            }
        }.getOrNull() ?: runCatching {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }.getOrNull()

        if (location != null) {
            val place = reverseGeocodePlaceName(context, location.latitude, location.longitude)
            return@withContext CapturedLocation(
                latitude = location.latitude,
                longitude = location.longitude,
                placeName = place
            )
        }

        CapturedLocation(
            latitude = 28.6139,
            longitude = 77.2090,
            placeName = "Connaught Place, New Delhi"
        )
    }

    @Suppress("DEPRECATION")
    fun reverseGeocodePlaceName(context: Context, lat: Double, lng: Double): String {
        return runCatching {
            if (!Geocoder.isPresent()) {
                return@runCatching formatCoordinateFallback(lat, lng)
            }
            val geocoder = Geocoder(context, Locale("en", "IN"))
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            val addr = addresses?.firstOrNull()
            if (addr != null) {
                val parts = listOfNotNull(
                    addr.subLocality?.takeIf { it.isNotBlank() },
                    addr.locality?.takeIf { it.isNotBlank() } ?: addr.subAdminArea?.takeIf { it.isNotBlank() },
                    addr.adminArea?.takeIf { it.isNotBlank() }
                ).distinct()
                if (parts.isNotEmpty()) {
                    parts.take(2).joinToString(", ")
                } else {
                    addr.featureName ?: formatCoordinateFallback(lat, lng)
                }
            } else {
                formatCoordinateFallback(lat, lng)
            }
        }.getOrDefault(formatCoordinateFallback(lat, lng))
    }

    private fun formatCoordinateFallback(lat: Double, lng: Double): String {
        return String.format(Locale.US, "GPS %.4f°N, %.4f°E", lat, lng)
    }
}
