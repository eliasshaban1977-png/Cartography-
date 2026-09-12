package com.example.cartography

import java.util.Locale
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

class SolarDeclinationEngine {
    companion object {
        const val DEG_TO_RAD = PI / 180.0
        const val RAD_TO_DEG = 180.0 / PI
        const val MEAN_AXIAL_TILT_DEG = 23.439291
    }

    /**
     * Calculates solar declination angle using Spencer's high-precision Fourier series (1971).
     * @param dayOfYear 1..366
     * @param julianCentury centuries since J2000.0 (e.g. 0.26 for year 2026)
     */
    fun calculateSolarDeclination(dayOfYear: Int, julianCentury: Double = 0.26): SolarDeclinationResult {
        val clampedDay = dayOfYear.coerceIn(1, 366)
        val gamma = (2.0 * PI / 365.0) * (clampedDay - 1)

        val declinationRad = 0.006918 -
                0.399912 * cos(gamma) + 0.070257 * sin(gamma) -
                0.006758 * cos(2 * gamma) + 0.000907 * sin(2 * gamma) -
                0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)

        val seasonalAxialTilt = MEAN_AXIAL_TILT_DEG - (0.0130042 * julianCentury)

        val eotMinutes = 229.18 * (0.000075 +
                0.001868 * cos(gamma) - 0.032077 * sin(gamma) -
                0.014615 * cos(2 * gamma) - 0.040849 * sin(2 * gamma))

        return SolarDeclinationResult(
            dayOfYear = clampedDay,
            declinationRad = declinationRad,
            declinationDeg = declinationRad * RAD_TO_DEG,
            axialTiltDeg = seasonalAxialTilt,
            equationOfTimeMinutes = eotMinutes
        )
    }

    /**
     * Computes the Dawn/Dusk barrier hour angle and unit plane vector
     * over the central cartographic intersection.
     */
    fun computeDawnDuskBarrier(
        latDeg: Double,
        lonDeg: Double,
        declination: SolarDeclinationResult,
        localHourAngleRad: Double = 0.0
    ): DawnDuskBarrierState {
        val latRad = latDeg * DEG_TO_RAD
        val decRad = declination.declinationRad

        val cosHourAngle = -tan(latRad) * tan(decRad)

        var isPolarDay = false
        var isPolarNight = false
        val hourAngleRad: Double = when {
            cosHourAngle <= -1.0 -> {
                isPolarDay = true
                PI
            }
            cosHourAngle >= 1.0 -> {
                isPolarNight = true
                0.0
            }
            else -> acos(cosHourAngle)
        }

        val h = localHourAngleRad
        val eastComponent = cos(decRad) * sin(h)
        val northComponent = sin(latRad) * cos(decRad) * cos(h) - cos(latRad) * sin(decRad)
        val upComponent = cos(latRad) * cos(decRad) * cos(h) + sin(latRad) * sin(decRad)

        val normalVector = doubleArrayOf(eastComponent, northComponent, upComponent)

        return DawnDuskBarrierState(
            latitudeDeg = latDeg,
            longitudeDeg = lonDeg,
            hourAngleRad = hourAngleRad,
            hourAngleDeg = hourAngleRad * RAD_TO_DEG,
            isPolarDay = isPolarDay,
            isPolarNight = isPolarNight,
            terminatorNormalVector = normalVector
        )
    }
}

class MudosCleanLabelEngine {
    val solarEngine = SolarDeclinationEngine()
    val sunNode = CleanCelestialNode("SOL_01", "Sun", PI / 3, 0.0020)
    val moonNode = CleanCelestialNode("LUNA_01", "The Moon", 0.0, 0.0016)
    val bucketNav = BucketMouthNav()

    fun tick(dayOfYear: Int, latDeg: Double, lonDeg: Double, speedMultiplier: Double = 1.0): String {
        sunNode.orbitalAngleRad = (sunNode.orbitalAngleRad + sunNode.angularVelocity * speedMultiplier) % (2 * PI)
        moonNode.orbitalAngleRad = (moonNode.orbitalAngleRad + moonNode.angularVelocity * speedMultiplier) % (2 * PI)
        bucketNav.siriusAntaresAngle = (bucketNav.siriusAntaresAngle + 0.0008 * speedMultiplier) % (2 * PI)

        val decResult = solarEngine.calculateSolarDeclination(dayOfYear)
        val barrierState = solarEngine.computeDawnDuskBarrier(latDeg, lonDeg, decResult, sunNode.orbitalAngleRad)

        val state = if (!barrierState.isPolarNight && sin(sunNode.orbitalAngleRad) > 0.0) "DAYLIGHT (DAWN PASS)" else "NIGHT (DUSK PASS)"

        return String.format(
            Locale.US,
            "Center Zenith Telemetry | Lat: %.2f° | Declination: %.2f° | Tilt: %.2f° | Status: %s",
            latDeg, decResult.declinationDeg, decResult.axialTiltDeg, state
        )
    }
}
