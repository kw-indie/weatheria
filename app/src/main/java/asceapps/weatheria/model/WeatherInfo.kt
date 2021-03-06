package asceapps.weatheria.model

import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

class WeatherInfo(
	val location: Location,
	val lastUpdate: Instant,
	val current: Current,
	val hourly: List<Hourly>,
	val daily: List<Daily>
) {

	// if daily includes today, get it, else, get last day
	val today = Instant.now().let { daily.lastOrNull { day -> day.date < it } } ?: daily[0]

	// if hourly includes this hour, get it, else get last hour
	val thisHour = Instant.now().let { hourly.lastOrNull { hour -> hour.hour < it } } ?: hourly[0]
	val secondOfToday = LocalTime.now(location.zoneOffset).toSecondOfDay()
	val secondOfSunriseToday = localSecondOfDay(today.sunrise, location.zoneOffset)
	val secondOfSunsetToday = localSecondOfDay(today.sunset, location.zoneOffset)

	private fun localSecondOfDay(instant: Instant, offset: ZoneOffset) =
		LocalDateTime.ofInstant(instant, offset)
			.toLocalTime()
			.toSecondOfDay()
}
