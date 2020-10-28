package asceapps.weatheria.model

import java.time.LocalTime

class Hourly(
	val hour: LocalTime,
	icon: String,
	temp: Int,
	pop: Int
) {

	val icon = Icon(icon)
	val temp = Temp(temp)
	val pop = Percent(pop)
}