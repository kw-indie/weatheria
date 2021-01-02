package asceapps.weatheria.data

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

	@Query("SELECT * FROM locations WHERE name LIKE :locationName || '%' ORDER BY name LIMIT :limit")
	fun find(locationName: String, limit: Int = 100): Flow<List<LocationEntity>>

	@Query(
		"SELECT * FROM locations WHERE lat BETWEEN :latB AND :latT AND lng BETWEEN :lngL AND :lngR ORDER BY (lat-:lat)*(lat-:lat)+(lng-:lng)*(lng-:lng) LIMIT :limit")
	fun find(
		lat: Float, lng: Float, latB: Float, latT: Float, lngL: Float, lngR: Float, limit: Int = 100
	): Flow<List<LocationEntity>>
}