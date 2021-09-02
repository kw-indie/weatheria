package asceapps.weatheria.shared.api

import com.google.gson.annotations.SerializedName

internal class SearchResponse(
	@SerializedName("Key") val id: String,
	@Flatten("GeoPosition.Latitude") val lat: Float,
	@Flatten("GeoPosition.Longitude") val lng: Float,
	@SerializedName("LocalizedName") val name: String,
	@Flatten("Country.ID") val country: String,
	// @Flatten("Country.Name") override val country: String, todo use this and rename above to cc
	@Flatten("TimeZone.Name") val zoneId: String
)