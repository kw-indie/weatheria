package asceapps.weatheria.api

import com.google.gson.annotations.SerializedName

data class CurrentResponse(
	val coord: Coord,
	val weather: List<Weather>,
	val main: Main,
	val wind: Wind,
	val sys: Sys,
	val timezone: Int,
	val id: Int,
	val name: String
)

data class FindResponse(
	val list: List<CurrentResponse>
)

data class Coord(
	val lat: Float,
	val lon: Float
)

data class Main(
	val temp: Float,
	@SerializedName("feels_like")
	val feel: Float,
	@SerializedName("temp_min")
	val min: Float,
	@SerializedName("temp_max")
	val max: Float,
	val humidity: Int
)

data class Sys(
	val country: String,
	val sunrise: Int,
	val sunset: Int
)

data class Weather(
	val main: String,
	val description: String,
	val icon: String
)

data class Wind(
	val speed: Float,
	val deg: Int
)