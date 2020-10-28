package asceapps.weatheria.model

import asceapps.weatheria.api.WeatherService
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

class WeatherInfo(
	val location: Location,
	val current: Current,
	val hourly: List<Hourly>,
	val daily: List<Daily>,
	val updateTime: LocalDateTime
) {

	val now get() = OffsetDateTime.now(ZoneId.from(location.zoneOffset))

	val nowString get() = dtFormatter.format(now)

	val updateTimeString get() = tFormatter.format(updateTime)

	val todayWeather
		get() = now.toLocalDate().let {today ->
			daily.find {it.date == today}
		}

	val thisHourWeather
		get() = now.toLocalTime().truncatedTo(ChronoUnit.HOURS).let {thisHour ->
			hourly.find {it.hour == thisHour}
		}

	/**
	 * `true` if daytime, `false` if nighttime or `null` if [todayWeather] is `null`
	 */
	val isDaytime
		get() = now.toLocalTime().let {now ->
			todayWeather?.run {now in sunrise..sunset}
		}
}

var metric = true
	private set
private var gSpeedUnit = ""
fun setMetric(b: Boolean, speedUnit: String) {
	metric = b
	gSpeedUnit = speedUnit
}
// prints at least 1 digit, sep each 3 digits, 0 to 2 decimal digits, rounds to nearest
private val decimalFormat = DecimalFormat(",##0.##")
private val dtFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
private val tFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

class Temp(kelvin: Int) {

	val v = kelvin
		get() = (if(metric) field - 273.15f else field * 1.8f - 459.67f).toInt()

	override fun toString() = "$v°"
}

class Speed(meterPerSec: Float) {

	val v = meterPerSec
		get() = if(metric) field else field * 2.237f

	override fun toString() = "${decimalFormat.format(v)!!} $gSpeedUnit"
}

class Percent(percent: Int) {

	val v = percent

	override fun toString() = "$v%"
}

class Icon(icon: String) {

	val v = icon

	override fun toString() = WeatherService.iconUrlFor(v)
}

class Direction(degree: Int) {

	val v = degree
	/**
	 * value in 0-7: 0=E, 1=NE...7=SE
	 */
	val eighth = (v + 22) % 360
		get() = when(field) {
			in 0..44 -> 0
			in 45..89 -> 1
			in 90..134 -> 2
			in 135..179 -> 3
			in 180..224 -> 4
			in 225..269 -> 5
			in 270..314 -> 6
			else -> 7
		}
}