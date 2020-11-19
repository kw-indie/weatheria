package asceapps.weatheria.model

import java.time.OffsetDateTime

class WeatherInfo(
	val location: Location,
	val current: Current,
	val hourly: List<Hourly>,
	val daily: List<Daily>,
	val lastUpdate: CharSequence,
	val now: OffsetDateTime,
	val today: Daily?,
	val thisHour: Hourly?,
	val isNowDaytime: Boolean?
) {

	val todayMinMaxString get() = today?.run {"${min.format.padEnd(5)}|${max.format.padStart(5)}"} ?: ""
}
