package asceapps.weatheria.ui.fragment

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.animation.ArgbEvaluator
import androidx.core.animation.ObjectAnimator
import androidx.core.animation.TypeEvaluator
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import asceapps.weatheria.R
import asceapps.weatheria.data.repo.Result
import asceapps.weatheria.databinding.FragmentHomeBinding
import asceapps.weatheria.model.WeatherInfo
import asceapps.weatheria.ui.adapter.WeatherInfoAdapter
import asceapps.weatheria.ui.viewmodel.MainViewModel
import asceapps.weatheria.util.observe
import asceapps.weatheria.util.onItemInsertedFlow
import asceapps.weatheria.util.onPageSelectedFlow
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.HttpException
import java.io.IOException
import java.io.InterruptedIOException

@AndroidEntryPoint
class HomeFragment: Fragment() {

	private val mainVM: MainViewModel by activityViewModels()

	private lateinit var init: IntArray
	private lateinit var dawn: IntArray
	private lateinit var day: IntArray
	private lateinit var dusk: IntArray
	private lateinit var night: IntArray
	private lateinit var gradientEvaluator: TypeEvaluator<IntArray>
	private lateinit var animator: ObjectAnimator

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
	}

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		val binding = FragmentHomeBinding.inflate(inflater, container, false)

		setUpBackgroundStuff()
		setUpOnlineStatusStuff(binding.root)

		val infoAdapter = WeatherInfoAdapter()
		// todo we could merge home + locations fragments with help from a
		//  recyclerView + different layout managers/adapters
		val pager = binding.pager.apply {
			adapter = infoAdapter
			offscreenPageLimit = 1
			onPageSelectedFlow(onClose = { currentPos ->
				mainVM.setSelectedLocation(currentPos)
			}).observe(viewLifecycleOwner) { pos ->
				updateColors(infoAdapter.getItem(pos))
			}
			onItemInsertedFlow().observe(viewLifecycleOwner) { pos ->
				// animate to newly added item
				currentItem = pos
			}
		}

		val swipeRefresh = binding.swipeRefresh.apply {
			setOnRefreshListener {
				// fixme no better way? we now have settingsRepo in mainVM
				val location = infoAdapter.getItem(pager.currentItem).location
				mainVM.refresh(location).observe(viewLifecycleOwner) {
					if(it is Result.Loading) {
						isRefreshing = true
					} else {
						isRefreshing = false
						if(it is Result.Error) {
							showMessage(
								when(it) {
									// obvious timeout
									is InterruptedIOException -> R.string.error_timed_out
									// others like UnknownHostException when it can't resolve hostname
									is IOException -> R.string.error_no_internet
									// others like http error codes (400, 404, etc.)
									is HttpException -> R.string.error_server_error
									else -> R.string.error_unknown
								}
							)
						}
					}
				}
			}
		}

		mainVM.weatherInfoList.observe(viewLifecycleOwner) {
			when(it) {
				is Result.Loading -> {
					swipeRefresh.isRefreshing = true
				}
				is Result.Success -> {
					swipeRefresh.isRefreshing = false
					val list = it.data
					infoAdapter.submitList(list)
					pager.setCurrentItem(mainVM.getSelectedLocation(), false)
					val isEmpty = list.isEmpty()
					binding.tvEmptyPager.isVisible = isEmpty
					swipeRefresh.isVisible = !isEmpty
				}
				is Result.Error -> {
					swipeRefresh.isRefreshing = false
					// todo does this ever happen?
				}
			}
		}

		return binding.root
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.home_menu, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when(item.itemId) {
			R.id.locationsFragment -> findNavController().navigate(R.id.action_open_saved_locations)
			R.id.addLocationFragment -> findNavController().navigate(R.id.action_open_add_location)
			else -> return super.onOptionsItemSelected(item)
		}
		return true
	}

	override fun onStop() {
		super.onStop()

		updateColors(null)
	}

	// todo share
	private fun showMessage(resId: Int) {
		Toast.makeText(requireContext(), resId, Toast.LENGTH_LONG).show()
	}

	private fun setUpBackgroundStuff() {
		val activity = requireActivity()
		val window = activity.window
		val bg = ResourcesCompat.getDrawable(resources, R.drawable.bg_sky, activity.theme) as GradientDrawable
		window.findViewById<ViewGroup>(android.R.id.content).apply {
			background = bg
		}

		// todo move to settings?
		// region init colors
		init = intArrayOf(0, 0, 0, 0) // transparent, init colors
		dawn = intArrayOf(
			ContextCompat.getColor(activity, R.color.dawn_1),
			ContextCompat.getColor(activity, R.color.dawn_2),
			ContextCompat.getColor(activity, R.color.dawn_3)
		)
		day = intArrayOf(
			ContextCompat.getColor(activity, R.color.day_1),
			ContextCompat.getColor(activity, R.color.day_2),
			ContextCompat.getColor(activity, R.color.day_3)
		)
		dusk = intArrayOf(
			ContextCompat.getColor(activity, R.color.dusk_1),
			ContextCompat.getColor(activity, R.color.dusk_2),
			ContextCompat.getColor(activity, R.color.dusk_3)
		)
		night = intArrayOf(
			ContextCompat.getColor(activity, R.color.night_1),
			ContextCompat.getColor(activity, R.color.night_2),
			ContextCompat.getColor(activity, R.color.night_3)
		)
		// endregion

		gradientEvaluator = TypeEvaluator<IntArray> { fraction, from, to ->
			val argbEvaluator = ArgbEvaluator.getInstance()
			IntArray(from.size) { i -> argbEvaluator.evaluate(fraction, from[i], to[i]) }
		}
		animator = ObjectAnimator.ofObject(bg, "colors", gradientEvaluator, init).apply {
			duration = 500L
		}
	}

	private fun updateColors(info: WeatherInfo?) {
		if(info == null) {
			animator.apply {
				cancel()
				setObjectValues(init)
				start()
			}
			return
		}
		val daySeconds = 24 * 60 * 60f
		// fraction of now in real today at this location
		val dayFraction = info.secondOfToday / daySeconds
		// fraction of sun rise/set in real OR approx. today
		val riseFraction = info.secondOfSunriseToday / daySeconds
		val setFraction = info.secondOfSunsetToday / daySeconds
		// 'delta' = time to transition between day/night, passing through dawn/dusk
		// user .coerceIn(1 / 24f / 60, abs(setFraction - riseFraction) / 2) to coerce value
		// between 1 min and half day/night
		val deltaFraction = 1 / 24f // transition takes 2 hours, 1 hour before/after dawn/dusk
		// new colors to animate to
		val newColors = when {
			dayFraction < riseFraction - deltaFraction -> night
			dayFraction < riseFraction -> {
				val head = riseFraction - deltaFraction
				val localFraction = (dayFraction - head) / (riseFraction - head)
				gradientEvaluator.evaluate(localFraction, night, dawn)
			}
			dayFraction < riseFraction + deltaFraction -> {
				val localFraction = (dayFraction - riseFraction) / deltaFraction
				gradientEvaluator.evaluate(localFraction, dawn, day)
			}
			dayFraction < setFraction - deltaFraction -> day
			dayFraction < setFraction -> {
				val head = setFraction - deltaFraction
				val localFraction = (dayFraction - head) / (setFraction - head)
				gradientEvaluator.evaluate(localFraction, day, dusk)
			}
			dayFraction < setFraction + deltaFraction -> {
				val localFraction = (dayFraction - setFraction) / deltaFraction
				gradientEvaluator.evaluate(localFraction, dusk, night)
			}
			else -> night
		}
		animator.run {
			cancel()
			setObjectValues(newColors)
			start()
		}
	}

	private fun setUpOnlineStatusStuff(anyView: View) {
		val snackbar = Snackbar.make(anyView, R.string.error_no_internet, Snackbar.LENGTH_INDEFINITE)
			.apply {
				animationMode = Snackbar.ANIMATION_MODE_SLIDE
				setAction(R.string.retry) { mainVM.checkOnline() }
			}
		mainVM.onlineStatus.observe(this) {
			when(it) {
				is Result.Loading -> {
					// todo show loading anim
				}
				is Result.Success -> {
					// todo cancel loading anim
					snackbar.dismiss()
				}
				is Result.Error -> {
					snackbar.show()
				}
			}
		}
	}
}