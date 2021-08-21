package asceapps.weatheria.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

const val EN = "en"
const val AR = "ar"
const val TR = "tr"

fun buildService(baseUrl: String, apiKeyParam: String? = null, apiKey: String? = null): Retrofit {
	val clientBuilder = OkHttpClient.Builder()
		.callTimeout(10, TimeUnit.SECONDS) // throws InterruptedIOException
	if(apiKeyParam != null) {
		val interceptor = Interceptor { chain ->
			val newUrl = chain.request().url.newBuilder()
				.addQueryParameter(apiKeyParam, apiKey)
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