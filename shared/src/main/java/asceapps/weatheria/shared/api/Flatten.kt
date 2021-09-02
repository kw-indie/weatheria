package asceapps.weatheria.shared.api

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
internal annotation class Flatten(val path: String)