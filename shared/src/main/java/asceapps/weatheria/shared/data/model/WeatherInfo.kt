package asceapps.weatheria.shared.data.model

import asceapps.weatheria.shared.data.base.Listable
import asceapps.weatheria.shared.data.util.*
import java.time.Instant

class WeatherInfo internal constructor(
	val location: Location,
	val current: Current,
	val hourly: List<Hourly>,
	val daily: List<Daily>,
	val lastUpdate: Instant,
	/**
	 * one of [ACCURACY_FRESH], [ACCURACY_HIGH], [ACCURACY_LOW] or [ACCURACY_OUTDATED].
	 */
	val accuracy: Int,
	val pos: Int
): Listable {

	override val id get() = location.id
	override val hash get() = id + lastUpdate.epochSecond.toInt()

	/**
	 * today if available, last day otherwise
	 */
	val today get() = today(daily) ?: daily.last()
	val todayOrNull get() = today(daily)
	val thisHourOrNull get() = thisHour(hourly)

	/**
	 * one of:
	 *  * 0:0 = night,
	 *  * 1:f = pre dawn,
	 *  * 2:f = post dawn,
	 *  * 3:0 = day,
	 *  * 4:f = pre dusk,
	 *  * 5:f = post dusk.
	 *
	 * where f is fraction of transition between day/night and dawn/dusk
	 */
	val partOfDay get() = partOfDay(today)
}