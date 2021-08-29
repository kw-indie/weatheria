package asceapps.weatheria.shared.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
	tableName = "current",
	primaryKeys = ["locationId"],
	foreignKeys = [
		ForeignKey(
			entity = LocationEntity::class,
			parentColumns = ["id"],
			childColumns = ["locationId"],
			onDelete = ForeignKey.CASCADE
		)
	]
)
internal class CurrentEntity(
	val locationId: Int,
	val dt: Int,
	val temp: Int,
	val feelsLike: Int,
	val condition: Int,
	val isDay: Boolean,
	val windSpeed: Float,
	val windDir: Int,
	val pressure: Int,
	val humidity: Int,
	val dewPoint: Int,
	val clouds: Int,
	val visibility: Float,
	val uv: Int
)