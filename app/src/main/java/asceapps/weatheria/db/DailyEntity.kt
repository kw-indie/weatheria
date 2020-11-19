package asceapps.weatheria.db

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
	tableName = Table.DAILY,
	primaryKeys = [Column.LOC_ID, Column.DT],
	foreignKeys = [
		ForeignKey(
			entity = SavedLocationEntity::class,
			parentColumns = [Column.ID],
			childColumns = [Column.LOC_ID],
			onDelete = ForeignKey.CASCADE
		)
	]
)
class DailyEntity(
	locationId: Int,
	dt: Int,
	conditionId: Int,
	windSpeed: Float,
	windDir: Int,
	pressure: Int,
	humidity: Int,
	dewPoint: Int,
	clouds: Int,
	val minTemp: Int,
	val maxTemp: Int,
	val mornTemp: Int,
	val dayTemp: Int,
	val eveTemp: Int,
	val nightTemp: Int,
	val mornFeel: Int,
	val dayFeel: Int,
	val eveFeel: Int,
	val nightFeel: Int,
	val sunrise: Int,
	val sunset: Int,
	val pop: Int,
	val uvi: Float,
	val rain: Float? = null,
	val snow: Float? = null
): BaseData(locationId, dt, conditionId, windSpeed, windDir, pressure, humidity, dewPoint, clouds)