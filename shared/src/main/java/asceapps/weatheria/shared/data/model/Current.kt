package asceapps.weatheria.shared.data.model

import asceapps.weatheria.shared.data.util.*

/**
 * Values that are no longer available for approximation are set to [UNKNOWN_INT] and [UNKNOWN_FLT]
 */
class Current internal constructor(
	val iconIndex: Int,
	val temp: Int,
	val feelsLike: Int,
	val windSpeed: Float,
	val windDir: Int,
	val pressure: Int,
	val humidity: Int,
	val dewPoint: Int,
	val clouds: Int,
	val visibility: Float,
	val uv: Int
) {

	/**
	 * one of [UV_LOW], [UV_MODERATE], [UV_HIGH], [UV_V_HIGH] or [UV_EXTREME]
	 */
	val uvLevel get() = uvLevel(uv)
}