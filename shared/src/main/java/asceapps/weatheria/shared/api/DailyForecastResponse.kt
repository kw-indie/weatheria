package asceapps.weatheria.shared.api

import com.google.gson.annotations.SerializedName

/**
 * @see CurrentWeatherResponse for fields meanings
 */
internal class DailyForecastResponse(
	@SerializedName("DailyForecasts") val forecasts: List<DailyForecast>
) {
	class DailyForecast(
		@SerializedName("EpochDate") val dt: Int,
		@Flatten("Temperature.Minimum.Value") val minTemp_c: Float,
		@Flatten("Temperature.Maximum.Value") val maxTemp_c: Float,
		@SerializedName("Day") val day: PartOfDay,
		@SerializedName("Night") val night: PartOfDay,
		@Flatten("AirAndPollen.5.Value") val uv: Int,
		@Flatten("Sun.EpochRise") val sunrise: Int,
		@Flatten("Sun.EpochSet") val sunset: Int,
		@Flatten("Moon.EpochRise") val moonrise: Int,
		@Flatten("Moon.EpochSet") val moonset: Int,
		@Flatten("Moon.Age") val moonAge: Int,
	) {
		class PartOfDay(
			@SerializedName("Icon") val condition: Int,
			@Flatten("Wind.Direction.Degrees") val wind_degrees: Int,
			@Flatten("Wind.Speed.Value") val wind_kph: Float,
			@Flatten("TotalLiquid.Value") val precip_mm: Int,
			@SerializedName("PrecipitationProbability") val pop: Int,
			@SerializedName("CloudCover") val clouds: Int
		)
	}
}