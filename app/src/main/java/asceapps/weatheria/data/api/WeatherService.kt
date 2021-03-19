package asceapps.weatheria.data.api

import android.content.Context
import asceapps.weatheria.BuildConfig
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface WeatherService {

	@GET("onecall?exclude=minutely")
	suspend fun oneCall(@Query("lat") lat: String, @Query("lon") lng: String): OneCallResponse

	@GET("find")
	suspend fun find(@Query("q") name: String): FindResponse

	@GET("find")
	suspend fun find(@Query("lat") lat: String, @Query("lon") lng: String): FindResponse

	companion object {

		const val ICON_URL_PATTERN = "https://openweathermap.org/img/wn/%s@2x.png"

		/**
		 * @param context to get cache dir
		 */
		fun create(context: Context): WeatherService {
			val cache = Cache(context.cacheDir, 1 shl 16) // 64 Kb
			val cacheControl = CacheControl.Builder()
				.maxAge(10, TimeUnit.MINUTES)
				.build()
			val interceptor = Interceptor { chain ->
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
				.callTimeout(10L, TimeUnit.SECONDS) // throws InterruptedIOException
				.build()
			return Retrofit.Builder()
				.baseUrl("https://api.openweathermap.org/data/2.5/")
				.addConverterFactory(GsonConverterFactory.create())
				.client(client)
				.build()
				.create()
		}
	}
}