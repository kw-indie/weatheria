package asceapps.weatheria.model

import java.time.Instant

class Daily(
	val date: Instant,
	val sunrise: Instant,
	val sunset: Instant,
	val iconNum: String,
	val min: Int,
	val max: Int,
	val pop: Int,
	val uvi: Float
)