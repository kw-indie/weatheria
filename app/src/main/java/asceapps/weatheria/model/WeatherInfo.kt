package asceapps.weatheria.model

import java.time.Instant

class WeatherInfo(
	val location: Location,
	val lastUpdate: Instant,
	val current: Current,
	val hourly: List<Hourly>,
	val daily: List<Daily>
) {

	// if daily includes today, get it, else, get last day
	val today get() = Instant.now().let {daily.last {day -> day.date < it}}
	// if hourly includes this hour, get it, else get last hour
	val thisHour get() = Instant.now().let {hourly.last {hour -> hour.hour < it}}
	// if today is available, check 'now' against its daytime
	val isNowDaytime get() = today.run {Instant.now() in sunrise..sunset}
}
