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
	@Singleton
	fun provideWeatherApi(): WeatherApi = buildApi(
		WeatherApi.BASE_URL,
		WeatherApi.KEY_PARAM, BuildConfig.WEATHER_API_KEY
	).create()

	private fun buildApi(
		baseUrl: String,
		paramName: String? = null, paramValue: String? = null
	): Retrofit {
		val clientBuilder = OkHttpClient.Builder()
			.callTimeout(10, TimeUnit.SECONDS) // throws InterruptedIOException
		if(paramName != null) {
			val interceptor = Interceptor { chain ->
				val newUrl = chain.request().url.newBuilder()
					.addQueryParameter(paramName, paramValue)
					.build()
				val request = chain.request().newBuilder()
					.url(newUrl)
					.build()
				chain.proceed(request)
			}
			clientBuilder.addInterceptor(interceptor)
		}
		return Retrofit.Builder()
			.baseUrl(baseUrl)
			.client(clientBuilder.build())
			// for now, all my api's are dealing in json
			.addConverterFactory(GsonConverterFactory.create())
			.build()
	}
}