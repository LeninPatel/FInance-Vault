package com.example.domain.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.roundToInt

data class CapturedLocation(
    val latitude: Double,
    val longitude: Double,
    val placeName: String,
    val streetAndArea: String = "",
    val coordinatesText: String = "",
    val accuracyMeters: Float? = null
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

        // 1. Request live high-accuracy fix first (street-level GPS precision)
        val highAccuracyLocation: Location? = withTimeoutOrNull(4_000L) {
            runCatching {
                suspendCancellableCoroutine<Location?> { cont ->
                    val cts = CancellationTokenSource()
                    val request = CurrentLocationRequest.Builder()
                        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                        .setMaxUpdateAgeMillis(8_000L)
                        .setDurationMillis(3_500L)
                        .build()
                    fusedClient.getCurrentLocation(request, cts.token)
                        .addOnSuccessListener { loc ->
                            if (cont.isActive) cont.resume(loc)
                        }
                        .addOnFailureListener {
                            if (cont.isActive) cont.resume(null)
                        }
                    cont.invokeOnCancellation { cts.cancel() }
                }
            }.getOrNull()
        }

        // 2. Fallback to balanced accuracy or freshest provider fix if indoor / satellite lock timed out
        val fallbackLocation: Location? = highAccuracyLocation ?: withTimeoutOrNull(2_500L) {
            runCatching {
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
            }.getOrNull()
        } ?: runCatching {
            suspendCancellableCoroutine<Location?> { cont ->
                fusedClient.lastLocation
                    .addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) }
                    .addOnFailureListener { if (cont.isActive) cont.resume(null) }
            }
        }.getOrNull() ?: getBestLastKnownSystemLocation(context)

        if (fallbackLocation != null) {
            val lat = fallbackLocation.latitude
            val lng = fallbackLocation.longitude
            val acc = if (fallbackLocation.hasAccuracy()) fallbackLocation.accuracy else null
            val streetArea = reverseGeocodeStreetAndArea(context, lat, lng)
            val coordsText = formatCoordinatesCompact(lat, lng, acc)
            val fullPlace = if (streetArea.isNotBlank()) {
                "$streetArea ($coordsText)"
            } else {
                coordsText
            }
            return@withContext CapturedLocation(
                latitude = lat,
                longitude = lng,
                placeName = fullPlace,
                streetAndArea = streetArea,
                coordinatesText = coordsText,
                accuracyMeters = acc
            )
        }

        null
    }

    @SuppressLint("MissingPermission")
    private fun getBestLastKnownSystemLocation(context: Context): Location? {
        return runCatching {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )
            providers.mapNotNull { provider ->
                runCatching { lm.getLastKnownLocation(provider) }.getOrNull()
            }.maxByOrNull { loc ->
                // Prefer fresher and more accurate fixes
                val accuracyScore = if (loc.hasAccuracy()) (1000f - loc.accuracy.coerceAtMost(1000f)).toLong() else 0L
                loc.time + accuracyScore * 100L
            }
        }.getOrNull()
    }

    /**
     * Extracts street/thoroughfare, landmark/premise, area/sub-locality, and city/locality.
     */
    @Suppress("DEPRECATION")
    fun reverseGeocodeStreetAndArea(context: Context, lat: Double, lng: Double): String {
        return runCatching {
            if (!Geocoder.isPresent()) return@runCatching ""
            val geocoder = Geocoder(context, Locale("en", "IN"))
            val addresses = geocoder.getFromLocation(lat, lng, 3)
            val addr = addresses?.firstOrNull() ?: return@runCatching ""

            val streetPart = buildList {
                val subThoroughfare = addr.subThoroughfare?.trim()?.takeIf { it.isNotBlank() }
                val thoroughfare = addr.thoroughfare?.trim()?.takeIf { it.isNotBlank() }
                val premises = addr.premises?.trim()?.takeIf { it.isNotBlank() }
                val feature = addr.featureName?.trim()?.takeIf {
                    it.isNotBlank() &&
                        !it.matches(Regex("^[0-9+\\-.,\\s]+$")) &&
                        it != subThoroughfare &&
                        it != thoroughfare &&
                        it != addr.subLocality &&
                        it != addr.locality
                }
                if (premises != null) add(premises)
                else if (feature != null) add(feature)
                if (subThoroughfare != null && thoroughfare != null) {
                    add("$subThoroughfare $thoroughfare")
                } else if (thoroughfare != null) {
                    add(thoroughfare)
                }
            }.distinct().joinToString(", ")

            val areaPart = listOfNotNull(
                addr.subLocality?.trim()?.takeIf { it.isNotBlank() },
                addr.locality?.trim()?.takeIf { it.isNotBlank() }
                    ?: addr.subAdminArea?.trim()?.takeIf { it.isNotBlank() },
                addr.adminArea?.trim()?.takeIf { it.isNotBlank() && addr.locality.isNullOrBlank() }
            ).distinct().joinToString(", ")

            listOf(streetPart, areaPart)
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(", ")
        }.getOrDefault("")
    }

    @Suppress("DEPRECATION")
    fun reverseGeocodePlaceName(context: Context, lat: Double, lng: Double): String {
        val streetAndArea = reverseGeocodeStreetAndArea(context, lat, lng)
        val coords = formatCoordinatesCompact(lat, lng, null)
        return if (streetAndArea.isNotBlank()) {
            "$streetAndArea ($coords)"
        } else {
            coords
        }
    }

    fun formatCoordinatesCompact(lat: Double, lng: Double, accuracyMeters: Float? = null): String {
        val base = String.format(Locale.US, "%.5f, %.5f", lat, lng)
        return if (accuracyMeters != null && accuracyMeters > 0f) {
            "$base ±${accuracyMeters.roundToInt()}m"
        } else {
            base
        }
    }

    /**
     * Combines an optional user-provided or spoken merchant/place name with live GPS
     * street, area, and coordinates so every transaction carries both context and exact GPS.
     */
    fun mergeLocationWithLiveGps(
        customOrSpokenPlace: String,
        captured: CapturedLocation?
    ): String {
        val trimmedCustom = customOrSpokenPlace.trim()
        if (captured == null) return trimmedCustom
        val gpsFull = captured.placeName.trim()
        if (trimmedCustom.isBlank()) return gpsFull
        if (trimmedCustom.equals(gpsFull, ignoreCase = true) ||
            trimmedCustom.contains(captured.coordinatesText, ignoreCase = true)
        ) {
            return trimmedCustom
        }
        return "$trimmedCustom • $gpsFull"
    }
}

