package asceapps.weatheria.shared.api

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Flatten(val path: String)