package asceapps.weatheria.data

import androidx.room.Embedded
import androidx.room.Relation

class WeatherInfoEntity(
	@Embedded val location: SavedLocationEntity,
	@Relation(parentColumn = "id", entityColumn = "locationId")
	val current: CurrentEntity,
	@Relation(parentColumn = "id", entityColumn = "locationId")
	val hourly: List<HourlyEntity>,
	@Relation(parentColumn = "id", entityColumn = "locationId")
	val daily: List<DailyEntity>
) {

	override fun hashCode() = location.id + current.dt

	// for flow.distinctUntilChanged
	override fun equals(other: Any?): Boolean {
		return other != null &&
			other is WeatherInfoEntity &&
			location.id == other.location.id &&
			current.dt == other.current.dt
	}
}