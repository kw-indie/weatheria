package asceapps.weatheria.db

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
	@Query("SELECT * FROM " + Table.SAVED_LOCATIONS + " ORDER BY " + Column.POS + " DESC")
	abstract fun getAllInfo(): Flow<List<WeatherInfoEntity>>

	@Query("SELECT * FROM " + Table.SAVED_LOCATIONS + " ORDER BY " + Column.POS + " DESC")
	abstract fun getSavedLocations(): Flow<List<SavedLocationEntity>>

	@Transaction
	@Query("SELECT * FROM " + Table.SAVED_LOCATIONS + " WHERE " + Column.ID + " = :locationId")
	abstract fun getInfo(locationId: Int): Flow<WeatherInfoEntity>

	@Query("SELECT * FROM " + Table.LOCATIONS +
		" WHERE " + Column.NAME + " LIKE :locationName || '%' LIMIT 5")
	abstract suspend fun find(locationName: String): List<LocationEntity>

	@Query("SELECT * FROM " + Table.LOCATIONS + " WHERE " +
		Column.LAT + " BETWEEN :latB AND :latT AND " + Column.LNG + " BETWEEN :lngL AND :lngR " +
		"ORDER BY (" + Column.LAT + "-:lat)*(" + Column.LAT + "-:lat)+" +
		"(" + Column.LNG + "-:lng)*(" + Column.LNG + "-:lng) LIMIT 5"
	)
	abstract suspend fun find(
		lat: Float, lng: Float, latB: Float, latT: Float, lngL: Float, lngR: Float
	): List<LocationEntity>

	@Transaction
	open suspend fun insert(
		l: SavedLocationEntity,
		c: CurrentEntity,
		h: List<HourlyEntity>,
		d: List<DailyEntity>
	) {
		l.pos = getMaxSavedLocationPos()
		insertSavedLocation(l)
		insertCurrent(c)
		insertHourly(h)
		insertDaily(d)
	}

	@Transaction
	open suspend fun update(
		c: CurrentEntity,
		h: List<HourlyEntity>,
		d: List<DailyEntity>
	) {
		// current doesn't need to delete old, its one row will always be replaced
		insertCurrent(c)
		// delete old data, using location id from any related data
		deleteHourly(c.locationId)
		insertHourly(h)
		deleteDaily(c.locationId)
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

	@Query("DELETE FROM " + Table.SAVED_LOCATIONS + " WHERE " + Column.ID + " != :locationId")
	abstract suspend fun retain(locationId: Int)

	@Query("DELETE FROM " + Table.SAVED_LOCATIONS)
	abstract suspend fun deleteAll()

	@Query("SELECT IFNULL(MAX(" + Column.POS + "), 0) + 1 FROM " + Table.SAVED_LOCATIONS)
	protected abstract suspend fun getMaxSavedLocationPos(): Int

	@Insert(onConflict = OnConflictStrategy.ABORT)
	protected abstract suspend fun insertSavedLocation(l: SavedLocationEntity)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	protected abstract suspend fun insertCurrent(c: CurrentEntity)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	protected abstract suspend fun insertHourly(h: List<HourlyEntity>)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	protected abstract suspend fun insertDaily(d: List<DailyEntity>)

	// rely on FK cascade to delete related info
	@Query("DELETE FROM " + Table.SAVED_LOCATIONS + " WHERE " + Column.ID + " = :locationId")
	protected abstract suspend fun delete(locationId: Int)

	@Query("UPDATE " + Table.SAVED_LOCATIONS + " SET " + Column.POS + " = :pos " +
		"WHERE " + Column.ID + " = :locationId")
	protected abstract suspend fun setPos(locationId: Int, pos: Int)

	@Query("UPDATE " + Table.SAVED_LOCATIONS + " SET " + Column.POS + " = " + Column.POS + "+1 " +
		"WHERE " + Column.POS + " BETWEEN :fromPos AND :toPos")
	protected abstract suspend fun shiftPosUp(fromPos: Int, toPos: Int)

	@Query("UPDATE " + Table.SAVED_LOCATIONS + " SET " + Column.POS + " = " + Column.POS + "-1 " +
		"WHERE " + Column.POS + " BETWEEN :fromPos AND :toPos")
	protected abstract suspend fun shiftPosDown(fromPos: Int, toPos: Int)

	@Query("UPDATE " + Table.SAVED_LOCATIONS + " SET " + Column.POS + " = " + Column.POS + "-1 " +
		"WHERE " + Column.POS + " >= :fromPos")
	protected abstract suspend fun shiftPosDown(fromPos: Int)

	@Query("DELETE FROM " + Table.HOURLY + " WHERE " + Column.LOC_ID + " = :locationId")
	protected abstract suspend fun deleteHourly(locationId: Int)

	@Query("DELETE FROM " + Table.DAILY + " WHERE " + Column.LOC_ID + " = :locationId")
	protected abstract suspend fun deleteDaily(locationId: Int)
}