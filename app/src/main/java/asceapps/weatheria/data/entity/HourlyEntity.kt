package asceapps.weatheria.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
	tableName = "hourly",
	primaryKeys = ["locationId", "dt"],
	foreignKeys = [
		ForeignKey(
			entity = SavedLocationEntity::class,
			parentColumns = ["id"],
			childColumns = ["locationId"],
			onDelete = ForeignKey.CASCADE
		)
	]
)
class HourlyEntity(
	val locationId: Int,
	val dt: Int,
	val conditionId: Int,
	val windSpeed: Float,
	val windDir: Int,
	val pressure: Int,
	val humidity: Int,
	val dewPoint: Int,
	val clouds: Int,
	val temp: Int,
	val feelsLike: Int,
	val visibility: Int,
	val pop: Int,
	val rain: Float? = null,
	val snow: Float? = null
)