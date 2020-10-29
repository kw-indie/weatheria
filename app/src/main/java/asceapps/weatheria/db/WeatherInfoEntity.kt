package asceapps.weatheria.db

import androidx.room.Embedded
import androidx.room.Relation

class WeatherInfoEntity(
	@Embedded val location: LocationEntity,
	@Relation(
		parentColumn = Column.LOC_ID,
		entityColumn = Column.LOC_ID
	)
	val current: CurrentEntity,
	@Relation(
		parentColumn = Column.LOC_ID,
		entityColumn = Column.LOC_ID
	)
	val hourly: List<HourlyEntity>,
	@Relation(
		parentColumn = Column.LOC_ID,
		entityColumn = Column.LOC_ID
	)
	val daily: List<DailyEntity>
)