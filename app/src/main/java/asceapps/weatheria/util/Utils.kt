package asceapps.weatheria.util

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import asceapps.weatheria.data.WeatherService
import coil.load
import java.time.Instant

private val conditionIds = intArrayOf(200, 201, 202, 210, 211, 212, 221, 230, 231, 232,
	300, 301, 302, 310, 311, 312, 313, 314, 321, 500, 501, 502, 503, 504, 511, 520, 521, 522, 531,
	600, 601, 602, 611, 612, 613, 615, 616, 620, 621, 622, 701, 711, 721, 731, 741, 751, 761, 762,
	771, 781, 800, 801, 802, 803, 804)

fun isCoordinate(str: String) = str.matches(Regex(
	"^[-+]?([1-8]?\\d(\\.\\d+)?|90(\\.0+)?)\\s*,\\s*[-+]?(180(\\.0+)?|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d+)?)\$"
))

fun conditionIcon(conditionId: Int, isDay: Boolean? = null) =
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

fun conditionIndex(conditionId: Int) = conditionIds.binarySearch(conditionId)
fun Int.toInstant(): Instant = Instant.ofEpochSecond(this.toLong())

// value in 0-7: 0=E, 1=NE...7=SE
fun dirIndex(deg: Int) = when((deg + 22) % 360) {
	in 0..44 -> 0
	in 45..89 -> 1
	in 90..134 -> 2
	in 135..179 -> 3
	in 180..224 -> 4
	in 225..269 -> 5
	in 270..314 -> 6
	else -> 7
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