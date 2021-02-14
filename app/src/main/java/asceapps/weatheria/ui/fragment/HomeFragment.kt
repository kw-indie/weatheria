package asceapps.weatheria.ui.fragment

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.animation.ArgbEvaluator
import androidx.core.animation.ObjectAnimator
import androidx.core.animation.TypeEvaluator
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import asceapps.weatheria.R
import asceapps.weatheria.data.repo.SettingsRepo
import asceapps.weatheria.databinding.FragmentHomeBinding
import asceapps.weatheria.model.WeatherInfo
import asceapps.weatheria.ui.adapter.WeatherInfoAdapter
import asceapps.weatheria.ui.viewmodel.MainViewModel
import asceapps.weatheria.util.setMetric
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment: Fragment() {

	private val mainVM: MainViewModel by activityViewModels()
	@Inject
	lateinit var settingsRepo: SettingsRepo
	lateinit var binding: FragmentHomeBinding

	private lateinit var contentView: ViewGroup
	private lateinit var bg: GradientDrawable

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
	}

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		// setup prefs
		// todo clean up
		// need to re-read this every time we are back to the fragment in case it changes
		val units = settingsRepo.units
		val speedUnit = resources.getStringArray(R.array.units_speed)[units]
		setMetric(units == 0, speedUnit)

		binding = FragmentHomeBinding.inflate(inflater, container, false).apply {
			val adapter = WeatherInfoAdapter()
			val adapterDataObserver = object: RecyclerView.AdapterDataObserver() {
				override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
					if(itemCount == 1) {
						// animate to newly added item
						pager.post {
							pager.currentItem = positionStart
						}
					} else {
						// fragment reload or sth
						pager.post {
							pager.setCurrentItem(settingsRepo.selectedLocation, false)
						}
					}
				}
			}
			adapter.registerAdapterDataObserver(adapterDataObserver)

			val activity = requireActivity()
			val window = activity.window
			contentView = window.findViewById(android.R.id.content)
			bg = ResourcesCompat.getDrawable(resources, R.drawable.bg_sky, activity.theme) as GradientDrawable

			val (init, dawn, day, dusk, night) = getDayPartsColors(requireContext())
			val evaluator = getGradientEvaluator()
			val animator = ObjectAnimator.ofObject(bg, "colors", evaluator, init).apply {
				duration = 1000L
			}
			val onPageChangeCallback = object: OnPageChangeCallback() {
				override fun onPageSelected(pos: Int) {
					// todo check no npe is throw when last item deleted
					updateColors(adapter.getItem(pos), dawn, day, dusk, night, evaluator, animator)
				}
			}
			pager.apply {
				this.adapter = adapter
				registerOnPageChangeCallback(onPageChangeCallback)
				offscreenPageLimit = 2
			}

			swipeRefresh.setOnRefreshListener {
				mainVM.refresh(adapter.getItem(pager.currentItem).location)
			}

			mainVM.weatherInfoList.observe(viewLifecycleOwner) {
				adapter.submitList(it)
				if(it.isEmpty()) {
					tvEmptyPager.visibility = View.VISIBLE
					swipeRefresh.visibility = View.GONE // to prevent swipe
				} else {
					tvEmptyPager.visibility = View.GONE
					swipeRefresh.visibility = View.VISIBLE
				}
			}
			mainVM.loading.observe(viewLifecycleOwner, swipeRefresh::setRefreshing)
		}
		return binding.root
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.home_menu, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when(item.itemId) {
			R.id.action_locations -> findNavController().navigate(R.id.action_open_saved_locations)
			R.id.action_search -> findNavController().navigate(R.id.action_open_search)
			else -> return super.onOptionsItemSelected(item)
		}
		return true
	}

	override fun onResume() {
		super.onResume()
		contentView.background = bg
	}

	override fun onPause() {
		super.onPause()
		contentView.background = null
		settingsRepo.selectedLocation = binding.pager.currentItem
	}

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