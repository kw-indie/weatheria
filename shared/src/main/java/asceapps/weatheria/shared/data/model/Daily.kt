package asceapps.weatheria.shared.data.model

import asceapps.weatheria.shared.data.util.*
import java.time.Instant

class Daily internal constructor(
	val date: Instant,
	val min: Int,
	val max: Int,
	val dayIconIndex: Int,
	val nightIconIndex: Int,
	val pop: Int,
	val sunrise: Instant,
	val sunset: Instant,
	val moonrise: Instant,
	val moonset: Instant,
	val moonAge: Int
) {

	/**
	 * one of [MOON_NEW], [MOON_WAXING_CRESCENT], [MOON_FIRST_QUARTER]. [MOON_WAXING_GIBBOUS], [MOON_FULL],
	 * [MOON_WANING_GIBBOUS], [MOON_THIRD_QUARTER] or [MOON_WANING_CRESCENT]
	 */
	val moonPhase get() = moonPhase(moonAge.toFloat())
}