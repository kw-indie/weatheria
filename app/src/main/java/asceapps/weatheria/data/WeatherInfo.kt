package asceapps.weatheria.data

import androidx.room.Embedded
import androidx.room.Relation
import asceapps.weatheria.api.CurrentResponse

class WeatherInfo(
	@Embedded val location: Location,
	@Relation(
		parentColumn = Location.COL_ID,
		entityColumn = CurrentWeather.COL_ID
	)
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