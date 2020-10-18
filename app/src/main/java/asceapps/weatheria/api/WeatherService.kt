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

	@GET(PATH_FIND)
	suspend fun find(@Query("lat") lat: String, @Query("lon") lng: String): FindResponse

	@GET("$PATH_FIND?sort=population")
	suspend fun find(@Query("q") query: String): FindResponse

	@GET(PATH_CURRENT)
	suspend fun current(@Query("lat") lat: String, @Query("lon") lng: String): CurrentResponse

	@GET(PATH_CURRENT)
	suspend fun current(@Query("q") query: String): CurrentResponse

	@GET(PATH_CURRENT)
	suspend fun current(@Query("id") locationId: Int): CurrentResponse

	companion object {

		private const val BASE_URL: String = "https://api.openweathermap.org/data/2.5/"
		private const val PATH_FIND: String = "find" // find?q=york&type=like&sort=population&cnt=3&appid=xxx
		private const val PATH_CURRENT: String = "weather" // weather?q=york&appid=xxx
		private const val PATH_FORECAST: String = "forecast" // 5day/3hr forecast?q=york&appid=xxx
		private const val PARAM_APP_ID: String = "appid"

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
					.addQueryParameter(PARAM_APP_ID, BuildConfig.WEATHER_APP_ID)
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
	}
}