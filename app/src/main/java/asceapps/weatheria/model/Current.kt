package asceapps.weatheria.model

import java.time.Instant

class Current(
	val time: Instant,
	val conditionIndex: Int,
	val icon: String,
	val temp: Int,
	val feelsLike: Int,
	val pressure: Int,
	val humidity: Int,
	val dewPoint: Int,
	val clouds: Int,
	val visibility: Int,
	val windSpeed: Float,
	val windDir: Int,
	/**
	 * 0: High, 1: Medium, 2: Low
	 */
	val accuracy: Int
)