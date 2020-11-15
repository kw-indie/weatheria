package asceapps.weatheria.db

import androidx.room.Embedded
import androidx.room.Relation

class WeatherInfoEntity(
	@Embedded val location: SavedLocationEntity,
	@Relation(
		parentColumn = Column.ID,
		entityColumn = Column.LOC_ID
	)
	val current: CurrentEntity,
	@Relation(
		parentColumn = Column.ID,
		entityColumn = Column.LOC_ID
	)
	val hourly: List<HourlyEntity>,
	@Relation(
		parentColumn = Column.ID,
		entityColumn = Column.LOC_ID
	)
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