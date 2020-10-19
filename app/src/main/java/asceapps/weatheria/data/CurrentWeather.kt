package asceapps.weatheria.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import asceapps.weatheria.api.CurrentResponse

/**
 * @param id city id. foreign key from [Location]
 * @param temp unit depends on api call. By default, in kelvin. also C or F
 * @param feelsLike ditto
 * @param min _daily_ minimum temp
 * @param max
 * @param main main weather condition (non-localized)
 * @param desc longer description (localized)
 * @param icon format: xxy, where xx: 2-digit condition number, and y: d/n for day/night
 * @param humidity in %
 * @param windDir wind direction in degrees
 * @param windSpeed by default, in m/s. also mph
 * @param sunrise unix seconds in UTC
 * @param sunset unix seconds in UTC
 */
@Entity(tableName = CurrentWeather.TABLE_NAME,
	foreignKeys = [
		ForeignKey(
			entity = Location::class,
			parentColumns = [Location.COL_ID],
			childColumns = [CurrentWeather.COL_ID],
			onDelete = ForeignKey.CASCADE)
	]
)
data class CurrentWeather(
	@PrimaryKey @ColumnInfo(name = COL_ID) val id: Int,
	val temp: Int,
	@ColumnInfo(name = COL_FEEL) val feelsLike: Int,
	val min: Int,
	val max: Int,
	val main: String,
	val desc: String,
	val icon: String,
	val humidity: Int,
	@ColumnInfo(name = COL_WIND_DIR) val windDir: Int,
	@ColumnInfo(name = COL_WIND_SPEED) val windSpeed: Float,
	val sunrise: Int,
	val sunset: Int
) {

	companion object {

		const val TABLE_NAME = "current_weather_table"
		const val COL_ID = "location_id"
		const val COL_FEEL = "feels_like"
		const val COL_WIND_DIR = "wind_dir"
		const val COL_WIND_SPEED = "wind_speed"
	}

	constructor(resp: CurrentResponse): this(
		resp.id,
		resp.main.temp.toInt(),
		resp.main.feel.toInt(),
		resp.main.min.toInt(),
		resp.main.max.toInt(),
		resp.weather[0].main,
		resp.weather[0].description,
		resp.weather[0].icon,
		resp.main.humidity,
		resp.wind.deg,
		resp.wind.speed,
		resp.sys.sunrise,
		resp.sys.sunset
	)
}


