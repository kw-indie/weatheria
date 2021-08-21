package asceapps.weatheria.data.model

// todo make values nullable for approximation
// todo make views show appropriate ui for null values
class Current(
	val conditionIndex: Int,
	val icon: Int,
	val temp: Int,
	val feelsLike: Int,
	val pressure: Int,
	val humidity: Int,
	val dewPoint: Int,
	val clouds: Int,
	val visibility: Float,
	val windSpeed: Float,
	val windDir: Int,
	val uv: Int
)