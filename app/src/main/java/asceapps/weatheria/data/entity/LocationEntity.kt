package asceapps.weatheria.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import asceapps.weatheria.data.IDed

@Entity(tableName = "locations")
class LocationEntity(
	@PrimaryKey override val id: Int,
	val lat: Float,
	val lng: Float,
	@ColumnInfo(collate = ColumnInfo.UNICODE) val name: String,
	val country: String
) : IDed {

	// for HashItemCallback
	override fun hashCode() = id
}