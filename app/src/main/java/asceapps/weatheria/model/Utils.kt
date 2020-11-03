@file:JvmName("Utils")
package asceapps.weatheria.model

import android.content.Context
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import asceapps.weatheria.R
import asceapps.weatheria.api.WeatherService
import coil.load
import java.text.DecimalFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.TemporalAccessor

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

	val format get() = "$v°"
}

class Speed(meterPerSec: Float) {

	val v = meterPerSec
		get() = if(metric) field else field * 2.237f

	val format get() = "${decimalFormat.format(v)!!} $gSpeedUnit"
}

class Percent(percent: Int) {

	val v = percent

	val format get() = "$v%"
}

class Icon(icon: String) {

	val v = icon

	val url get() = WeatherService.iconUrlFor(v)
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

@BindingAdapter("icon")
fun loadIconFromUrl(view: ImageView, url: String) {
	view.load(url) {
		// placeholder(res_id)
		// error(res_id)
		// transformations(CircleCropTransformation())
		crossfade(true)
	}
}

fun dtString(temporal: TemporalAccessor): String = dtFormatter.format(temporal)

fun tString(temporal: TemporalAccessor): String = tFormatter.format(temporal)

fun windString(c: Context, current: Current) = current.windSpeed.format +
	c.getString(R.string.comma) + " " +
	c.resources.getStringArray(R.array.dir_letters)[current.windDir.eighth]