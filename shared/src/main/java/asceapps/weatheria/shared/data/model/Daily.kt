package asceapps.weatheria.shared.data.model

import java.time.Instant

class Daily internal constructor(
	val date: Instant,
	val min: Temp,
	val max: Temp,
	val dayIconResId: Int,
	val nightIconResId: Int,
	val pop: Percent,
	val uv: UV,
	val sunrise: Instant,
	val sunset: Instant,
	val moonrise: Instant,
	val moonset: Instant,
	val moonPhaseIndex: Int
)