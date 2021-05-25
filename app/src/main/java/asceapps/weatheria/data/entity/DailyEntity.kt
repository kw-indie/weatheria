package asceapps.weatheria.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
	tableName = "daily",
	primaryKeys = ["locationId", "dt"],
	foreignKeys = [
		ForeignKey(
			entity = LocationEntity::class,
			parentColumns = ["id"],
			childColumns = ["locationId"],
			onDelete = ForeignKey.CASCADE
		)
	]
)
class DailyEntity(
	val locationId: Int,
	val dt: Int,
	val minTemp: Int,
	val maxTemp: Int,
	val condition: Int,
	val windSpeed: Float,
	val precip: Float,
	val humidity: Int,
	val visibility: Int,
	val pop: Int,
	val uv: Int,
	val sunrise: Int,
	val sunset: Int,
	val moonrise: Int,
	val moonset: Int,
	val moonphase: Int
)