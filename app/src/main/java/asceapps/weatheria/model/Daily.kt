package asceapps.weatheria.model

import java.time.LocalDate
import java.time.LocalTime

class Daily(
	val date: LocalDate,
	val sunrise: LocalTime,
	val sunset: LocalTime,
	val icon: String,
	min: Int,
	max: Int,
	pop: Int,
	val uvi: Float
) {

	val min = Temp(min)
	val max = Temp(max)
	val pop = Percent(pop)
}