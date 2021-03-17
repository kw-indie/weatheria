package asceapps.weatheria.ui.viewmodel

import android.content.Context
import android.text.format.DateUtils
import androidx.lifecycle.*
import asceapps.weatheria.data.entity.LocationEntity
import asceapps.weatheria.data.repo.WeatherInfoRepo
import asceapps.weatheria.model.Location
import asceapps.weatheria.util.asyncPing
import asceapps.weatheria.util.debounce
import asceapps.weatheria.util.onlineStatusFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ActivityContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
	@ActivityContext context: Context,
	private val infoRepo: WeatherInfoRepo
) : ViewModel() {

	val weatherInfoList = infoRepo.loadAll()
		.shareIn(viewModelScope, SharingStarted.Lazily, 1)
		.onEach { _loading.value = false }

	// todo add selectedLocation here and init it from settingsRepo

	private val _loading = MutableLiveData(true)
	val loading: LiveData<Boolean> get() = _loading

	private val _error = MutableLiveData<Throwable>()
	val error: LiveData<Throwable> get() = _error

	// fixme use StateFlow<Result>() e.g. Result.loading()
	// used liveData for now cuz flows don't re-emit same value
	private val onlineManualCheck = MutableLiveData<Boolean?>()
	val onlineStatus: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
		addSource(onlineManualCheck) { value = it }
		addSource(context.onlineStatusFlow().asLiveData()) { value = it }
	}.debounce(500, viewModelScope)
	// this debounce helps when ui hasn't completed responding to last emission

	fun checkOnline() = viewModelScope.launch {
		onlineManualCheck.value = asyncPing()
	}

	fun addNewLocation(l: LocationEntity) = viewModelScope.launch {
		try {
			_loading.value = true
			infoRepo.get(l)
		} catch (e: Exception) {
			e.printStackTrace()
			_error.value = e
		} finally {
			_loading.value = false
		}
	}

	fun refresh(l: Location) = viewModelScope.launch {
		try {
			_loading.value = true
			with(l) {
				infoRepo.refresh(id, lat, lng)
			}
		} catch (e: Exception) {
			e.printStackTrace()
			_error.value = e
		} finally {
			_loading.value = false
		}
	}

	fun reorder(l: Location, toPos: Int) = viewModelScope.launch {
		infoRepo.reorder(l.id, l.pos, toPos)
	}

	fun delete(l: Location) = viewModelScope.launch {
		infoRepo.delete(l.id, l.pos)
	}

	fun deleteAll() = viewModelScope.launch {
		infoRepo.deleteAll()
	}

	companion object {

		// region formatting
		// prints at least 1 digit, sep each 3 digits, 0 to 2 decimal digits, rounds to nearest
		private val nFormat = NumberFormat.getInstance().apply {
			minimumFractionDigits = 0
			maximumFractionDigits = 2
		}

		// adds localized percent char
		private val pFormat = NumberFormat.getPercentInstance()
		private val dtFormatter = DateTimeFormatter.ofPattern("EEE, d MMMM, h:mm a (xxx)")
		private val tFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

		// use Locale.Builder().setLanguageTag("ar-u-nu-arab").build()
		// for testing arabic numbering locale
		fun relativeTime(millis: Int): CharSequence =
			DateUtils.getRelativeTimeSpanString(millis.toLong())

		fun nowDateTime(offset: ZoneOffset): String = dtFormatter.format(OffsetDateTime.now(offset))
		fun time(instant: Instant, offset: ZoneOffset): String =
			tFormatter.format(instant.atOffset(offset))

		fun temp(deg: Int, metric: Boolean) =
			nFormat.format((if (metric) deg - 273.15f else deg * 1.8f - 459.67f).toInt()) + '°'

		fun minMax(min: Int, max: Int, metric: Boolean) =
			temp(min, metric).padEnd(5) + '|' + temp(max, metric).padStart(5)

		fun speed(mps: Float, metric: Boolean, speedUnit: String) =
			nFormat.format(if (metric) mps else mps * 2.237f) + ' ' + speedUnit

		// our ratios are already from 0-100, this formatter expects fractions from 0-1
		fun percent(ratio: Int): String = pFormat.format(ratio / 100f)
		// endregion
	}
}