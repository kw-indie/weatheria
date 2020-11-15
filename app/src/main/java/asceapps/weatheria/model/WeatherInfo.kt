package asceapps.weatheria.model

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class WeatherInfo(
	val location: Location,
	val current: Current,
	val hourly: List<Hourly>,
	val daily: List<Daily>
) {

	val now get() = OffsetDateTime.now(ZoneId.from(location.zoneOffset))

	val today
		get() = now.toLocalDate().let {today ->
			daily.find {it.date == today}
		}

	val todayMinMaxString get() = today?.run {"${min.format.padEnd(5)}|${max.format.padStart(5)}"} ?: ""

	val thisHour
		get() = now.toLocalTime().truncatedTo(ChronoUnit.HOURS).let {thisHour ->
			hourly.find {it.hour == thisHour}
		}

	/**
	 * `true` if daytime, `false` if nighttime or `null` if [today] is `null`
	 */
	val isDaytime
		get() = now.toLocalTime().let {now ->
			today?.run {now in sunrise..sunset}
		}
}
