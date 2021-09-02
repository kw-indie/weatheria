package asceapps.weatheria.shared.data.model

import java.time.Instant

class Daily internal constructor(
	val date: Instant,
	val min: Int,
	val max: Int,
	val dayIconIndex: Int,
	val nightIconIndex: Int,
	val pop: Int,
	val uv: Int,
	val sunrise: Instant,
	val sunset: Instant,
	val moonrise: Instant,
	val moonset: Instant,
	val moonPhaseIndex: Int
)