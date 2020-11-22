package asceapps.weatheria.model

import java.time.Instant

class Hourly(
	val hour: Instant,
	val icon: String,
	val temp: Int,
	val pop: Int
)