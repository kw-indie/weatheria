package asceapps.weatheria.shared.data.model

import asceapps.weatheria.shared.data.repo.UNKNOWN_FLT
import asceapps.weatheria.shared.data.repo.UNKNOWN_INT

/**
 * Values that are no longer available for approximation are set to [UNKNOWN_INT] and [UNKNOWN_FLT]
 */
class Current internal constructor(
	val conditionIndex: Int,
	val iconResId: Int,
	val temp: Temp,
	val feelsLike: Temp,
	val windSpeed: Distance,
	val windDir: Int,
	val pressure: Pressure,
	val humidity: Percent,
	val dewPoint: Temp,
	val clouds: Percent,
	val visibility: Distance,
	val uv: UV
)