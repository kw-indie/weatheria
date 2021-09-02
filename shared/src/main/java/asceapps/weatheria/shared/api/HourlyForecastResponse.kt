package asceapps.weatheria.shared.api

import com.google.gson.annotations.SerializedName

/**
 * @see [CurrentWeatherResponse] for field meanings
 */
internal class HourlyForecastResponse(
	@SerializedName("EpochDateTime") val dt: Int,
	@Flatten("Temperature.Value") val temp_c: Float,
	@Flatten("RealFeelTemperature.Value") val feelsLike_c: Float,
	@SerializedName("WeatherIcon") val condition: Int,
	@SerializedName("IsDaylight") val isDay: Boolean,
	@Flatten("Wind.Speed.Value") val wind_kph: Float,
	@Flatten("Wind.Direction.Degrees") val wind_degrees: Int,
	@Flatten("TotalLiquid.Value") val precip_mm: Int,
	@SerializedName("RelativeHumidity") val humidity: Int,
	@Flatten("DewPoint.Value") val dewPoint_c: Float,
	@SerializedName("CloudCover") val clouds: Int,
	@Flatten("Visibility.Value") val vis_km: Float,
	@SerializedName("PrecipitationProbability") val pop: Int,
	@SerializedName("UVIndex") val uv: Int
)