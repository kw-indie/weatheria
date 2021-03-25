package asceapps.weatheria.data.api

import retrofit2.http.GET

interface IPApi {

	// this number is short for: query (ip), countryCode, city, lat, lon, offset
	//@GET("?fields=33562834")
	//suspend fun lookup(): GeoIPResponse
	// returns xx.xxx..,yy.yyy... coords
	@GET("latlong")
	suspend fun lookup(): String
}