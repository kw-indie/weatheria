package asceapps.weatheria.shared.api

import retrofit2.http.GET

internal interface IPWhoisApi {

	companion object {
		// chose this cuz: SSL, IP, location info and free 10k req/month
		const val BASE_URL = "https://ipwhois.app"
	}

	@GET("json/?objects=ip,latitude,longitude,city,country_code")
	suspend fun whois(): WhoisResponse

}