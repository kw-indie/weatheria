package asceapps.weatheria.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
	tableName = Table.CURRENT,
	primaryKeys = [Column.LOC_ID],
	foreignKeys = [
		ForeignKey(
			entity = LocationEntity::class,
			parentColumns = [Column.LOC_ID],
			childColumns = [Column.LOC_ID],
			onDelete = ForeignKey.CASCADE
		),
		ForeignKey(
			entity = WeatherConditionEntity::class,
			parentColumns = [Column.CONDITION_ID],
			childColumns = [Column.CONDITION_ID],
			onDelete = ForeignKey.RESTRICT
		)
	],
	indices = [Index(Column.CONDITION_ID)]
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