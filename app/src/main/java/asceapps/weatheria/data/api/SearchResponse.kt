package asceapps.weatheria.data.api

import asceapps.weatheria.shared.data.base.BaseLocation
import asceapps.weatheria.shared.data.base.Listable
import com.google.gson.annotations.SerializedName

class SearchResponse(
	@SerializedName("Key") override val id: Int,
	@Flatten("GeoPosition.Latitude") override val lat: Float,
	@Flatten("GeoPosition.Longitude") override val lng: Float,
	@SerializedName("LocalizedName") override val name: String,
	@Flatten("Country.ID") override val country: String,
	// @Flatten("Country.Name") override val country: String, todo use this and rename above to cc
	@Flatten("TimeZone.Name") val zoneId: String
): BaseLocation, Listable {
	override val hash get() = id
}