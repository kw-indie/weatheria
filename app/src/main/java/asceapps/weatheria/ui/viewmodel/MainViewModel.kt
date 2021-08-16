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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
	@ApplicationContext appContext: Context,
	private val infoRepo: WeatherInfoRepo,
	private val settingsRepo: SettingsRepo
): ViewModel() {

	val weatherInfoList = infoRepo.getAll()
		.shareIn(viewModelScope, SharingStarted.WhileSubscribed(60 * 1000), 1)

	// saving pos is straightforward/easy, saving id is doable but more complex for no gains
	var selectedPos = settingsRepo.selectedPos // only assigns init value

	// sharedFlow does not have .distinctUntilChanged() like stateFlow
	private val manualOnlineCheck = MutableSharedFlow<Result<Unit>>(1)
	val onlineStatus = merge(manualOnlineCheck, appContext.onlineStatusFlow())
		// debounce helps ui complete responding (anim) to last emission
		.debounce(1000)

	override fun onCleared() {
		settingsRepo.selectedPos = selectedPos
		super.onCleared()
	}

	fun checkOnline() = viewModelScope.launch {
		// use tryEmit w/ reply=1 or emit w/ reply=0
		manualOnlineCheck.tryEmit(Loading())
		manualOnlineCheck.tryEmit(asyncPing())
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