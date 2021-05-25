package asceapps.weatheria.di

import asceapps.weatheria.BuildConfig
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

	@Provides
	@Singleton // fixme prolly not needed
	fun provideWeatherApi(): WeatherApi {
		val interceptor = Interceptor { chain ->
			val newUrl = chain.request().url.newBuilder()
				.addQueryParameter("key", BuildConfig.WEATHER_API_KEY)
				.build()
			val request = chain.request().newBuilder()
				.url(newUrl)
				.build()
			chain.proceed(request)
		}
		val client = OkHttpClient.Builder()
			.addInterceptor(interceptor)
			.callTimeout(10, TimeUnit.SECONDS) // throws InterruptedIOException
			.build()
		return Retrofit.Builder()
			.baseUrl("https://api.weatherapi.com/v1/")
			.client(client)
			.addConverterFactory(GsonConverterFactory.create())
			.build()
			.create()
	}
}