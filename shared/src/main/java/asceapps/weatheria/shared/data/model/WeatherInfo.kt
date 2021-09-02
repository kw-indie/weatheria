package asceapps.weatheria.shared.data.model

import asceapps.weatheria.shared.data.base.Listable
import asceapps.weatheria.shared.data.repo.*
import java.time.Instant

/**
 * @param accuracy one of [ACCURACY_FRESH], [ACCURACY_HIGH], [ACCURACY_LOW] or [ACCURACY_OUTDATED].
 */
class WeatherInfo internal constructor(
	val location: Location,
	val current: Current,
	val hourly: List<Hourly>,
	val daily: List<Daily>,
	val lastUpdate: Instant,
	val accuracy: Int,
	val pos: Int
): Listable {

	override val id get() = location.id
	override val hash get() = id + lastUpdate.epochSecond.toInt()

	val today = today(daily) ?: daily.last()
	val todayOrNull get() = today(daily)
	val thisHourOrNull get() = thisHour(hourly)
	val partOfDay get() = partOfDay(today)
}