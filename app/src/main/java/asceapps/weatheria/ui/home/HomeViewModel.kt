package asceapps.weatheria.ui.home

import android.os.Bundle
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistryOwner
import asceapps.weatheria.model.Location
import asceapps.weatheria.model.WeatherInfoRepo
import kotlinx.coroutines.launch

class HomeViewModel(
	private val repo: WeatherInfoRepo,
	private val savedStateHandle: SavedStateHandle
): ViewModel() {

	val infoList = repo.getAll().asLiveData()
	private val _error = MutableLiveData<Throwable>()
	val error: LiveData<Throwable> get() = _error
	private val _refreshing = MutableLiveData<Boolean>()
	val refreshing: LiveData<Boolean> get() = _refreshing
	var selected: Int?
		get() = savedStateHandle[SELECTED_PAGE_KEY]
		set(value) = savedStateHandle.set(SELECTED_PAGE_KEY, value)

	/**
	 * If successful, the result is auto inserted in the database.
	 * @param query user input. a location name or comma separated coordinates
	 */
	fun addNewLocation(query: String) = viewModelScope.launch {
		_refreshing.value = true
		try {
			if(isCoordinate(query)) {
				// this 7 is for cases like -xx.yyy, we want max 6 but 7th for accuracy
				// need to clamp because gps gives way too many decimals
				val coords = query.split(',').map {it.take(7).trim()}
				repo.get(coords[0], coords[1])
			} else {
				repo.get(query.trim())
			}
		} catch(e: Exception) {
			e.printStackTrace()
			_error.value = e
		} finally {
			_refreshing.value = false
		}
	}

	fun update(l: Location) = viewModelScope.launch {
		_refreshing.value = true
		try {
			repo.getUpdate(l)
		} catch(e: Exception) {
			e.printStackTrace()
			_error.value = e
		} finally {
			_refreshing.value = false
		}
	}

	fun delete(l: Location) = viewModelScope.launch {
		repo.delete(l)
	}

	fun deleteAll() = viewModelScope.launch {
		repo.deleteAll()
	}

	companion object {

		private const val SELECTED_PAGE_KEY = "SP"

		private fun isCoordinate(str: String) = str.matches(Regex(
			"^[-+]?([1-8]?\\d(\\.\\d+)?|90(\\.0+)?)\\s*,\\s*[-+]?(180(\\.0+)?|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d+)?)\$"
		))
	}

	class Factory(
		private val weatherInfoRepo: WeatherInfoRepo,
		owner: SavedStateRegistryOwner,
		defaultArgs: Bundle? = null
	): AbstractSavedStateViewModelFactory(owner, defaultArgs) {

		@Suppress("unchecked_cast")
		override fun <T: ViewModel?> create(
			key: String,
			modelClass: Class<T>,
			handle: SavedStateHandle
		) = HomeViewModel(weatherInfoRepo, handle) as T
	}
}