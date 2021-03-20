package asceapps.weatheria.di

import android.content.Context
import asceapps.weatheria.BuildConfig
import asceapps.weatheria.data.api.GeoIPService
import asceapps.weatheria.data.api.WeatherService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {

	@Provides
	@Singleton
	fun provideWeatherService(@ApplicationContext appContext: Context): WeatherService {
		val cache = Cache(appContext.cacheDir, 1 shl 18) // 256 Kb
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
			.client(client)
			.addConverterFactory(GsonConverterFactory.create())
			.build()
			.create()
	}
}