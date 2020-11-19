package asceapps.weatheria.model

class Current(
	val conditionId: Int,
	val icon: String,
	temp: Int,
	feelsLike: Int,
	val pressure: Int,
	humidity: Int,
	dewPoint: Int,
	clouds: Int,
	val visibility: Int,
	windSpeed: Float,
	windDir: Int
) {

	val temp = Temp(temp)
	val feelsLike = Temp(feelsLike)
	val humidity = Percent(humidity)
	val dewPoint = Temp(dewPoint)
	val clouds = Percent(clouds)
	val windSpeed = Speed(windSpeed)
	val windDir = Direction(windDir)
}