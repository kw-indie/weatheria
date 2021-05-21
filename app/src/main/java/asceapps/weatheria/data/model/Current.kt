package asceapps.weatheria.data.model

class Current(
	val conditionIndex: Int,
	val icon: String,
	val temp: Int,
	val feelsLike: Int,
	val pressure: Int,
	val humidity: Int,
	val dewPoint: Int,
	val clouds: Int,
	val visibility: Int,
	val windSpeed: Float,
	val windDir: Int
)