package asceapps.weatheria.shared.data.entity

import androidx.room.Embedded
import androidx.room.Relation

class WeatherInfoEntity(
	@Embedded val location: LocationEntity,
	@Relation(parentColumn = "id", entityColumn = "locationId")
	val current: CurrentEntity,
	@Relation(parentColumn = "id", entityColumn = "locationId")
	val hourly: List<HourlyEntity>,
	@Relation(parentColumn = "id", entityColumn = "locationId")
	val daily: List<DailyEntity>
) {

	// for distinctUntilChanged
	override fun equals(other: Any?): Boolean {
		return other != null &&
			other is WeatherInfoEntity &&
			location.id == other.location.id &&
			location.lastUpdate == other.location.lastUpdate
	}
}