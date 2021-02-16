package asceapps.weatheria.ui.viewmodel

import android.text.format.DateUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import asceapps.weatheria.data.entity.LocationEntity
import asceapps.weatheria.data.entity.WeatherInfoEntity
import asceapps.weatheria.data.repo.SettingsRepo
import asceapps.weatheria.data.repo.WeatherInfoRepo
import asceapps.weatheria.model.Current
import asceapps.weatheria.model.Daily
import asceapps.weatheria.model.Hourly
import asceapps.weatheria.model.Location
import asceapps.weatheria.model.WeatherInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
	private val infoRepo: WeatherInfoRepo
): ViewModel() {

	val weatherInfoList = infoRepo.loadAll()
		.distinctUntilChanged()
		.map {list -> list.map {entityToModel(it)}}
		.onEach {
			_loading.value = false
		}.asLiveData()
	private val _loading = MutableLiveData(true)
	val loading: LiveData<Boolean> get() = _loading
	private val _error = MutableLiveData<Throwable>()
	val error: LiveData<Throwable> = _error

	fun addNewLocation(l: LocationEntity) {
		_loading.value = true
		try {
			viewModelScope.launch {
				infoRepo.fetch(l)
			}
		} catch(e: Exception) {
			e.printStackTrace()
			_error.value = e
		} finally {
			_loading.value = false
		}
	}

	fun refresh(l: Location) {
		_loading.value = true
		try {
			viewModelScope.launch {
				with(l) {infoRepo.refresh(id, lat, lng)}
			}
		} catch(e: Exception) {
			e.printStackTrace()
			_error.value = e
		} finally {
			_loading.value = false
		}
	}

	fun reorder(l: Location, toPos: Int) {
		viewModelScope.launch {
			infoRepo.reorder(l.id, l.pos, toPos)
		}
	}

	fun delete(l: Location) {
		viewModelScope.launch {
			infoRepo.delete(l.id, l.pos)
		}
	}

	fun deleteAll() {
		viewModelScope.launch {
			infoRepo.deleteAll()
		}
	}

	private fun entityToModel(info: WeatherInfoEntity): WeatherInfo {
		val location = with(info.location) {
			Location(id, lat, lng, name, country,
				ZoneOffset.ofTotalSeconds(info.location.zoneOffset),
				pos
			)
		}
		val daily = info.daily.map {
			with(it) {
				Daily(
					toInstant(dt),
					toInstant(sunrise),
					toInstant(sunset),
					conditionIcon(conditionId),
					minTemp,
					maxTemp,
					pop,
					uvi
				)
			}
		}
		val first3DaysDaytime = arrayListOf(
			with(daily[0]) {sunrise..sunset},
			with(daily[1]) {sunrise..sunset},
			with(daily[2]) {sunrise..sunset}
		)
		val hourly = info.hourly.map {
			with(it) {
				val hour = toInstant(dt)
				// hourly data are only from first 48 hours, starting from this hour not 00:00
				Hourly(
					hour,
					conditionIcon(conditionId, first3DaysDaytime.any {daytime -> hour in daytime}),
					temp,
					pop
				)
			}
		}
		val lastUpdate = toInstant(info.current.dt)
		val now = Instant.now()
		val elapsedTime = Duration.between(lastUpdate, now)
		val current = when {
			elapsedTime.toHours() < 1 -> { // fresh
				with(info.current) {
					Current(
						conditionIndex(conditionId),
						conditionIcon(conditionId, now in first3DaysDaytime[0]),
						temp,
						feelsLike,
						pressure,
						humidity,
						dewPoint,
						clouds,
						visibility,
						windSpeed,
						dirIndex(windDir),
						0 // high
					)
				}
			}
			elapsedTime.toHours() < info.hourly.size -> { // approximate from hourly
				with(info.hourly.last {it.dt < now.epochSecond}) {
					Current(
						conditionIndex(conditionId),
						conditionIcon(conditionId, first3DaysDaytime.any {daytime -> now in daytime}),
						temp,
						feelsLike,
						pressure,
						humidity,
						dewPoint,
						clouds,
						visibility,
						windSpeed,
						dirIndex(windDir),
						1 // medium
					)
				}
			}
			else -> { // approximate from daily, if no match, just use last day
				val day = info.daily.last {it.dt < now.epochSecond}
				// we can push all times to offset, but we'll get the same result with default offset
				val nowTime = LocalTime.now()
				val morn = LocalTime.of(6, 0)
				val noon = LocalTime.of(12, 0)
				val eve = LocalTime.of(18, 0)
				with(day) {
					Current(
						conditionIndex(conditionId),
						conditionIcon(conditionId, now.epochSecond in sunrise..sunset),
						when {
							nowTime.isBefore(morn) -> mornTemp
							nowTime.isBefore(noon) -> dayTemp
							nowTime.isBefore(eve) -> eveTemp
							else -> nightTemp
						},
						when {
							nowTime.isBefore(morn) -> mornFeel
							nowTime.isBefore(noon) -> dayFeel
							nowTime.isBefore(eve) -> eveFeel
							else -> nightFeel
						},
						pressure,
						humidity,
						dewPoint,
						clouds,
						info.hourly.last().visibility, // no visibility in daily, use last hour's
						windSpeed,
						dirIndex(windDir),
						2 // low
					)
				}
			}
		}
		return WeatherInfo(location, lastUpdate, current, hourly, daily)
	}

	companion object {

		// region formatting
		// prints at least 1 digit, sep each 3 digits, 0 to 2 decimal digits, rounds to nearest
		private val nFormat = NumberFormat.getInstance().apply {
			minimumFractionDigits = 0
			maximumFractionDigits = 2
		}
		// adds localized percent char
		private val pFormat = NumberFormat.getPercentInstance()
		private val dtFormatter = DateTimeFormatter.ofPattern("EEE, d MMMM, h:mm a (xxx)")
		private val tFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
		// use Locale.Builder().setLanguageTag("ar-u-nu-arab").build()
		// for testing arabic numbering locale
		fun relativeTime(millis: Int): CharSequence = DateUtils.getRelativeTimeSpanString(millis.toLong())
		fun nowDateTime(offset: ZoneOffset): String = dtFormatter.format(OffsetDateTime.now(offset))
		fun time(instant: Instant, offset: ZoneOffset): String = tFormatter.format(instant.atOffset(offset))
		fun temp(deg: Int, metric: Boolean) = nFormat.format(
			(if(metric) deg - 273.15f else deg * 1.8f - 459.67f).toInt()) + '°'

		fun minMax(min: Int, max: Int, metric: Boolean) = temp(min, metric).padEnd(5) + '|' + temp(max,
			metric).padStart(5)

		fun speed(mps: Float, metric: Boolean, speedUnit: String) = nFormat.format(
			if(metric) mps else mps * 2.237f) + ' ' + speedUnit
		// our ratios are already from 0-100, this formatter expects fractions from 0-1
		fun percent(ratio: Int): String = pFormat.format(ratio / 100f)
		// endregion

		private fun toInstant(epochSeconds: Int) = Instant.ofEpochSecond(epochSeconds.toLong())

		// region weather condition stuff
		private val conditionIds = intArrayOf(200, 201, 202, 210, 211, 212, 221, 230, 231, 232,
			300, 301, 302, 310, 311, 312, 313, 314, 321, 500, 501, 502, 503, 504, 511, 520, 521, 522, 531,
			600, 601, 602, 611, 612, 613, 615, 616, 620, 621, 622, 701, 711, 721, 731, 741, 751, 761, 762,
			771, 781, 800, 801, 802, 803, 804)

		private fun conditionIndex(conditionId: Int) = conditionIds.binarySearch(conditionId)

		private fun conditionIcon(conditionId: Int, isDay: Boolean? = null) =
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
				else -> throw IllegalArgumentException("no such condition")
			} + when(isDay) {
				true -> "d"
				false -> "n"
				else -> ""
			}
		// endregion

		private fun dirIndex(deg: Int) = when((deg + 22) % 360) {
			in 0..44 -> 0 // E
			in 45..89 -> 1 // NE
			in 90..134 -> 2 // N
			in 135..179 -> 3 // NW
			in 180..224 -> 4 // W
			in 225..269 -> 5 // SW
			in 270..314 -> 6 // S
			else -> 7 // SE
		}

		private fun moonPhase(instant: Instant, offset: ZoneOffset): Int {
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

		private fun moonPhase2(instant: Instant, offset: ZoneOffset): Int {
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
	}
}