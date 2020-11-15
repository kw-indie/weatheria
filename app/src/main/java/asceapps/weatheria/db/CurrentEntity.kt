package asceapps.weatheria.db

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
	tableName = Table.CURRENT,
	primaryKeys = [Column.LOC_ID],
	foreignKeys = [
		ForeignKey(
			entity = SavedLocationEntity::class,
			parentColumns = [Column.ID],
			childColumns = [Column.LOC_ID],
			onDelete = ForeignKey.CASCADE
		),
		ForeignKey(
			entity = WeatherConditionEntity::class,
			parentColumns = [Column.ID],
			childColumns = [Column.CONDITION_ID],
			onDelete = ForeignKey.RESTRICT
		)
	]
)
class CurrentEntity(
	locationId: Int,
	dt: Int,
	conditionId: Int,
	windSpeed: Float,
	windDir: Int,
	pressure: Int,
	humidity: Int,
	dewPoint: Int,
	clouds: Int,
	val temp: Int,
	val feelsLike: Int,
	val visibility: Int,
	val rain: Float? = null,
	val snow: Float? = null
): BaseData(locationId, dt, conditionId, windSpeed, windDir, pressure, humidity, dewPoint, clouds)