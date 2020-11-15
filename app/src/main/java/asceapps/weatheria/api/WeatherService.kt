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

	@GET("onecall?exclude=minutely")
	suspend fun oneCall(@Query("lat") lat: String, @Query("lon") lng: String): OneCallResponse

	companion object {

		/**
		 * @param context to get cache dir
		 */
		fun create(context: Context): WeatherService {
			val cache = Cache(context.cacheDir, 1 shl 16) // 64 Kb
			val interceptor = Interceptor {chain ->
				val cacheControl = CacheControl.Builder()
					.maxAge(10, TimeUnit.MINUTES)
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
				.baseUrl("https://api.openweathermap.org/data/2.5/")
				.addConverterFactory(GsonConverterFactory.create())
				.client(client)
				.build()
				.create(WeatherService::class.java)
		}

		fun iconUrlFor(icon: String) = "https://openweathermap.org/img/wn/$icon@2x.png"
	}
}