package asceapps.weatheria.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

@Module
@InstallIn(SingletonComponent::class)
class DispatchersModule {

	@Provides
	@IoDispatcher
	fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class IoDispatcher