package com.onthecourt.app.util

import java.util.Locale

object LocationUtil {

    // Number of decimal places to keep. 5 decimals is about 1.1 meters of
    // precision - plenty to tell two different courts apart.
    private const val DECIMALS = 5

    // Turns raw GPS coordinates into a stable text key used to match a court
    // across app sessions (see CourtMapFragment and CourtDetailFragment).
    // Two live searches for the same real-world court can return coordinates
    // with slightly different decimal precision, so we round instead of using
    // Double.toString() directly - otherwise the same court could produce two
    // different keys and a game created by one user would not be found by another.
    fun latLngKey(lat: Double, lng: Double): String {
        return String.format(Locale.US, "%.${DECIMALS}f,%.${DECIMALS}f", lat, lng)
    }
}