package asceapps.weatheria.model

import android.content.Context
import asceapps.weatheria.R
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAccessor

private val dtFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
private val tFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

class WeatherInfo(
	val location: Location,
	val current: Current,
	val hourly: List<Hourly>,
	val daily: List<Daily>,
	val updateTime: Instant
) {

	val now get() = OffsetDateTime.now(ZoneId.from(location.zoneOffset))

	val today
		get() = now.toLocalDate().let {today ->
			daily.find {it.date == today}
		}

	val todayMinMaxString get() = today?.let {"${it.min.format}\t\t|\t\t${it.max.format}"} ?: ""

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

	fun dtString(temporal: TemporalAccessor): String = dtFormatter.format(temporal)

	fun tString(temporal: TemporalAccessor): String = tFormatter.format(temporal)

	fun windString(c: Context) = current.windSpeed.format +
		c.getString(R.string.comma) + " " +
		c.resources.getStringArray(R.array.dir_letters)[current.windDir.eighth]
}
