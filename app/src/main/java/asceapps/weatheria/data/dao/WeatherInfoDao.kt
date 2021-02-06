package asceapps.weatheria.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import asceapps.weatheria.data.entity.CurrentEntity
import asceapps.weatheria.data.entity.DailyEntity
import asceapps.weatheria.data.entity.HourlyEntity
import asceapps.weatheria.data.entity.SavedLocationEntity
import asceapps.weatheria.data.entity.WeatherInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class WeatherInfoDao {

	@Transaction
	@Query("SELECT * FROM saved_locations ORDER BY pos")
	abstract fun loadAll(): Flow<List<WeatherInfoEntity>>

	@Query("SELECT * FROM saved_locations ORDER BY pos")
	abstract suspend fun savedLocations(): List<SavedLocationEntity>

	@Query("SELECT * FROM saved_locations ORDER BY pos")
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
	open suspend fun reorder(id: Int, fromPos: Int, toPos: Int) {
		if(fromPos == toPos) return
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
		pullPosDown(locationPos + 1) // shift down all rows with bigger pos
	}

	@Query("DELETE FROM saved_locations")
	abstract suspend fun deleteAll()

	@Query("SELECT IFNULL(MAX(pos), 0) FROM saved_locations")
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

	@Query("UPDATE saved_locations SET pos = pos+1 WHERE pos >= :fromPos")
	protected abstract suspend fun pushPosUp(fromPos: Int)

	@Query("UPDATE saved_locations SET pos = pos+1 WHERE pos BETWEEN :fromPos AND :toPos")
	protected abstract suspend fun incPos(fromPos: Int, toPos: Int)

	@Query("UPDATE saved_locations SET pos = pos-1 WHERE pos BETWEEN :fromPos AND :toPos")
	protected abstract suspend fun decPos(fromPos: Int, toPos: Int)

	@Query("UPDATE saved_locations SET pos = pos-1 WHERE pos >= :fromPos")
	protected abstract suspend fun pullPosDown(fromPos: Int)

	@Query("DELETE FROM hourly WHERE locationId = :locationId")
	protected abstract suspend fun deleteHourly(locationId: Int)

	@Query("DELETE FROM daily WHERE locationId = :locationId")
	protected abstract suspend fun deleteDaily(locationId: Int)
}