package org.greenstand.android.TreeTracker.utilities

/**
 *  Contains configuration parameters for Location data capture
 *  and location convergence parameter thresholds.
 */
object LocationDataConfig {

    // Location update config values
    val MIN_TIME_BTWN_UPDATES = 1000L // Minimum time between location updates
    val MIN_DISTANCE_BTW_UPDATES = 0F // Minimum distance between location updates

    // Location Convergence config values
    val CONVERGENCE_TIMEOUT = 60000L // Timeout value for Location Convergence
    val CONVERGENCE_DATA_SIZE = 5 // Number of data required to compute convergence parameters
    val LONG_STD_DEV = 0.00001 // Threshold standard deviation value for longitude convergence
    val LAT_STD_DEV = 0.00001 // Threshold standard deviation value for latitude convergence
}
