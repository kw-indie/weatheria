package asceapps.weatheria.shared.data.model

import android.text.format.DateUtils
import asceapps.weatheria.shared.data.repo.*
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * one of [UNITS_METRIC] or [UNITS_IMPERIAL]
 */
var unitSystem = UNITS_METRIC
var locale = Locale.getDefault()
	set(value) {
		if(field != value) {
			field = value
			initialize()
		}
	}
private val isMetric get() = unitSystem == 0

// prints at least 1 digit, separate each 3 digits, 0 to 2 decimal digits, rounds to nearest
private lateinit var nFormat: NumberFormat

// expects fraction. adds localized percent char
private lateinit var pFormat: NumberFormat

// use Locale.Builder().setLanguageTag("ar-u-nu-arab").build() for arabic numbers
private lateinit var nowFormatter: DateTimeFormatter
private lateinit var dayFormatter: DateTimeFormatter
private lateinit var tFormatter: DateTimeFormatter
private lateinit var hourFormatter: DateTimeFormatter

internal fun initialize() {
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
 * eg. 3 hours ago, x days ago (min. res = hours) localized
 */
fun relativeTime(i: Instant): CharSequence = DateUtils.getRelativeTimeSpanString(
	i.toEpochMilli(), System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS
)

/**
 * eg. Mon 5:23 PM (+4:00) localized
 */
fun zonedNow(z: ZoneId): String = nowFormatter.format(Instant.now().atZone(z))

/**
 * eg. SAT, SUN localized
 */
fun zonedDay(i: Instant, z: ZoneId): String = dayFormatter.format(i.atZone(z)).uppercase()

/**
 * eg. 12:03 AM localized
 */
fun zonedTime(i: Instant, z: ZoneId): String = tFormatter.format(i.atZone(z))

/**
 * eg. 4 AM localized
 */
fun zonedHour(i: Instant, z: ZoneId): String = hourFormatter.format(i.atZone(z))

@JvmInline
value class Temp internal constructor(val v: Int) {
	/**
	 * eg. `23°` localized value (C) or (F)
	 */
	override fun toString(): String = if(v == UNKNOWN_INT) UNKNOWN_STR else
		nFormat.format(if(isMetric) v else (v * 1.8f + 32).toInt()) + '°'
}

@JvmInline
value class Percent internal constructor(val v: Int) {
	/**
	 * eg. `43%` localized
	 */
	override fun toString(): String = if(v == UNKNOWN_INT) UNKNOWN_STR else
		pFormat.format(v / 100f)
}

@JvmInline
value class Distance internal constructor(private val v: Float) {
	/**
	 * localized value (km) or (mi)
	 */
	override fun toString(): String = if(v == UNKNOWN_FLT) UNKNOWN_STR else
		nFormat.format(if(isMetric) v else v * 0.6214f)
}

@JvmInline
value class Pressure internal constructor(private val v: Int) {
	/**
	 * eg. localized value (mb) or fraction (inHg)
	 */
	override fun toString(): String = if(v == UNKNOWN_INT) UNKNOWN_STR else
		nFormat.format(if(isMetric) v else v * 0.0295301f)
}

@JvmInline
value class UV internal constructor(private val index: Int) {
	/**
	 * one of [UV_LOW], [UV_MEDIUM], [UV_HIGH], [UV_V_HIGH] or [UV_EXTREME]
	 */
	val level
		get() = when(index) {
			in 0..2 -> UV_LOW
			in 3..5 -> UV_MEDIUM
			in 6..7 -> UV_HIGH
			in 8..10 -> UV_V_HIGH
			else -> UV_EXTREME
		}

	/**
	 * localized index
	 */
	override fun toString(): String = nFormat.format(index)
}