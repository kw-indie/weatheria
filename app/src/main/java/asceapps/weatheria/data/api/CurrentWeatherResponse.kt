package asceapps.weatheria.data.api

import com.google.gson.annotations.SerializedName

/**
 * According to accuWeather, uv levels go like this:
 * * 0.0-2.5 = low
 * * 2.5-5.5 = med
 * * 5.5-7.5 = high
 * * 7.5-10.5 = v high
 *
 * @param wind_degree mathematical, aka CCW and 0 is E
 */
class CurrentWeatherResponse(
	@SerializedName("EpochTime") val lastUpdate: Int,
	@Flatten("Temperature.Metric.Value") val temp_c: Float,
	@Flatten("RealFeelTemperature.Metric.Value") val feelsLike_c: Float,
	@SerializedName("WeatherIcon") val condition: Int,
	@SerializedName("IsDayTime") val isDay: Boolean,
	@Flatten("Wind.Direction.Speed.Metric.Value") val wind_kph: Float,
	@Flatten("Wind.Direction.Degrees") val wind_degree: Int,
	@Flatten("Pressure.Metric.Value") val pressure_mb: Int,
	@Flatten("Precip1hr.Metric.Value") val precip_mm: Int,
	@SerializedName("RelativeHumidity") val humidity: Int,
	@Flatten("DewPoint.Metric.Value") val dewPoint_c: Float,
	@SerializedName("CloudCover") val clouds: Int,
	@Flatten("Visibility.Metric.Value") val vis_km: Float,
	@SerializedName("UVIndex") val uv: Int
)