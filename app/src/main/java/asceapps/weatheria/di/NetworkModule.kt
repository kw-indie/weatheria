package asceapps.weatheria.di

import asceapps.weatheria.BuildConfig
import asceapps.weatheria.data.api.FlattenTypeAdapterFactory
import asceapps.weatheria.data.api.WeatherApi
import com.google.gson.GsonBuilder
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

	/**
	 * @param params key-value pairs of url params.
	 */
	private fun buildApi(baseUrl: String, vararg params: String): Retrofit {
		val clientBuilder = OkHttpClient.Builder()
			.callTimeout(10, TimeUnit.SECONDS) // throws InterruptedIOException
		if(params.isNotEmpty()) {
			val interceptor = Interceptor { chain ->
				val newUrl = chain.request().url.newBuilder().apply {
					for(i in params.indices step 2) {
						addQueryParameter(params[i], params[i + 1])
					}
				}.build()
				val request = chain.request().newBuilder()
					.url(newUrl)
					.build()
				chain.proceed(request)
			}
			clientBuilder.addInterceptor(interceptor)
		}
		val gson = GsonBuilder()
			.registerTypeAdapterFactory(FlattenTypeAdapterFactory())
			.create()
		return Retrofit.Builder()
			.baseUrl(baseUrl)
			.client(clientBuilder.build())
			// for now, all my api's are dealing in json
			.addConverterFactory(GsonConverterFactory.create(gson))
			.build()
	}
}