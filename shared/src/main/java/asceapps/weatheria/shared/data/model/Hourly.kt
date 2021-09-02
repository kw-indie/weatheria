package asceapps.weatheria.shared.data.model

import java.time.Instant

class Hourly internal constructor(
	val hour: Instant,
	val icon: Int,
	val temp: Int,
	val pop: Int
)