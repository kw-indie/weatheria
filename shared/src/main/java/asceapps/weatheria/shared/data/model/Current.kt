package asceapps.weatheria.shared.data.model

import asceapps.weatheria.shared.data.repo.UNKNOWN_FLT
import asceapps.weatheria.shared.data.repo.UNKNOWN_INT

/**
 * Values that are no longer available for approximation are set to [UNKNOWN_INT] and [UNKNOWN_FLT]
 */
class Current internal constructor(
	val conditionIndex: Int,
	val icon: Int,
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
)