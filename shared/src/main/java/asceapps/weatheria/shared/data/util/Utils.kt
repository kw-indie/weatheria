package asceapps.weatheria.shared.data.util

import asceapps.weatheria.shared.data.entity.DailyEntity
import asceapps.weatheria.shared.data.entity.HourlyEntity
import asceapps.weatheria.shared.data.model.Daily
import asceapps.weatheria.shared.data.model.Hourly
import java.time.Instant
import kotlin.math.min

internal fun countryFlag(cc: String): String {
	val offset = 0x1F1E6 - 0x41 // tiny A - normal A
	val cps = IntArray(2) { i -> cc[i].code + offset }
	return String(cps, 0, cps.size)
}

internal fun uvLevel(index: Int) = when(index) {
	in 0..2 -> UV_LOW
	in 3..5 -> UV_MODERATE
	in 6..7 -> UV_HIGH
	in 8..10 -> UV_V_HIGH
	else -> UV_EXTREME
}

internal fun moonPhase(age: Float) = when(age) {
	in 1f..6.38f -> MOON_WAXING_CRESCENT
	in 6.38f..8.38f -> MOON_FIRST_QUARTER
	in 8.38f..13.77f -> MOON_WAXING_GIBBOUS
	in 13.77f..15.77f -> MOON_FULL
	in 15.77f..21.15f -> MOON_WANING_GIBBOUS
	in 21.15f..23.15f -> MOON_THIRD_QUARTER
	in 23.15f..28.5f -> MOON_WANING_CRESCENT
	else -> MOON_NEW
}

internal fun today(daily: List<Daily>): Daily? {
	val today = thisDaySeconds()
	val index = daily.binarySearch { truncateToDaySeconds(it.date) - today }
	return if(index < 0) null else daily[index]
}

internal fun todayEntity(daily: List<DailyEntity>): DailyEntity? {
	val today = thisDaySeconds()
	val index = daily.binarySearch { truncateToDaySeconds(it.dt) - today }
	return if(index < 0) null else daily[index]
}

internal fun thisHour(hourly: List<Hourly>): Hourly? {
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

internal fun partOfDay(day: Daily): Pair<Int, Float> {
	val night = 0 to 0f
	val secondsInDay = 86_400f
	val nowSecondOfDay = currentSeconds() % secondsInDay
	// all values below are in fractions and not seconds
	val nowF = nowSecondOfDay / secondsInDay
	// sunrise/set are taken from approx of `today`
	val sunriseF = (day.sunrise.epochSecond % secondsInDay) / secondsInDay
	val sunsetF = (day.sunset.epochSecond % secondsInDay) / secondsInDay
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

private fun truncateToDaySeconds(i: Instant) = (i.epochSecond - i.epochSecond % 86_400).toInt()
private fun truncateToDaySeconds(sec: Int) = sec - sec % 86_400