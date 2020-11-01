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
	// 1- no pagination, since query result is expected to be small
	// 2- no separate calls for each table since all available info is almost always required
	// also, live data is retrieved async, no need for suspend

	// can make return type int[] to return ids of inserted rows, but  our api provides the ids as well.
	// no update for location since it's static once retrieved from api and inserted into db.

	@Transaction
	@Query("SELECT * FROM " + Table.LOCATIONS + " ORDER BY " + Column.POS + " DESC")
	abstract fun getAllInfo(): Flow<List<WeatherInfoEntity>>

	@Query("SELECT " + Column.LOC_ID + " FROM " + Table.LOCATIONS + " ORDER BY " + Column.POS + " ASC")
	abstract suspend fun getLocationIds(): List<Int>

	@Query("SELECT * FROM " + Table.CONDITIONS + " WHERE " + Column.CONDITION_ID + " IN (:ids)")
	abstract suspend fun getConditions(ids: List<Int>): List<WeatherConditionEntity>

	@Transaction
	@Query("SELECT * FROM " + Table.LOCATIONS + " WHERE " + Column.LOC_ID + " = :locationId")
	abstract suspend fun getInfo(locationId: Int): WeatherInfoEntity

	@Transaction
	open suspend fun insert(
		wc: List<WeatherConditionEntity>,
		l: LocationEntity,
		c: CurrentEntity,
		h: List<HourlyEntity>,
		d: List<DailyEntity>
	) {
		insertWeatherCondition(wc)
		l.pos = getMaxPosInLocations()
		insertLocation(l)
		insertCurrent(c)
		insertHourly(h)
		insertDaily(d)
	}

	@Transaction
	open suspend fun update(
		wc: List<WeatherConditionEntity>,
		c: CurrentEntity,
		h: List<HourlyEntity>,
		d: List<DailyEntity>
	) {
		insertWeatherCondition(wc)
		insertCurrent(c)
		insertHourly(h)
		insertDaily(d)
	}

	// rely on FK cascade to delete related info
	@Query("DELETE FROM " + Table.LOCATIONS + " WHERE " + Column.LOC_ID + " = :locationId")
	abstract suspend fun delete(locationId: Int)

	@Query("DELETE FROM " + Table.LOCATIONS + " WHERE " + Column.LOC_ID + " != :locationId")
	abstract suspend fun retain(locationId: Int)

	@Query("DELETE FROM " + Table.LOCATIONS)
	abstract suspend fun deleteAll()

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	protected abstract suspend fun insertWeatherCondition(wc: List<WeatherConditionEntity>)

	@Query("SELECT IFNULL(MAX(" + Column.POS + "), 0) + 1 FROM " + Table.LOCATIONS)
	protected abstract suspend fun getMaxPosInLocations(): Int

	@Insert(onConflict = OnConflictStrategy.ABORT)
	protected abstract suspend fun insertLocation(l: LocationEntity)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	protected abstract suspend fun insertCurrent(c: CurrentEntity)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	protected abstract suspend fun insertHourly(h: List<HourlyEntity>)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	protected abstract suspend fun insertDaily(d: List<DailyEntity>)
}