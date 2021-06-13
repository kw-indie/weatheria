package asceapps.weatheria.data.api

import asceapps.weatheria.data.base.BaseLocation
import asceapps.weatheria.data.base.Listable
import com.google.gson.annotations.SerializedName

class SearchResponse(
	override val id: Int,
	override val lat: Float,
	@SerializedName("lon") override val lng: Float,
	override val name: String,
	override val country: String
) : BaseLocation, Listable {

	override fun hashCode() = id
}