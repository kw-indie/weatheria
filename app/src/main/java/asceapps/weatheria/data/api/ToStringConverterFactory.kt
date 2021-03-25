package asceapps.weatheria.data.api

import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

class ToStringConverterFactory : Converter.Factory() {
	companion object {

		private val INSTANCE = Converter<ResponseBody, String> { it.string() }
	}

	override fun responseBodyConverter(
		type: Type,
		annotations: Array<out Annotation>,
		retrofit: Retrofit
	) = INSTANCE
}