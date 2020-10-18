package asceapps.weatheria.data

import androidx.room.Embedded
import asceapps.weatheria.api.CurrentResponse

data class WeatherInfo(
	@Embedded
	val location: Location,
	@Embedded
	val current: CurrentWeather
) {

	constructor(resp: CurrentResponse): this(
		Location(resp),
		CurrentWeather(resp)
	)

	class Update(resp: CurrentResponse) {

		val location = Location.Update(resp)
		val current = CurrentWeather(resp)
	}
}