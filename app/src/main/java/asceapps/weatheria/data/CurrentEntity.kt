package asceapps.weatheria.data

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
	tableName = "current",
	primaryKeys = ["locationId"],
	foreignKeys = [
		ForeignKey(
			entity = SavedLocationEntity::class,
			parentColumns = ["id"],
			childColumns = ["locationId"],
			onDelete = ForeignKey.CASCADE
		)
	]
)
class CurrentEntity(
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
	val rain: Float? = null,
	val snow: Float? = null
)