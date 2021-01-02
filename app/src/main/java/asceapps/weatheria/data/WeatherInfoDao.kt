package asceapps.weatheria.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class WeatherInfoDao {

	// our use case forgives us for:
	// todo remove if we add pagination
	// 1- no pagination, since query result is expected to be small
	// 2- no separate calls for each table since all available info is almost always required
	// also, live data is retrieved async, no need for suspend

	// can make return type int[] to return ids of inserted rows, but  our api provides the ids as well.
	// no update for location since it's static once retrieved from api and inserted into db.

	@Transaction
	@Query("SELECT * FROM saved_locations ORDER BY pos DESC")
	abstract fun loadAll(): Flow<List<WeatherInfoEntity>>

	@Query("SELECT * FROM saved_locations ORDER BY pos DESC")
	abstract fun loadSavedLocations(): Flow<List<SavedLocationEntity>>

	@Transaction
	@Query("SELECT * FROM saved_locations WHERE id = :locationId")
	abstract fun load(locationId: Int): Flow<WeatherInfoEntity>

	@Transaction
	open suspend fun insert(
		l: SavedLocationEntity,
		c: CurrentEntity,
		h: List<HourlyEntity>,
		d: List<DailyEntity>
	) {
		l.pos = getMaxSavedLocationPos()
		insertLocation(l)
		insertCurrent(c)
		insertHourly(h)
		insertDaily(d)
	}

	@Transaction
	open suspend fun update(c: CurrentEntity, h: List<HourlyEntity>, d: List<DailyEntity>) {
		insertCurrent(c) // no need to delete old, it will always replace it
		deleteHourly(h[0].locationId) // delete old
		insertHourly(h)
		deleteDaily(d[0].locationId) // delete old
		insertDaily(d)
	}

	@Transaction
	open suspend fun reorder(l: SavedLocationEntity, toPos: Int) {
		if(l.pos == toPos) return
		//setPos(l.id, -1) // since pos column is not unique, we can skip this
		if(l.pos > toPos) // move down to smaller pos
			shiftPosUp(toPos, l.pos - 1) // shift up items in between
		else // move up to bigger pos
			shiftPosDown(toPos, l.pos + 1) // shift down items in between
		setPos(l.id, toPos)
	}

	@Transaction
	open suspend fun delete(locationId: Int, locationPos: Int) {
		delete(locationId)
		shiftPosDown(locationPos + 1) // shift down all rows with bigger pos
	}

	@Query("DELETE FROM saved_locations WHERE id != :locationId")
	abstract suspend fun retain(locationId: Int)

	@Query("DELETE FROM saved_locations")
	abstract suspend fun deleteAll()

	@Query("SELECT IFNULL(MAX(pos), 0) + 1 FROM saved_locations")
	abstract suspend fun getMaxSavedLocationPos(): Int

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	protected abstract suspend fun insertLocation(l: SavedLocationEntity)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	protected abstract suspend fun insertCurrent(c: CurrentEntity)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	protected abstract suspend fun insertHourly(h: List<HourlyEntity>)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	protected abstract suspend fun insertDaily(d: List<DailyEntity>)

	// rely on FK cascade to delete related info
	@Query("DELETE FROM saved_locations WHERE id = :locationId")
	protected abstract suspend fun delete(locationId: Int)

	@Query("UPDATE saved_locations SET pos = :pos WHERE id = :locationId")
	protected abstract suspend fun setPos(locationId: Int, pos: Int)

	@Query("UPDATE saved_locations SET pos = pos+1 WHERE pos BETWEEN :fromPos AND :toPos")
	protected abstract suspend fun shiftPosUp(fromPos: Int, toPos: Int)

	@Query("UPDATE saved_locations SET pos = pos-1 WHERE pos BETWEEN :fromPos AND :toPos")
	protected abstract suspend fun shiftPosDown(fromPos: Int, toPos: Int)

	@Query("UPDATE saved_locations SET pos = pos-1 WHERE pos >= :fromPos")
	protected abstract suspend fun shiftPosDown(fromPos: Int)

	@Query("DELETE FROM hourly WHERE locationId = :locationId")
	protected abstract suspend fun deleteHourly(locationId: Int)

	@Query("DELETE FROM daily WHERE locationId = :locationId")
	protected abstract suspend fun deleteDaily(locationId: Int)
}