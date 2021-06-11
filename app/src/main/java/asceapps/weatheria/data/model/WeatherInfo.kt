package asceapps.weatheria.data.model

import android.text.format.DateUtils
import asceapps.weatheria.data.base.Listable
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import kotlin.math.min

/**
 * @param accuracy 0: Real (1 hour). 1: High (1 day). 2: Medium (2 days). 3: Low (3 days). 4: Outdated
 */
class WeatherInfo(
	val location: Location,
	val current: Current,
	val hourly: List<Hourly>,
	val daily: List<Daily>,
	val accuracy: Int
): Listable {

	override val id = location.id
	override fun hashCode() = id + location.lastUpdate.epochSecond.toInt()

	// in both 'today' and 'thisHour', sometimes the first element is some time in the future,
	// hence the 'orNull()' and '?: x[0]'
	// if daily includes today, get it, else, get last day
	val today = Instant.now().let { daily.lastOrNull { day -> day.date < it } } ?: daily[0]

	// if hourly includes this hour, get it, else get last hour
	val thisHour = Instant.now().let { hourly.lastOrNull { hour -> hour.hour < it } } ?: hourly[0]

	val partOfDay: Pair<Int, Float>
		get() {
			val night = 0 to 0f
			val secondsInDay = 24 * 60 * 60f
			val now = Instant.now()
			val nowSecondOfDay = now.epochSecond % secondsInDay
			// all values below are in fractions and not seconds
			val nowF = nowSecondOfDay / secondsInDay
			// sunrise/set are taken from approx of `today`
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
				nowF < startOfDawnF -> night // don't remove this case, we need to subtract from < sunriseF
				nowF < sunriseF -> 1 to (nowF - startOfDawnF) / transitionF
				nowF < endOfDawnF -> 2 to (nowF - sunriseF) / transitionF
				nowF < startOfDuskF -> 3 to 0f
				nowF < sunsetF -> 4 to (nowF - startOfDuskF) / transitionF
				nowF < endOfDuskF -> 5 to (nowF - sunsetF) / transitionF
				else -> night
			}
		}

	// todo make most of these properties value classes in kotlin 1.5
	val localNow get() = localDateTime(location.zoneId)
	val lastUpdate get() = relativeTime(location.lastUpdate)
	val unitSys get() = unitSystem
	val currentTemp get() = temp(current.temp)
	val currentFeel get() = temp(current.feelsLike)
	val currentPressure get() = pressure(current.pressure)
	val currentWindSpeed get() = distance(current.windSpeed)
	val currentHumidity get() = percent(current.humidity)
	val currentDewPoint get() = temp(current.dewPoint)
	val currentClouds get() = percent(current.clouds)
	val currentVisibility get() = distance(current.visibility)
	val currentUVIndex get() = number(current.uv)
	val currentUVLevelIndex
		get() = when(current.uv) {
			in 0..2 -> 0
			in 3..5 -> 1
			in 6..7 -> 2
			in 8..10 -> 3
			else -> 4
		}
	val todayMax get() = temp(today.max)
	val todayMin get() = temp(today.min)
	val todayPop get() = percent(today.pop)
	val todaySunrise get() = localTime(today.sunrise, location.zoneId)
	val todaySunset get() = localTime(today.sunset, location.zoneId)
	val todayMoonrise get() = localTime(today.moonrise, location.zoneId)
	val todayMoonset get() = localTime(today.moonset, location.zoneId)

	companion object {

		// region formatting
		fun setUnitsSystem(units: Int) {
			unitSystem = units
		}

		private var unitSystem = 0
		private val isMetric get() = unitSystem == 0

		// prints at least 1 digit, sep each 3 digits, 0 to 2 decimal digits, rounds to nearest
		private val nFormat = NumberFormat.getInstance().apply {
			minimumFractionDigits = 0
			maximumFractionDigits = 2
		}

		// adds localized percent char
		private val pFormat = NumberFormat.getPercentInstance()

		// trash java never localizes the xxx part // old pattern: EEE, MMMM d, h:mm a (xxx)
		private val dtFormatter = DateTimeFormatter.ofPattern("EEE h:mm a (xxx)")
			.withDecimalStyle(DecimalStyle.ofDefaultLocale())
		private val tFormatter = DateTimeFormatter.ofPattern("h:mm a")
			.withDecimalStyle(DecimalStyle.ofDefaultLocale())

		// use Locale.Builder().setLanguageTag("ar-u-nu-arab").build() for arabic numbers
		private fun relativeTime(instant: Instant): CharSequence =
			DateUtils.getRelativeTimeSpanString(instant.toEpochMilli())

		private fun localDateTime(zone: ZoneId): String =
			dtFormatter.format(Instant.now().atZone(zone))

		private fun localTime(instant: Instant, zone: ZoneId): String =
			tFormatter.format(instant.atZone(zone))

		private fun number(n: Number): String = nFormat.format(n)

		private fun temp(deg: Int) =
			nFormat.format(if(isMetric) deg else (deg * 1.8f + 32).toInt()) + '°'

		private fun distance(km: Float): String = nFormat.format(if(isMetric) km else km * 0.6214f)

		// our ratios are already from 0-100, this formatter expects fractions from 0-1
		private fun percent(ratio: Int): String = pFormat.format(ratio / 100f)

		private fun pressure(mb: Int): String = nFormat.format(if(isMetric) mb else mb * 0.0295301f)
		// endregion
	}
}
