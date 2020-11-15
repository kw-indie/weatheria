package asceapps.weatheria.model

class Current(
	val dt: Int,
	val desc: String,
	icon: String,
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

	val icon = Icon(icon)
	val temp = Temp(temp)
	val feelsLike = Temp(feelsLike)
	val humidity = Percent(humidity)
	val dewPoint = Temp(dewPoint)
	val clouds = Percent(clouds)
	val windSpeed = Speed(windSpeed)
	val windDir = Direction(windDir)
}