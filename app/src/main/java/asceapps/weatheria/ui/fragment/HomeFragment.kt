package asceapps.weatheria.ui.fragment

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.*
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
import asceapps.weatheria.data.repo.SettingsRepo
import asceapps.weatheria.databinding.FragmentHomeBinding
import asceapps.weatheria.model.WeatherInfo
import asceapps.weatheria.ui.adapter.WeatherInfoAdapter
import asceapps.weatheria.ui.viewmodel.MainViewModel
import asceapps.weatheria.util.observe
import asceapps.weatheria.util.onItemInsertedFlow
import asceapps.weatheria.util.onPageSelectedFlow
import asceapps.weatheria.util.setMetric
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment: Fragment() {

	private val mainVM: MainViewModel by activityViewModels()
	@Inject
	lateinit var settingsRepo: SettingsRepo

	private lateinit var contentView: ViewGroup
	private lateinit var bg: GradientDrawable

	private var selectedLocation = 0

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)

		val activity = requireActivity()
		val window = activity.window
		contentView = window.findViewById(android.R.id.content)
		bg = ResourcesCompat.getDrawable(resources, R.drawable.bg_sky, activity.theme) as GradientDrawable
	}

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		// on reinstall or sth, make sure all settings are reapplied
		// note: irl, this happens when app settings are auto backed up
		// settingsRepo.reapply()

		// setup prefs
		// todo clean up
		// need to re-read this every time we are back to the fragment in case it changes
		setMetric(settingsRepo.isMetric, settingsRepo.speedUnit)

		return FragmentHomeBinding.inflate(inflater, container, false).apply {
			val infoAdapter = WeatherInfoAdapter().apply {
				onItemInsertedFlow().observe(viewLifecycleOwner) { pos ->
					// animate to newly added item
					pager.currentItem = pos
				}
			}

			val (init, dawn, day, dusk, night) = getDayPartsColors(requireContext())
			val evaluator = getGradientEvaluator()
			val animator = ObjectAnimator.ofObject(bg, "colors", evaluator, init).apply {
				duration = 1000L
			}
			// todo we could merge home + savedLocations fragments with help from a
			//  recyclerView + different layout managers/adapters
			pager.apply {
				adapter = infoAdapter
				offscreenPageLimit = 1
				onPageSelectedFlow().observe(viewLifecycleOwner) { pos ->
					selectedLocation = pos
					updateColors(
						infoAdapter.getItem(pos),
						dawn,
						day,
						dusk,
						night,
						evaluator,
						animator
					)
				}
			}

			swipeRefresh.apply {
				setOnRefreshListener {
					mainVM.refresh(infoAdapter.getItem(pager.currentItem).location)
				}
			}

			selectedLocation = settingsRepo.selectedLocation
			mainVM.weatherInfoList.observe(viewLifecycleOwner) {
				infoAdapter.submitList(it)
				pager.setCurrentItem(selectedLocation, false)
				val isEmpty = it.isEmpty()
				tvEmptyPager.isVisible = isEmpty
				swipeRefresh.isVisible = !isEmpty
			}
			mainVM.loading.observe(viewLifecycleOwner) {
				swipeRefresh.isRefreshing = it
			}
		}.root
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.home_menu, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when(item.itemId) {
			R.id.action_locations -> findNavController().navigate(R.id.action_open_saved_locations)
			R.id.action_add_location -> findNavController().navigate(R.id.action_open_add_location)
			else -> return super.onOptionsItemSelected(item)
		}
		return true
	}

	override fun onPause() {
		super.onPause()

		settingsRepo.selectedLocation = selectedLocation
	}

	override fun onStart() {
		super.onStart()
		contentView.background = bg
	}

	override fun onStop() {
		super.onStop()

		// todo animate out?
		contentView.background = null
	}

	// todo move to settings?
	private fun getDayPartsColors(context: Context) = arrayOf(
		intArrayOf(0, 0, 0, 0), // transparent, init colors
		intArrayOf(
			ContextCompat.getColor(context, R.color.dawn_1),
			ContextCompat.getColor(context, R.color.dawn_2),
			ContextCompat.getColor(context, R.color.dawn_3)
		),
		intArrayOf(
			ContextCompat.getColor(context, R.color.day_1),
			ContextCompat.getColor(context, R.color.day_2),
			ContextCompat.getColor(context, R.color.day_3)
		),
		intArrayOf(
			ContextCompat.getColor(context, R.color.dusk_1),
			ContextCompat.getColor(context, R.color.dusk_2),
			ContextCompat.getColor(context, R.color.dusk_3)
		),
		intArrayOf(
			ContextCompat.getColor(context, R.color.night_1),
			ContextCompat.getColor(context, R.color.night_2),
			ContextCompat.getColor(context, R.color.night_3)
		)
	)

	private fun getGradientEvaluator() = TypeEvaluator<IntArray> {fraction, from, to ->
		val argbEvaluator = ArgbEvaluator.getInstance()
		val newColors = IntArray(from.size)
		for(i in newColors.indices) {
			newColors[i] = argbEvaluator.evaluate(fraction, from[i], to[i])
		}
		newColors
	}

	private fun updateColors(
		info: WeatherInfo,
		dawn: IntArray, day: IntArray, dusk: IntArray, night: IntArray,
		evaluator: TypeEvaluator<IntArray>, animator: ObjectAnimator
	) {
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
				evaluator.evaluate(localFraction, night, dawn)
			}
			dayFraction < riseFraction + deltaFraction -> {
				val localFraction = (dayFraction - riseFraction) / deltaFraction
				evaluator.evaluate(localFraction, dawn, day)
			}
			dayFraction < setFraction - deltaFraction -> day
			dayFraction < setFraction -> {
				val head = setFraction - deltaFraction
				val localFraction = (dayFraction - head) / (setFraction - head)
				evaluator.evaluate(localFraction, day, dusk)
			}
			dayFraction < setFraction + deltaFraction -> {
				val localFraction = (dayFraction - setFraction) / deltaFraction
				evaluator.evaluate(localFraction, dusk, night)
			}
			else -> night
		}
		animator.run {
			cancel()
			setObjectValues(newColors)
			start()
		}
	}
}