package asceapps.weatheria.util

import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import asceapps.weatheria.api.WeatherService
import coil.load
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit

fun hideKeyboard(view: View) {
	ContextCompat.getSystemService(view.context, InputMethodManager::class.java)
		?.hideSoftInputFromWindow(view.windowToken, 0)
}

fun <T> LiveData<T>.debounce(timeoutMillis: Long, scope: CoroutineScope) = MediatorLiveData<T>().also {mld ->
	var job: Job? = null
	mld.addSource(this) {
		job?.cancel()
		job = scope.launch {
			delay(timeoutMillis)
			mld.value = value
		}
	}
}

private val conditionIds = intArrayOf(200, 201, 202, 210, 211, 212, 221, 230, 231, 232,
	300, 301, 302, 310, 311, 312, 313, 314, 321, 500, 501, 502, 503, 504, 511, 520, 521, 522, 531,
	600, 601, 602, 611, 612, 613, 615, 616, 620, 621, 622, 701, 711, 721, 731, 741, 751, 761, 762,
	771, 781, 800, 801, 802, 803, 804)

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

fun isCoordinate(str: String) = str.matches(Regex(
	"^[-+]?([1-8]?\\d(\\.\\d+)?|90(\\.0+)?)\\s*,\\s*[-+]?(180(\\.0+)?|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d+)?)\$"
))

fun cleanCoordinates(str: String) = str.replace(" ", "")
	.split(',')
	.map {it.toFloat()}
	.let {
		"%1$.3f,%2$.3f".format(it[0], it[1])
	}

fun dirIndex(deg: Int) = when((deg + 22) % 360) {
	in 0..44 -> 0 // E
	in 45..89 -> 1 // NE
	in 90..134 -> 2 // N
	in 135..179 -> 3 // NW
	in 180..224 -> 4 // W
	in 225..269 -> 5 // SW
	in 270..314 -> 6 // S
	else -> 7 // SE
}

fun moonPhase(instant: Instant, offset: ZoneOffset): Int {
	val day = HijrahDate.from(
		OffsetDateTime.ofInstant(instant, offset)
	)[ChronoField.DAY_OF_MONTH]
	val phase = when(day) {
		in 2..6 -> 0 //"Waxing Crescent Moon"
		in 6..8 -> 1 //"Quarter Moon"
		in 8..13 -> 2 //"Waxing Gibbous Moon"
		in 13..15 -> 3 //"Full Moon"
		in 15..21 -> 4 //"Waning Gibbous Moon"
		in 21..23 -> 5 //"Last Quarter Moon"
		in 23..28 -> 6 //"Waning Crescent Moon"
		else -> 7 //"New Moon" includes 28.53-29.5 and 0-1
	}
	return phase
}

fun moonPhase2(instant: Instant, offset: ZoneOffset): Int {
	// lunar cycle days
	val lunarCycle = 29.530588853
	// a reference new moon
	val ref = LocalDateTime.of(2000, 1, 6, 18, 14).atOffset(offset)
	// could ask for hour/min for a tiny bit of extra accuracy
	val now = OffsetDateTime.ofInstant(instant, offset)
	// this loses a number of hours of accuracy
	val days = ChronoUnit.DAYS.between(ref, now)
	val cycles = days / lunarCycle
	// take fractional part of cycles x full cycle = current lunation
	val lunation = (cycles % 1) * lunarCycle
	return when(lunation) {
		in 1.0..6.38 -> 0 //"Waxing Crescent Moon"
		in 6.38..8.38 -> 1 //"Quarter Moon"
		in 8.38..13.765 -> 2 //"Waxing Gibbous Moon"
		in 13.765..15.765 -> 3 //"Full Moon"
		in 15.765..21.148 -> 4 //"Waning Gibbous Moon"
		in 21.148..23.148 -> 5 //"Last Quarter Moon"
		in 23.148..28.53 -> 6 //"Waning Crescent Moon"
		else -> 7 //"New Moon" includes 28.53-29.5 and 0-1
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