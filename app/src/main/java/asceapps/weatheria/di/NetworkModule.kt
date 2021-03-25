package asceapps.weatheria.di

import asceapps.weatheria.BuildConfig
import asceapps.weatheria.data.api.IPApi
import asceapps.weatheria.data.api.ToStringConverterFactory
import asceapps.weatheria.data.api.WeatherApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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

	companion object {

		private const val CALL_TIMEOUT = 10L
	}

	@Provides
	@Singleton
	fun provideIPApi(): IPApi {
		// this nice api is free and does not even require an API key
		val client = OkHttpClient.Builder()
			.callTimeout(CALL_TIMEOUT, TimeUnit.SECONDS)
			.build()
		return Retrofit.Builder()
			//.baseUrl("http://ip-api.com/json") // no free https
			.baseUrl("https://ipapi.co/")
			.client(client)
			.addConverterFactory(ToStringConverterFactory())
			.build()
			.create()
	}

	@Provides
	@Singleton
	fun provideWeatherApi(): WeatherApi {
		val interceptor = Interceptor { chain ->
			val newUrl = chain.request().url.newBuilder()
				.addQueryParameter("appId", BuildConfig.WEATHER_APP_ID)
				.build()
			val request = chain.request().newBuilder()
				.url(newUrl)
				.build()
			chain.proceed(request)
		}
		val client = OkHttpClient.Builder()
			.addInterceptor(interceptor)
			.callTimeout(CALL_TIMEOUT, TimeUnit.SECONDS) // throws InterruptedIOException
			.build()
		return Retrofit.Builder()
			.baseUrl("https://api.openweathermap.org/data/2.5/")
			.client(client)
			.addConverterFactory(GsonConverterFactory.create())
			.build()
			.create()
	}
}