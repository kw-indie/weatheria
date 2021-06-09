package asceapps.weatheria.data.api

import com.google.gson.annotations.SerializedName

class ForecastResponse(
	val location: Location,
	val current: Current,
	val forecast: Forecast,
	val alerts: Alerts?
) {

	/**
	 * @param country full country name, not just 2 letters
	 * @param tz_id timezone id, eg. Europe/London
	 * @param localtime_epoch seconds since epoch
	 */
	class Location(
		val lat: Float,
		val lon: Float,
		val name: String,
		val country: String,
		val tz_id: String,
		val localtime_epoch: Int
	)

	/**
	 * @param last_updated_epoch unix seconds of data update on server
	 * @param condition weather condition
	 * @param is_day 1 or 0, true or false
	 * @param pressure_mb in millibars
	 * @param humidity as percentage [0-100]
	 * @param cloud same as humidity
	 * @param vis_km visibility
	 * @param uv index: 1-2 Low, 3-5 Moderate, 6-7: High, 8-10: Very high, 11+: Extreme
	 */
	class Current(
		val last_updated_epoch: Int,
		val temp_c: Float,
		val feelslike_c: Float,
		val condition: Condition,
		val is_day: Int, // 1 or 0
		val wind_kph: Float,
		val wind_degree: Int,
		val pressure_mb: Float,
		val precip_mm: Float,
		val humidity: Int,
		val cloud: Int,
		val vis_km: Float,
		val uv: Float,
		val air_quality: AirQuality?
	)

	class Condition(val code: Int)

	/**
	 * All are measured in micrograms/meter^3
	 * @param co carbon monoxide
	 * @param no2 nitrogen dioxide
	 * @param o3 ozone
	 * @param so2 sulfur dioxide
	 * @param pm2_5 particulate matter <= 2.5 micrometers in diameter
	 * @param pm10 same but 10 micrometers
	 * @param epa_index US EPA standard: 1: Good, 2: Moderate, 3: Unhealthy for sensitive groups,
	 * 4: Unhealthy, 5: Very unhealthy, 6: Hazardous
	 * @param defra_index UK Defra index: 1-3: Low (pollution), 4-6: Moderate, 7-9: High, 10: Very high
	 */
	class AirQuality(
		val co: Float,
		val no2: Float,
		val o3: Float,
		val so2: Float,
		val pm2_5: Float,
		val pm10: Float,
		@SerializedName("us-epa-index") val epa_index: Int,
		@SerializedName("gb-defra-index") val defra_index: Int
	)

	class Forecast(val forecastday: List<ForecastDay>)

	/**
	 * @param date_epoch dt at the start of this forecast day
	 * @param hour hourly forecast (24)
	 * @param day day info
	 * @param astro astro info
	 */
	class ForecastDay(
		val date_epoch: Int,
		val hour: List<Hour>,
		val day: Day,
		val astro: Astro
	)

	/**
	 * @param time_epoch a random? past time < 1 day
	 * @param chance_of_rain as percentage (0-100)
	 */
	class Hour(
		val time_epoch: Int,
		val temp_c: Float,
		val feelslike_c: Float,
		val condition: Condition,
		val is_day: Int,
		val wind_kph: Float,
		val wind_degree: Int,
		val pressure_mb: Float,
		val precip_mm: Float,
		val humidity: Int,
		val dewpoint_c: Float,
		val cloud: Int,
		val vis_km: Float,
		val chance_of_rain: Int,
		val chance_of_snow: Int,
		val uv: Float
	)

	class Day(
		val mintemp_c: Float,
		val maxtemp_c: Float,
		val condition: Condition,
		val maxwind_kph: Float,
		val totalprecip_mm: Float,
		val avghumidity: Float,
		val avgvis_km: Float,
		val daily_chance_of_rain: Int,
		val daily_chance_of_snow: Int,
		val uv: Float,
	)

	/**
	 * All times are in local format = hh:mm a
	 * @param moon_phase 8 phases
	 */
	class Astro(
		val sunrise: String,
		val sunset: String,
		val moonrise: String,
		val moonset: String,
		val moon_phase: String
	)

	class Alerts(val alert: List<Alert>)

	/**
	 * Language may be local
	 * @param headline summary. eg. Flood warning from 00:00 until 00:00 by XYZ
	 * @param severity eg. Moderate
	 * @param event eg. Flood warning
	 * @param effective eg. 2021-01-05T21:47:00-05:00
	 * @param expires same as above
	 * @param desc long description
	 * @param instruction long instructions
	 */
	class Alert(
		val headline: String,
		val severity: String,
		val event: String,
		val effective: String,
		val expires: String,
		val desc: String,
		val instruction: String
	)
}