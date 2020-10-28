package asceapps.weatheria.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import asceapps.weatheria.api.CurrentResponse
import com.google.gson.annotations.SerializedName

/**
 * @param id city id from OpenWeatherMap api
 * @param lat latitude
 * @param lng longitude
 * @param city localized
 * @param country 2-letter code, non-localized
 * @param timezone difference in seconds from UTC
 * @param creationTime when this location was first added
 * @param updatedAt when this location's weather info was last updated
 */
@Entity(tableName = Location.TABLE_NAME)
class Location(
	@PrimaryKey val id: Int,
	val lat: Float,
	val lng: Float,
	val city: String,
	val country: String,
	val timezone: Int,
	@ColumnInfo(name = COL_CREATED_AT) val creationTime: Long = System.currentTimeMillis(),
	@ColumnInfo(name = COL_UPDATED_AT) val updatedAt: Long = System.currentTimeMillis()
) {

	companion object {

		const val TABLE_NAME = "locations_table"
		const val COL_ID = "id"
		const val COL_CREATED_AT = "created_at"
		const val COL_UPDATED_AT = "updated_at"
	}

	constructor(resp: CurrentResponse): this(
		resp.id,
		resp.coord.lat,
		resp.coord.lon,
		resp.name,
		resp.sys.country,
		resp.timezone
	)

	class Update(resp: CurrentResponse) {

		val id: Int = resp.id

		@ColumnInfo(name = COL_UPDATED_AT)
		@SerializedName(COL_UPDATED_AT)
		val updatedAt: Long = System.currentTimeMillis()
	}
}