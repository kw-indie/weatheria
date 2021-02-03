package asceapps.weatheria.ui.viewmodel

import android.annotation.SuppressLint
import androidx.lifecycle.*
import asceapps.weatheria.data.repo.LocationRepo
import asceapps.weatheria.util.debounce
import asceapps.weatheria.util.getFreshLocation
import com.google.android.gms.location.FusedLocationProviderClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
	private val locationRepo: LocationRepo
): ViewModel() {

	// todo save query in saveStateHandler and build 'result' reacting to that live data
	// see https://github.com/android/architecture-components-samples/blob/master/BasicSample/app/src/main/java/com/example/android/persistence/viewmodel/ProductListViewModel.java
	val query = MutableLiveData<String>() // todo convert to stateFlow in AS 4.3
	val result = query
		.debounce(500, viewModelScope)
		.map {it.trim()}
		.distinctUntilChanged()
		.switchMap {q -> locationRepo.search(q)}
	/*.asFlow()
	.debounce(500)
	.filterNot {it.isNullOrBlank()}
	.distinctUntilChanged()
	.flatMapLatest {q -> locationRepo.search(q)}*/

	private val _error = MutableLiveData<Throwable>()
	val error: LiveData<Throwable> get() = _error

	fun getMyLocation(client: FusedLocationProviderClient) {
		viewModelScope.launch {
			try {
				@SuppressLint("MissingPermission")
				val location = client.getFreshLocation()
				query.value = with(location) {"$latitude,$longitude"}
			} catch(e: Exception) {
				e.printStackTrace()
				_error.value = e
			}
		}
	}
}