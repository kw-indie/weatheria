package asceapps.weatheria.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun <T> LiveData<T>.debounce(timeoutMillis: Int, scope: CoroutineScope): LiveData<T> =
	MediatorLiveData<T>().also { mld ->
		var job: Job? = null
		mld.addSource(this) {
			job?.cancel()
			job = scope.launch {
				delay(timeoutMillis.toLong())
				mld.value = it
			}
		}
	}

inline fun <T> LiveData<T>.filter(crossinline predicate: (T) -> Boolean): LiveData<T> =
	MediatorLiveData<T>().also { mld ->
		mld.addSource(this) {
			if(predicate(it)) mld.value = it
		}
	}

inline fun <T> LiveData<T>.onEach(crossinline block: (T) -> Unit): LiveData<T> =
	MediatorLiveData<T>().also { mld ->
		mld.addSource(this) {
			block(it)
			mld.value = it
		}
	}