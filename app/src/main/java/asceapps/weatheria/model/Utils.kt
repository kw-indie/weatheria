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
private val dtFormatter = DateTimeFormatter.ofPattern("EEE, MMMM d, h:m a (xxx)")
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
fun loadIconFromUrl(view: ImageView, icon: String) {
	view.load(WeatherService.ICON_URL_PATTERN.format(icon)) {
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

fun iconOf(conditionId: Int, isDay: Boolean? = null) =
	when(conditionId) {
		in 200..232 -> "11" // thunderstorm
		in 300..321 -> "09" // drizzle
		in 500..504 -> "10" // rain
		511 -> "13" // freezing rain
		in 520..531 -> "09" // showers
		in 600..622 -> "13" // snow
		in 700..781 -> "50" // atmosphere
		800 -> "01" // clear sky
		801 -> "02" // few clouds
		802 -> "03" // scattered clouds
		803 -> "04" // broken clouds
		804 -> "04" // overcast clouds
		else -> throw IllegalAccessException("no such condition")
	} + when(isDay) {
		true -> "d"
		false -> "n"
		else -> ""
	}

private val conditionIds = intArrayOf(200, 201, 202, 210, 211, 212, 221, 230, 231, 232,
	300, 301, 302, 310, 311, 312, 313, 314, 321, 500, 501, 502, 503, 504, 511, 520, 521, 522, 531,
	600, 601, 602, 611, 612, 613, 615, 616, 620, 621, 622, 701, 711, 721, 731, 741, 751, 761, 762,
	771, 781, 800, 801, 802, 803, 804)

//todo dissolve weather condition entity? refactor base data?
fun indexOf(conditionId: Int) = conditionIds.binarySearch(conditionId)