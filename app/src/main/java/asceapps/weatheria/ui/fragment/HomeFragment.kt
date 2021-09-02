package asceapps.weatheria.ui.fragment

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.animation.ArgbEvaluator
import androidx.core.animation.ObjectAnimator
import androidx.core.animation.TypeEvaluator
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import asceapps.weatheria.R
import asceapps.weatheria.databinding.FragmentHomeBinding
import asceapps.weatheria.ext.getColors
import asceapps.weatheria.ext.observe
import asceapps.weatheria.ext.onItemInserted
import asceapps.weatheria.ext.onPageChanged
import asceapps.weatheria.shared.data.model.WeatherInfo
import asceapps.weatheria.shared.data.repo.Error
import asceapps.weatheria.shared.data.repo.Loading
import asceapps.weatheria.shared.data.repo.Success
import asceapps.weatheria.ui.adapter.PagerAdapter
import asceapps.weatheria.ui.viewmodel.MainViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException
import java.io.InterruptedIOException

@AndroidEntryPoint
class HomeFragment: Fragment() {

	private val mainVM: MainViewModel by activityViewModels()

	private val init = intArrayOf(0, 0, 0)
	private lateinit var dawn: IntArray
	private lateinit var day: IntArray
	private lateinit var dusk: IntArray
	private lateinit var night: IntArray
	private lateinit var gradientEvaluator: TypeEvaluator<IntArray>
	private lateinit var animator: ObjectAnimator
	private var currentSky = init

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		// turns out this can be called from here as well
		setHasOptionsMenu(true)

		val binding = FragmentHomeBinding.inflate(inflater, container, false)

		setUpBackgroundStuff()
		setUpOnlineStatusStuff(binding.root)

		val pagerAdapter = PagerAdapter()
		val pager = binding.pager.apply {
			adapter = pagerAdapter.apply {
				onItemInserted { setCurrentItem(it, true) }
			}
			onPageChanged { pos ->
				mainVM.selectedPos = pos
				updateColors(pagerAdapter.getItem(pos))
			}
		}

		val swipeRefresh = binding.swipeRefresh.apply {
			setOnRefreshListener {
				val info = pagerAdapter.getItem(pager.currentItem)
				mainVM.refresh(info).observe(viewLifecycleOwner) {
					if(it is Loading) {
						isRefreshing = true
					} else {
						isRefreshing = false
						if(it is Error) {
							val msg = when(it.t) {
								// obvious timeout
								is InterruptedIOException -> R.string.error_timed_out
								// others like UnknownHostException when it can't resolve hostname
								is IOException -> R.string.error_no_internet
								// others like http error codes (400, 404, etc.).
								// fixme originally HttpException from retrofit
								is RuntimeException -> R.string.error_server_error
								else -> R.string.error_unknown
							}
							showMessage(msg)
						}
					}
				}
			}
		}

		mainVM.weatherInfoList.observe(viewLifecycleOwner) {
			when(it) {
				is Loading -> {
					swipeRefresh.isRefreshing = true
				}
				is Success -> {
					swipeRefresh.isRefreshing = false
					val list = it.data
					pagerAdapter.submitList(list)
					val isEmpty = list.isEmpty()
					binding.tvEmptyPager.isVisible = isEmpty
					swipeRefresh.isVisible = !isEmpty
					if(!isEmpty) {
						val selected = mainVM.selectedPos.coerceIn(list.indices)
						// updateColor here fixes issue when fragment is stopped/started,
						// but onPageChanged not triggered
						updateColors(list[selected])
						pager.setCurrentItem(selected, false)
					}
				}
				is Error -> {
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
			R.id.myLocationsFragment -> findNavController().navigate(R.id.myLocationsFragment)
			R.id.addLocationFragment -> findNavController().navigate(R.id.addLocationFragment)
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
		dawn = activity.getColors(R.color.dawn_1, R.color.dawn_2, R.color.dawn_3)
		day = activity.getColors(R.color.day_1, R.color.day_2, R.color.day_3)
		dusk = activity.getColors(R.color.dusk_1, R.color.dusk_2, R.color.dusk_3)
		night = activity.getColors(R.color.night_1, R.color.night_2, R.color.night_3)

		gradientEvaluator = TypeEvaluator<IntArray> { fraction, from, to ->
			val argbEvaluator = ArgbEvaluator.getInstance()
			IntArray(from.size) { i -> argbEvaluator.evaluate(fraction, from[i], to[i]) }
		}
		animator = ObjectAnimator.ofObject(bg, "colors", gradientEvaluator, init).apply {
			duration = 500L
			setAutoCancel(true)
		}
	}

	private fun updateColors(info: WeatherInfo?) {
		val newSky = if(info == null) init
		else {
			val (partOfDay, fraction) = info.partOfDay
			// new colors to animate to
			when(partOfDay) {
				1 -> gradientEvaluator.evaluate(fraction, night, dawn)
				2 -> gradientEvaluator.evaluate(fraction, dawn, day)
				3 -> day
				4 -> gradientEvaluator.evaluate(fraction, day, dusk)
				5 -> gradientEvaluator.evaluate(fraction, dusk, night)
				else -> night
			}
		}
		// don't animate if same colors
		if(currentSky !== newSky) {
			currentSky = newSky
			animator.run {
				setObjectValues(newSky)
				start()
			}
		}
	}

	private fun setUpOnlineStatusStuff(anyView: View) {
		val snackbar = Snackbar.make(anyView, R.string.error_no_internet, Snackbar.LENGTH_INDEFINITE).apply {
			animationMode = Snackbar.ANIMATION_MODE_SLIDE
			setAction(R.string.retry) { mainVM.checkOnline() }
		}
		mainVM.onlineStatus.observe(viewLifecycleOwner) {
			when(it) {
				is Loading -> {
					// todo show loading anim
				}
				is Success -> {
					// todo cancel loading anim
					snackbar.dismiss()
				}
				is Error -> {
					snackbar.show()
				}
			}
		}
	}
}