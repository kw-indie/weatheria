package asceapps.weatheria.api

import android.content.Context
import asceapps.weatheria.BuildConfig
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface WeatherService {

	@GET("$ONE_CALL$EXCLUDE")
	suspend fun oneCall(@Query(LAT) lat: String, @Query(LON) lng: String): OneCallResponse

	@GET("$ONE_CALL$EXCLUDE,$HOURLY,$DAILY")
	suspend fun current(@Query(LAT) lat: String, @Query(LON) lng: String): OneCallResponse

	@GET("$ONE_CALL$EXCLUDE,$CURRENT,$DAILY")
	suspend fun hourly(@Query(LAT) lat: String, @Query(LON) lng: String): OneCallResponse

	@GET("$ONE_CALL$EXCLUDE,$CURRENT,$HOURLY")
	suspend fun daily(@Query(LAT) lat: String, @Query(LON) lng: String): OneCallResponse

	@GET(FIND)
	suspend fun find(@Query(LAT) lat: String, @Query(LON) lng: String): FindResponse

	@GET(FIND)
	suspend fun find(@Query("q") query: String): FindResponse

	companion object {

		private const val BASE_URL = "https://api.openweathermap.org/data/2.5/"
		private const val LAT = "lat"
		private const val LON = "lon"
		// oneCall?lat=xx&lon=yy&exclude=minutely,hourly,daily,alerts
		private const val ONE_CALL = "onecall"
		private const val EXCLUDE = "?exclude=minutely"
		private const val CURRENT = "current"
		private const val HOURLY = "hourly"
		private const val DAILY = "daily"
		// find?q=york&type=like&sort=population&cnt=3
		private const val FIND = "find"
		// 5day/3hr forecast?q=york&cnt=16 (2days)
		private const val FORECAST = "forecast"

		/**
		 * @param context to get cache dir
		 */
		fun create(context: Context): WeatherService {
			val cache = Cache(context.cacheDir, 1 shl 20) // 1 Mb
			val interceptor = Interceptor {chain ->
				val cacheControl = CacheControl.Builder()
					.maxAge(10, TimeUnit.MINUTES)
					.maxStale(1, TimeUnit.HOURS)
					.build()
				val newUrl = chain.request().url.newBuilder()
					.addQueryParameter("appId", BuildConfig.WEATHER_APP_ID)
					.build()
				val request = chain.request().newBuilder()
					.cacheControl(cacheControl)
					.url(newUrl)
					.build()
				chain.proceed(request)
			}
			val client = OkHttpClient.Builder()
				.cache(cache)
				.addInterceptor(interceptor)
				.build()
			return Retrofit.Builder()
				.baseUrl(BASE_URL)
				.addConverterFactory(GsonConverterFactory.create())
				.client(client)
				.build()
				.create(WeatherService::class.java)
		}

		fun iconUrlFor(icon: String) = "https://openweathermap.org/img/wn/$icon@2x.png"
	}
}