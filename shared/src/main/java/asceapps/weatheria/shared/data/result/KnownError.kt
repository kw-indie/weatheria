package asceapps.weatheria.shared.data.result

/**
 * [message] is always one of `ERROR_*` constants in util package
 */
class KnownError(msg: String): Throwable(msg)