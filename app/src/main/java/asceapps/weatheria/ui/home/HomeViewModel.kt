package asceapps.weatheria.ui.home

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import asceapps.weatheria.model.WeatherInfoRepo
import asceapps.weatheria.util.isCoordinate
import kotlinx.coroutines.launch

class HomeViewModel(
	private val repo: WeatherInfoRepo,
	private val savedStateHandle: SavedStateHandle
): ViewModel() {

	val infoList = repo.loadAllInfo()
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
			val info = if(isCoordinate(query)) {
				val coords = query.split(',').map {it.trim()}
				repo.fetch(coords[0], coords[1])
			} else {
				repo.fetch(query.trim())
			}
			repo.save(info)
		} catch(e: Exception) {
			e.printStackTrace()
			_error.value = e
		} finally {
			_refreshing.value = false
		}
	}

	fun update(locationId: Int) = viewModelScope.launch {
		_refreshing.value = true
		try {
			val updated = repo.fetchUpdate(locationId)
			repo.update(updated)
		} catch(e: Exception) {
			e.printStackTrace()
			_error.value = e
		} finally {
			_refreshing.value = false
		}
	}

	fun delete(locationId: Int) = viewModelScope.launch {
		repo.delete(locationId)
	}

	fun deleteAll() = viewModelScope.launch {
		repo.deleteAll()
	}

	companion object {

		private const val SELECTED_PAGE_KEY = "SP"
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