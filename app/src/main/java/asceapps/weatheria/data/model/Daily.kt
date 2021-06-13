package asceapps.weatheria.data.model

import java.time.Instant

class Daily(
	val date: Instant,
	val min: Int,
	val max: Int,
	val icon: Int,
	val pop: Int,
	val uv: Int,
	val sunrise: Instant,
	val sunset: Instant,
	val moonrise: Instant,
	val moonset: Instant,
	val moonPhaseIndex: Int
)