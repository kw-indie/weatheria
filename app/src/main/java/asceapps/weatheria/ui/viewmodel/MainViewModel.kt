package asceapps.weatheria.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import asceapps.weatheria.data.model.WeatherInfo
import asceapps.weatheria.data.repo.Loading
import asceapps.weatheria.data.repo.Result
import asceapps.weatheria.data.repo.SettingsRepo
import asceapps.weatheria.data.repo.WeatherInfoRepo
import asceapps.weatheria.util.asyncPing
import asceapps.weatheria.util.onlineStatusFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration

@HiltViewModel
class MainViewModel @Inject constructor(
	@ApplicationContext appContext: Context,
	private val infoRepo: WeatherInfoRepo,
	private val settingsRepo: SettingsRepo
): ViewModel() {

	val weatherInfoList = infoRepo.getAll()
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(Duration.minutes(1)), Loading)
	var selectedLocation = settingsRepo.selectedLocation // only assigns init value

	// sharedFlow does not have .distinctUntilChanged() like stateFlow
	private val manualOnlineCheck = MutableSharedFlow<Result<Unit>>()
	val onlineStatus = merge(manualOnlineCheck, appContext.onlineStatusFlow())
		// debounce helps ui complete responding (anim) to last emission
		.debounce(1000)

	override fun onCleared() {
		settingsRepo.selectedLocation = selectedLocation
		super.onCleared()
	}

	fun checkOnline() = viewModelScope.launch {
		manualOnlineCheck.emit(Loading)
		manualOnlineCheck.emit(asyncPing())
	}

	fun refresh(info: WeatherInfo) = infoRepo.refresh(info)

	fun refreshAll() = infoRepo.refreshAll()

	fun reorder(info: WeatherInfo, toPos: Int) = viewModelScope.launch {
		infoRepo.reorder(info, toPos)
	}

	fun delete(info: WeatherInfo) = viewModelScope.launch {
		infoRepo.delete(info)
	}

	fun deleteAll() = viewModelScope.launch {
		infoRepo.deleteAll()
	}
}