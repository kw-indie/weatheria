package asceapps.weatheria.shared.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class IoDispatcher
// had to move here, di in shared wouldn't work otherwise