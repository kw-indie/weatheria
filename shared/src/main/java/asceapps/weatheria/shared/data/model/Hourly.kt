package asceapps.weatheria.shared.data.model

import java.time.Instant

class Hourly internal constructor(
	val hour: Instant,
	val iconResId: Int,
	val temp: Temp,
	val pop: Percent
)