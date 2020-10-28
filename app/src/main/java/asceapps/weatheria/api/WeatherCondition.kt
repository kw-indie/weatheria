package asceapps.weatheria.api

/**
 * @param id condition id
 * @param main short description of weather
 * @param description more detailed
 * @param icon icon id to use for download
 */
class WeatherCondition(
	val id: Int,
	val main: String,
	val description: String,
	val icon: String
)