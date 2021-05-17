package asceapps.weatheria.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import asceapps.weatheria.data.entity.CurrentEntity
import asceapps.weatheria.data.entity.DailyEntity
import asceapps.weatheria.data.entity.HourlyEntity
import asceapps.weatheria.data.entity.LocationEntity
import asceapps.weatheria.data.entity.WeatherInfoEntity
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
		zoneOffset: Int,
		current: CurrentEntity,
		hourly: List<HourlyEntity>,
		daily: List<DailyEntity>
	) {
		updateLocation(current.locationId, zoneOffset)
		upsertCurrent(current)
		deleteHourly(current.locationId) // delete old
		upsertHourly(hourly)
		deleteDaily(current.locationId) // delete old
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
		val nowSeconds = (System.currentTimeMillis() / 1000).toInt()
		val prevHour = nowSeconds - 60 * 60
		val prevDay = nowSeconds - 60 * 60 * 24
		val locations = getLocations()
		locations.forEach { l ->
			val lastHour = getLastHour(l.id)
			if(nowSeconds < lastHour) {
				deleteAllHoursBefore(l.id, prevHour)
			} else {
				deleteAllHoursButLast(l.id)
			}
			val lastDay = getLastDay(l.id)
			if(nowSeconds < lastDay) {
				deleteAllDaysBefore(l.id, prevDay)
			} else {
				deleteAllDaysButLast(l.id)
			}
		}
	}

	@Query("SELECT MAX(dt) from hourly WHERE locationId = :locationId")
	protected abstract suspend fun getLastHour(locationId: Int): Int

	@Query("DELETE FROM hourly WHERE locationId = :locationId AND dt < :unixSeconds")
	protected abstract suspend fun deleteAllHoursBefore(locationId: Int, unixSeconds: Int)

	@Query("DELETE FROM hourly WHERE locationId = :locationId AND dt != (SELECT MAX(dt) FROM hourly WHERE locationId = :locationId)")
	protected abstract suspend fun deleteAllHoursButLast(locationId: Int)

	@Query("SELECT MAX(dt) from daily WHERE locationId = :locationId")
	protected abstract suspend fun getLastDay(locationId: Int): Int

	@Query("DELETE FROM daily WHERE locationId = :locationId AND dt < :unixSeconds")
	protected abstract suspend fun deleteAllDaysBefore(locationId: Int, unixSeconds: Int)

	@Query("DELETE FROM daily WHERE locationId = :locationId AND dt != (SELECT MAX(dt) FROM daily WHERE locationId = :locationId)")
	protected abstract suspend fun deleteAllDaysButLast(locationId: Int)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	protected abstract suspend fun upsertLocation(l: LocationEntity)

	@Query("UPDATE locations SET zoneOffset = :zoneOffset WHERE id = :id")
	protected abstract suspend fun updateLocation(id: Int, zoneOffset: Int)

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