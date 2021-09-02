package asceapps.weatheria.shared.data.repo

import asceapps.weatheria.shared.data.entity.DailyEntity
import asceapps.weatheria.shared.data.entity.HourlyEntity
import asceapps.weatheria.shared.data.model.Daily
import asceapps.weatheria.shared.data.model.Hourly
import asceapps.weatheria.shared.data.model.WeatherInfo
import java.time.Instant
import kotlin.math.min

fun today(daily: List<Daily>): Daily? {
	val today = thisDaySeconds()
	val index = daily.binarySearch { truncateToDaySeconds(it.date) - today }
	return if(index < 0) null else daily[index]
}

internal fun todayEntity(daily: List<DailyEntity>): DailyEntity? {
	val today = thisDaySeconds()
	val index = daily.binarySearch { truncateToDaySeconds(it.dt) - today }
	return if(index < 0) null else daily[index]
}

fun thisHour(hourly: List<Hourly>): Hourly? {
	val thisHour = thisHourSeconds() + 3600 // start from next hour
	val index = hourly.binarySearch { (it.hour.epochSecond - thisHour).toInt() }
	return if(index < 0) null else hourly[index]
}

internal fun thisHourEntity(hourly: List<HourlyEntity>): HourlyEntity? {
	val thisHour = thisHourSeconds()
	val index = hourly.binarySearch { it.dt - thisHour }
	return if(index < 0) null else hourly[index]
}

internal fun currentSeconds() = (System.currentTimeMillis() / 1000).toInt()

internal fun thisHourSeconds() = (System.currentTimeMillis() / 1000).toInt().let { it - it % 3_600 }

internal fun thisDaySeconds() = truncateToDaySeconds((System.currentTimeMillis() / 1000).toInt())

private fun truncateToDaySeconds(i: Instant) = (i.epochSecond - i.epochSecond % 86_400).toInt()
private fun truncateToDaySeconds(sec: Int) = sec - sec % 86_400

/**
 * returns one of:
 *  * 0:0 = night,
 *  * 1:f = pre dawn,
 *  * 2:f = post dawn,
 *  * 3:0 = day,
 *  * 4:f = pre dusk,
 *  * 5:f = post dusk.
 *
 * where f is fraction of transition between day/night and dawn/dusk
 */
fun partOfDay(info: WeatherInfo): Pair<Int, Float> {
	val today = info.today
	val night = 0 to 0f
	val secondsInDay = 86_400f
	val nowSecondOfDay = currentSeconds() % secondsInDay
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