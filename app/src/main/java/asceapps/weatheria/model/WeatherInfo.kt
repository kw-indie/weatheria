package asceapps.weatheria.model

import android.text.format.DateUtils
import asceapps.weatheria.data.IDed
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle

class WeatherInfo(
	val location: Location,
	private val lastUpdateInstant: Instant,
	val current: Current,
	val hourly: List<Hourly>,
	val daily: List<Daily>
) : IDed {

	override val id = location.id

	// for HashItemCallback
	override fun hashCode() = id + lastUpdateInstant.epochSecond.toInt()

	// if daily includes today, get it, else, get last day
	val today = Instant.now().let { daily.lastOrNull { day -> day.date < it } } ?: daily[0]
	// if hourly includes this hour, get it, else get last hour
	val thisHour = Instant.now().let { hourly.lastOrNull { hour -> hour.hour < it } } ?: hourly[0]

	val secondOfToday get() = LocalTime.now(location.zoneOffset).toSecondOfDay()
	val secondOfSunriseToday get() = localSecondOfDay(today.sunrise, location.zoneOffset)
	val secondOfSunsetToday get() = localSecondOfDay(today.sunset, location.zoneOffset)

	val locationName get() = commaSep(location.name, location.country)
	val localNow get() = localDateTime(location.zoneOffset)
	val lastUpdate get() = relativeTime(lastUpdateInstant)
	val currentTemp get() = temp(current.temp)
	val currentFeel get() = temp(current.feelsLike)
	val currentWindSpeed get() = speed(current.windSpeed)
	val currentHumidity get() = percent(current.humidity)
	val todayMinMax get() = minMax(today.min, today.max)
	val todaySunrise get() = time(today.sunrise, location.zoneOffset)
	val todaySunset get() = time(today.sunset, location.zoneOffset)

	companion object {

		private fun localSecondOfDay(instant: Instant, offset: ZoneOffset) =
			LocalDateTime.ofInstant(instant, offset)
				.toLocalTime()
				.toSecondOfDay()

		// region formatting
		fun setFormatSystem(metric: Boolean, speedUnit: String) {
			this.metric = metric
			this.speedUnit = speedUnit
		}

		private var metric = true
		private var speedUnit = ""
		// prints at least 1 digit, sep each 3 digits, 0 to 2 decimal digits, rounds to nearest
		private val nFormat = NumberFormat.getInstance().apply {
			minimumFractionDigits = 0
			maximumFractionDigits = 2
		}

		// adds localized percent char
		private val pFormat = NumberFormat.getPercentInstance()
		// trash java never localizes the xxx part
		private val dtFormatter = DateTimeFormatter.ofPattern("EEE, d MMMM, h:mm a (xxx)")
			.withDecimalStyle(DecimalStyle.ofDefaultLocale())
		private val tFormatter = DateTimeFormatter.ofPattern("h:mm a")
			.withDecimalStyle(DecimalStyle.ofDefaultLocale())

		private fun commaSep(vararg str: String) = str.joinToString()
		// use Locale.Builder().setLanguageTag("ar-u-nu-arab").build() for arabic numbers
		private fun relativeTime(instant: Instant): CharSequence =
			DateUtils.getRelativeTimeSpanString(instant.toEpochMilli())

		private fun localDateTime(offset: ZoneOffset): String =
			dtFormatter.format(OffsetDateTime.now(offset))

		private fun time(instant: Instant, offset: ZoneOffset): String =
			tFormatter.format(instant.atOffset(offset))

		private fun temp(deg: Int) =
			nFormat.format((if(metric) deg - 273.15f else deg * 1.8f - 459.67f).toInt()) + '°'

		private fun minMax(min: Int, max: Int) =
			temp(min).padEnd(5) + '|' + temp(max).padStart(5)

		private fun speed(mps: Float) =
			nFormat.format(if(metric) mps else mps * 2.237f) + ' ' + speedUnit
		// our ratios are already from 0-100, this formatter expects fractions from 0-1
		private fun percent(ratio: Int): String = pFormat.format(ratio / 100f)
		// endregion
	}
}
