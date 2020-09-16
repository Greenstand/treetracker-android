package org.greenstand.android.TreeTracker.models

import android.location.Location
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ConvergenceTest {

    lateinit var convergence: Convergence

    val locations = mutableListOf<Location>()

    // Hard coded longitude and latitude pair values
    val locationValues = listOf(
        Pair(-122.08400001, 37.42149486),
        Pair(-122.08500111, 37.42149487),
        Pair(-122.093121519, 37.42149483),
        Pair(-122.0915121999, 37.42149489),
        Pair(-122.0773486753, 37.42149481),
        Pair(-121.0184743902, 37.42149490)
    )

    // Stats derived from first five pairs from locationValues
    val LONGITUDINAL_MEAN = -122.08619670284
    val LONGITUDINAL_VARIANCE = 0.00003215001617309765000
    val LONGITUDINAL_STD_DEV = 0.005670098427108444
    val LATITUDINAL_MEAN = 37.4214948520000000
    val LATITUDINAL_VARIANCE = 0.0000000000000008159999758707253
    val LATITUDINAL_STD_DEV = 0.000000028565713291824612

    // Sliding window calculations - excluding 0th value and including 5th value from locationValues
    val SLIDING_LONGITUDINAL_MEAN = -121.87309157888
    val SLIDING_LONGITUDINAL_VARIANCE = 0.1461052927617462
    val SLIDING_LONGITUDINAL_STD_DEV = 0.38223722053424647
    val SLIDING_LATITUDE_MEAN = 37.4214948600000000
    val SLIDING_LATITUDE_VARIANCE = 0.0000000000000011231999130195744
    val SLIDING_LATITUDE_STD_DEV = 0.000000033514174807379253

    @Before
    fun setup() {
        for (i in 0..5) {
            val location = mockk<Location>(relaxed = true)
            every { location.longitude } returns locationValues[i].first
            every { location.latitude } returns locationValues[i].second
            locations.add(location)
        }
        convergence = Convergence(locations.subList(0, 5))
    }

    @Test
    fun noConvergenceUntilThresholdDataSize() {
        // instantiate with locations with size less than required for convergence computation
        convergence = Convergence(locations.subList(0, 3))

        convergence.computeConvergence()

        assertNull(convergence.longitudeConvergence)
        assertNull(convergence.latitudeConvergence)
    }

    @Test
    fun convergenceOnThresholdDataSize() {

        convergence.computeConvergence()

        assertNotNull(convergence.longitudeConvergence)
        assertNotNull(convergence.latitudeConvergence)
    }

    @Test
    fun longitudeConvergenceComputation() {

        convergence.computeConvergence()

        assertEquals(
            "longitude mean computation", LONGITUDINAL_MEAN,
            convergence.longitudeConvergence!!.mean, 0.0)
        assertEquals(
            "longitude variance computation", LONGITUDINAL_VARIANCE,
            convergence.longitudeConvergence!!.variance, 0.0)
        assertEquals(
            "longitude std dev computation", LONGITUDINAL_STD_DEV,
            convergence.longitudeConvergence!!.standardDeviation, 0.0)
    }

    @Test
    fun latitudeConvergenceComputation() {

        convergence.computeConvergence()

        assertEquals(
            "latitude mean computation", LATITUDINAL_MEAN,
            convergence.latitudeConvergence!!.mean, 0.0)
        assertEquals(
            "latitude variance computation", LATITUDINAL_VARIANCE,
            convergence.latitudeConvergence!!.variance, 0.0)
        assertEquals(
            "latitude std dev computation", LATITUDINAL_STD_DEV,
            convergence.latitudeConvergence!!.standardDeviation, 0.0)
    }

    @Test
    fun longitudinalSlidingWindowComputation() {

        // Perform initial computation
        convergence.computeConvergence()

        // Perform sliding window computation - exclude 0th and include 5th location value from
        // locationValues
        convergence.computeSlidingWindowConvergence(locations[0], locations[5])

        assertEquals(
            "Longitude Mean - sliding window", SLIDING_LONGITUDINAL_MEAN,
            convergence.longitudeConvergence!!.mean, 0.0)
        assertEquals(
            "Longitude Variance - sliding window", SLIDING_LONGITUDINAL_VARIANCE,
            convergence.longitudeConvergence!!.variance, 0.0)
        assertEquals(
            "Longitude std dev - sliding window", SLIDING_LONGITUDINAL_STD_DEV,
            convergence.longitudeConvergence!!.standardDeviation, 0.0)
    }

    @Test
    fun latitudeSlidingWindowComputation() {

        // Perform initial computation
        convergence.computeConvergence()

        // Sliding window computation
        convergence.computeSlidingWindowConvergence(locations[0], locations[5])

        assertEquals(
            "Latitude Mean - sliding window", SLIDING_LATITUDE_MEAN,
            convergence.latitudeConvergence!!.mean, 0.0)
        assertEquals(
            "Latitude Variance - sliding window", SLIDING_LATITUDE_VARIANCE,
            convergence.latitudeConvergence!!.variance, 0.0)
        assertEquals(
            "Latitude std dev - sliding window", SLIDING_LATITUDE_STD_DEV,
            convergence.latitudeConvergence!!.standardDeviation, 0.0)
    }
}
