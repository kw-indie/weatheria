package asceapps.weatheria.data.model

import java.time.Instant

class Hourly(
	val hour: Instant,
	val icon: Int,
	val temp: Int,
	val pop: Int
)