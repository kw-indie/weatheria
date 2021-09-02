package asceapps.weatheria.shared.api

import com.google.gson.annotations.SerializedName

internal class WhoisResponse(
	@SerializedName("ip") val ip: String,
	@SerializedName("latitude") val lat: Float,
	@SerializedName("longitude") val lng: Float,
	@SerializedName("city") val city: String,
	@SerializedName("country_code") val country: String
)