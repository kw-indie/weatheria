package asceapps.weatheria.model

import java.time.LocalTime

class Hourly(
	val time: LocalTime,
	val icon: String,
	temp: Int,
	pop: Int
) {

	val temp = Temp(temp)
	val pop = Percent(pop)
}