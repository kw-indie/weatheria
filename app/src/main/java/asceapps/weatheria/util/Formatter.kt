package asceapps.weatheria.util

import android.text.format.DateUtils
import asceapps.weatheria.shared.data.util.UNKNOWN_FLT
import asceapps.weatheria.shared.data.util.UNKNOWN_INT
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

const val UNITS_METRIC = 0
const val UNITS_IMPERIAL = 1

const val UNKNOWN_STR = "--"

// made this class an object cuz i'm worried for excessive code generation (it's used in adapters/views/widgets)
/**
 * all format methods return [UNKNOWN_STR] when the input is either [UNKNOWN_INT] or [UNKNOWN_FLT]
 */
object Formatter {

	/**
	 * one of [UNITS_METRIC] or [UNITS_IMPERIAL]
	 */
	var unitSystem = UNITS_METRIC
	var locale = Locale.getDefault()
		set(value) {
			if(field != value) {
				field = value
				reset()
			}
		}
	private val isMetric get() = unitSystem == 0

	// prints at least 1 digit, separate each 3 digits, 0 to 2 decimal digits, rounds to nearest
	private lateinit var nFormat: NumberFormat

	// expects fraction. adds localized percent char
	private lateinit var pFormat: NumberFormat

	// use Locale.forLanguageTag("ar-u-nu-arab") for arabic numbers
	private lateinit var nowFormatter: DateTimeFormatter
	private lateinit var dayFormatter: DateTimeFormatter
	private lateinit var tFormatter: DateTimeFormatter
	private lateinit var hourFormatter: DateTimeFormatter

	init {
		reset()
	}

	private fun reset() {
		nFormat = NumberFormat.getInstance(locale).apply {
			minimumFractionDigits = 0
			maximumFractionDigits = 2
		}
		pFormat = NumberFormat.getPercentInstance(locale)
		// trash java never localizes the xxx (e.g +3:00)
		nowFormatter = DateTimeFormatter.ofPattern("EEE h:mm a (xxx)", locale)
		dayFormatter = DateTimeFormatter.ofPattern("EEE", locale)
		tFormatter = DateTimeFormatter.ofPattern("h:mm a", locale)
		hourFormatter = DateTimeFormatter.ofPattern("h a", locale)
	}

	/**
	 * eg. `3 hours ago`, `x days ago` (min. res = hours) localized
	 */
	fun relativeTime(i: Instant): CharSequence = DateUtils.getRelativeTimeSpanString(
		i.toEpochMilli(), System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS
	)

	/**
	 * eg. `Mon 5:23 PM (+4:00)` localized
	 */
	fun zonedNow(z: ZoneId): String = nowFormatter.format(Instant.now().atZone(z))

	/**
	 * eg. `SAT`, `SUN` localized
	 */
	fun zonedDay(i: Instant, z: ZoneId): String = dayFormatter.format(i.atZone(z)).uppercase()

	/**
	 * eg. `12:03 AM` localized
	 */
	fun zonedTime(i: Instant, z: ZoneId): String = tFormatter.format(i.atZone(z))

	/**
	 * eg. `4 AM` localized
	 */
	fun zonedHour(i: Instant, z: ZoneId): String = hourFormatter.format(i.atZone(z))

	/**
	 * eg. `23°` or `73.4°` localized, (C) or (F)
	 */
	fun temp(c: Int): String = if(c == UNKNOWN_INT) UNKNOWN_STR else
		nFormat.format(if(isMetric) c else (c * 1.8f + 32).toInt()) + '°'

	/**
	 * eg. `43%` localized
	 */
	fun percent(p: Int): String = if(p == UNKNOWN_INT) UNKNOWN_STR else
		pFormat.format(p / 100f)

	/**
	 * eg. `10.1` or `6.26` localized, (km) or (mi)
	 */
	fun distance(km: Float): String = if(km == UNKNOWN_FLT) UNKNOWN_STR else
		nFormat.format(if(isMetric) km else km * 0.6214f)

	/**
	 * eg. `1001` or `29.56` localized, (mb) or fraction (inHg)
	 */
	fun pressure(mb: Int): String = if(mb == UNKNOWN_INT) UNKNOWN_STR else
		nFormat.format(if(isMetric) mb else mb * 0.0295301f)

	/**
	 * same as input, localized
	 */
	fun number(n: Int): String = nFormat.format(n)
}