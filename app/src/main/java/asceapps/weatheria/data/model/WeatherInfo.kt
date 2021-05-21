package asceapps.weatheria.data.model

import android.text.format.DateUtils
import asceapps.weatheria.data.base.Listable
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import kotlin.math.min

class WeatherInfo(
	val location: Location,
	val current: Current,
	val hourly: List<Hourly>,
	val daily: List<Daily>,
	/**
	 * 0: High, within an hour
	 * 1: Medium, within 2 days
	 * 2: Low, within a week
	 */
	val accuracy: Int
): Listable {

	override val id = location.id
	override fun hashCode() = id + location.lastUpdate.epochSecond.toInt()

	// if daily includes today, get it, else, get last day
	val today = Instant.now().let { daily.lastOrNull { day -> day.date < it } } ?: daily[0]
	// if hourly includes this hour, get it, else get last hour
	val thisHour = Instant.now().let { hourly.lastOrNull { hour -> hour.hour < it } } ?: hourly[0]

	val partOfDay: Pair<Int, Float>
		get() {
			val secondsInDay = 24 * 60 * 60f
			val now = Instant.now()
			val nowSecondOfDay = now.epochSecond % secondsInDay
			val zeroSecondOfDay = now.epochSecond - nowSecondOfDay
			// all values below are in fractions and not seconds
			val nowF = nowSecondOfDay / secondsInDay
			// sun- rise/set are taken from approx of `today`
			val sunriseF = (today.sunrise.epochSecond % secondsInDay) / secondsInDay
			val sunsetF = (today.sunset.epochSecond % secondsInDay) / secondsInDay
			// transitionF is dawn or dusk (~70 min at the equator).
			// for simplicity, make it 1 hour before/after sun- rise/set.
			// near poles, days/nights can be too long, so
			// ensure transition is min of 1 hour or half of smaller of day/night
			val dayLengthF = sunsetF - sunriseF
			val nightLengthF = 1 - dayLengthF
			val transitionF = min(1 / 24f, min(dayLengthF, nightLengthF) / 2)

			val startOfDawnF = sunriseF - transitionF
			val endOfDawnF = sunriseF + transitionF
			val startOfDuskF = sunsetF - transitionF
			val endOfDuskF = sunsetF + transitionF
			return when {
				nowF < startOfDawnF -> 0 to 0f
				nowF < sunriseF -> 1 to (nowF - startOfDawnF) / transitionF
				nowF < endOfDawnF -> 2 to (nowF - sunriseF) / transitionF
				nowF < startOfDuskF -> 3 to 0f
				nowF < sunsetF -> 4 to (nowF - startOfDuskF) / transitionF
				nowF < endOfDuskF -> 5 to (nowF - sunsetF) / transitionF
				else -> 0 to 0f
			}
		}

	// todo make most of these properties value classes in kotlin 1.5
	val localNow get() = localDateTime(location.zoneOffset)
	val lastUpdate get() = relativeTime(location.lastUpdate)
	val currentTemp get() = temp(current.temp)
	val currentFeel get() = temp(current.feelsLike)
	val currentWindSpeed get() = speed(current.windSpeed)
	val currentHumidity get() = percent(current.humidity)
	val currentDewPoint get() = temp(current.dewPoint)
	val todayMinMax get() = minMax(today.min, today.max)
	val todaySunrise get() = localTime(today.sunrise, location.zoneOffset)
	val todaySunset get() = localTime(today.sunset, location.zoneOffset)

	companion object {

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

		// use Locale.Builder().setLanguageTag("ar-u-nu-arab").build() for arabic numbers
		private fun relativeTime(instant: Instant): CharSequence =
			DateUtils.getRelativeTimeSpanString(instant.toEpochMilli())

		private fun localDateTime(offset: ZoneOffset): String =
			dtFormatter.format(Instant.now().atOffset(offset))

		private fun localTime(instant: Instant, offset: ZoneOffset): String =
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
