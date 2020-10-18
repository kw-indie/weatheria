@file:JvmName("Utils")

package asceapps.weatheria.util

import android.content.Context
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import asceapps.weatheria.R
import asceapps.weatheria.data.WeatherInfo
import coil.load
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@BindingAdapter("icon")
fun loadIconFromUrl(view: ImageView, icon: String) {
	view.load("https://openweathermap.org/img/wn/$icon@2x.png") {
		// placeholder(res_id)
		// error(res_id)
		// transformations(CircleCropTransformation())
		crossfade(true)
	}
}

fun formatWind(context: Context, info: WeatherInfo, metric: Boolean) =
	with(context) {
		formatSpeed(this, info.current.windSpeed, metric) +
			getString(R.string.comma) + " " +
			degreesToDirectionLetters(this, info.current.windDir)
	}

fun formatNowDateTime(offsetSec: Int): String =
	OffsetDateTime.now(
		ZoneId.from(ZoneOffset.ofTotalSeconds(offsetSec))
	).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))

fun formatTime(unixSec: Int, offsetSec: Int): String =
	OffsetDateTime.ofInstant(
		Instant.ofEpochSecond(unixSec.toLong()),
		ZoneId.from(ZoneOffset.ofTotalSeconds(offsetSec))
	).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

fun formatTemp(kelvin: Float, metric: Boolean) =
	"${if(metric) (kelvin - 273.15f).toInt() else (kelvin * 1.8f - 459.67f).toInt()}°"

fun formatMinMax(min: Float, max: Float, metric: Boolean) =
	"${formatTemp(min, metric)}\t\t|\t\t${formatTemp(max, metric)}"

fun formatHumidity(humidity: Int) = "$humidity %"

fun formatSpeed(context: Context, mps: Float, metric: Boolean): String {
	val s: Float
	val unit: String
	if(metric) {
		s = mps
		unit = context.getString(R.string.metric_speed)
	} else {
		s = mps * 2.237f
		unit = context.getString(R.string.imp_speed)
	}
	return "%1$.2f %2\$s".format(s, unit)
}

fun isCoordinate(str: String) = str.matches(Regex(
	"^[-+]?([1-8]?\\d(\\.\\d+)?|90(\\.0+)?)\\s*,\\s*[-+]?(180(\\.0+)?|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d+)?)\$"
))

// each 45 degree will indicate a direction N, NW, W, SW, etc.
// rotate CCW 22 (45/2) degrees to make calculation easy to understand
private fun degreesToDirectionLetters(ctx: Context, degree: Int): String {
	val array = ctx.resources.getStringArray(R.array.dir_letters)
	return when((degree + 22) % 360) {
		in 0..44 -> array[0]
		in 45..89 -> array[1]
		in 90..134 -> array[2]
		in 135..179 -> array[3]
		in 180..224 -> array[4]
		in 225..269 -> array[5]
		in 270..314 -> array[6]
		else -> array[7]
	}
}