package asceapps.weatheria.data.dao

import androidx.room.*
import asceapps.weatheria.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
abstract class WeatherInfoDao {

	@Transaction
	@Query("SELECT * FROM locations ORDER BY pos")
	abstract fun loadAll(): Flow<List<WeatherInfoEntity>>

	@Query("SELECT * FROM locations ORDER BY pos")
	abstract suspend fun getLocations(): List<LocationEntity>

	@Query("SELECT COUNT() FROM locations")
	abstract suspend fun getLocationsCount(): Int

	@Transaction
	open suspend fun insert(info: WeatherInfoEntity) {
		with(info) {
			upsertLocation(location)
			upsertCurrent(current)
			upsertHourly(hourly)
			upsertDaily(daily)
		}
	}

	@Transaction
	open suspend fun update(
		current: CurrentEntity,
		hourly: List<HourlyEntity>,
		daily: List<DailyEntity>
	) {
		updateLocation(current.locationId, current.dt)
		// we don't need to delete old rows now since we have prune
		upsertCurrent(current)
		upsertHourly(hourly)
		upsertDaily(daily)
	}

	@Transaction
	open suspend fun reorder(id: Int, fromPos: Int, toPos: Int) {
		if(fromPos == toPos) return
		// BETWEEN operator is inclusive-inclusive
		//setPos(l.id, -1) // since pos column is not unique, we can skip this
		if(fromPos < toPos) // from small index to bigger (move down in list)
			decPos(fromPos + 1, toPos) // dec pos of items in between (move up in list)
		else // from big index to smaller (move it up in list)
			incPos(toPos, fromPos - 1) // inc pos of items in between (move in list)
		setPos(id, toPos)
	}

	@Transaction
	open suspend fun delete(locationId: Int, locationPos: Int) {
		delete(locationId)
		decPos(locationPos + 1) // shift down all rows with bigger pos
	}

	@Query("DELETE FROM locations")
	abstract suspend fun deleteAll()

	@Transaction
	open suspend fun prune() {
		// this is scheduled to run at midnight.
		// since we are only dealing with utc times here, no need to bring in local dt
		val nowSeconds = (System.currentTimeMillis() / 1000).toInt()
		val thisHour = nowSeconds - nowSeconds % 3600
		val prevHour = thisHour - 3600
		val thisDay = nowSeconds - nowSeconds % (3600 * 24)
		// since we don't care what location it is, we wanna prune all locations,
		// we just run queries on the table directly.
		// current is valid for 1 hour, then it's approximated by hourly
		deleteAllCurrentBefore(prevHour) // 'prev' cuz dt might be 11:59, so it's still valid
		deleteAllHourlyBefore(thisHour) // 'this' cuz first hour is already 'this hour'
		deleteAllDailyBefore(thisDay) // same as hourly
	}

	@Query("DELETE FROM current WHERE dt < :unixSeconds")
	protected abstract suspend fun deleteAllCurrentBefore(unixSeconds: Int)

	@Query("DELETE FROM hourly WHERE dt < :unixSeconds")
	protected abstract suspend fun deleteAllHourlyBefore(unixSeconds: Int)

	@Query("DELETE FROM daily WHERE dt < :unixSeconds")
	protected abstract suspend fun deleteAllDailyBefore(unixSeconds: Int)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	protected abstract suspend fun upsertLocation(l: LocationEntity)

	@Query("UPDATE locations SET lastUpdate = :lastUpdate WHERE id = :id")
	protected abstract suspend fun updateLocation(id: Int, lastUpdate: Int)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	protected abstract suspend fun upsertCurrent(c: CurrentEntity)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	protected abstract suspend fun upsertHourly(h: List<HourlyEntity>)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	protected abstract suspend fun upsertDaily(d: List<DailyEntity>)

	// rely on FK cascade to delete related info
	@Query("DELETE FROM locations WHERE id = :locationId")
	protected abstract suspend fun delete(locationId: Int)

	@Query("UPDATE locations SET pos = :pos WHERE id = :locationId")
	protected abstract suspend fun setPos(locationId: Int, pos: Int)

	@Query("UPDATE locations SET pos = pos+1 WHERE pos BETWEEN :fromPos AND :toPos")
	protected abstract suspend fun incPos(fromPos: Int, toPos: Int)

	@Query("UPDATE locations SET pos = pos-1 WHERE pos >= :fromPos")
	protected abstract suspend fun decPos(fromPos: Int)

	@Query("UPDATE locations SET pos = pos-1 WHERE pos BETWEEN :fromPos AND :toPos")
	protected abstract suspend fun decPos(fromPos: Int, toPos: Int)

	@Query("DELETE FROM hourly WHERE locationId = :locationId")
	protected abstract suspend fun deleteHourly(locationId: Int)

	@Query("DELETE FROM daily WHERE locationId = :locationId")
	protected abstract suspend fun deleteDaily(locationId: Int)
}