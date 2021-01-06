package asceapps.weatheria.util

import android.text.format.DateUtils
import java.text.NumberFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

var metric = true
	private set
private var speedUnit = ""
// todo instead of asking to be set, go and read it from setting repo
fun setMetric(b: Boolean, speedUnit: String) {
	metric = b
	asceapps.weatheria.util.speedUnit = speedUnit
}

// prints at least 1 digit, sep each 3 digits, 0 to 2 decimal digits, rounds to nearest
private val nFormat = NumberFormat.getInstance().apply {
	minimumFractionDigits = 0
	maximumFractionDigits = 2
}
// adds localized percent char
private val pFormat = NumberFormat.getPercentInstance()
private val dtFormatter = DateTimeFormatter.ofPattern("EEE, d MMMM, h:mm a (xxx)")
private val tFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
// todo use 'ar-u-nu-arab' tag for arabic numbering locale
fun relativeTime(instant: Instant): CharSequence = DateUtils.getRelativeTimeSpanString(instant.toEpochMilli())
fun nowDt(offset: ZoneOffset): String = dtFormatter.format(OffsetDateTime.now(offset))
fun time(instant: Instant, offset: ZoneOffset): String = tFormatter.format(instant.atOffset(offset))
fun temp(deg: Int) = nFormat.format((if(metric) deg - 273.15f else deg * 1.8f - 459.67f).toInt()) + '°'
fun minMax(min: Int, max: Int) = temp(min).padEnd(5) + '|' + temp(max).padStart(5)
fun speed(mps: Float) = nFormat.format(if(metric) mps else mps * 2.237f) + ' ' + speedUnit
// our ratios are already from 0-100, this formatter expects fractions from 0-1
fun percent(ratio: Int): String = pFormat.format(ratio / 100f)