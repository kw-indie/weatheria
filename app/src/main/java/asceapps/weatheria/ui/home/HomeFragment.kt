package asceapps.weatheria.ui.home

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.SeekableAnimatedVectorDrawable
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import asceapps.weatheria.R
import asceapps.weatheria.databinding.FragmentHomeBinding
import asceapps.weatheria.model.WeatherInfoRepo
import asceapps.weatheria.model.setMetric
import asceapps.weatheria.ui.MainActivity

class HomeFragment: Fragment() {

	private val viewModel: HomeViewModel by viewModels {
		HomeViewModel.Factory(WeatherInfoRepo.getInstance(requireContext()))
	}

	override fun onCreateView(
		inflater: LayoutInflater, root: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		// setup prefs
		val prefs = (requireActivity() as MainActivity).prefs
		val unitsKey = getString(R.string.key_units)
		val units = prefs.getString(unitsKey, "0")
		val isMetric = units == "0"
		val speedUnit = if(isMetric) getString(R.string.metric_speed) else getString(R.string.imp_speed)
		setMetric(isMetric, speedUnit)

		val binding = FragmentHomeBinding.inflate(inflater, root, false).apply {
			// setup bg
			val animCallback = object: SeekableAnimatedVectorDrawable.AnimationCallback() {
				val midPoint = 1000L
				var toDay: Boolean = false
				override fun onAnimationUpdate(drawable: SeekableAnimatedVectorDrawable) {
					with(drawable) {
						if(currentPlayTime > midPoint && !toDay) { // if beyond night by few  millis due to fps
							pause()
							currentPlayTime = midPoint
							toDay = true
						}
					}
				}

				override fun onAnimationEnd(drawable: SeekableAnimatedVectorDrawable) {
					toDay = false
				}
			}
			val bg = SeekableAnimatedVectorDrawable.create(requireContext(), R.drawable.bg_day_night)!!
				.apply {
					start();stop() // fixes a bug where everything is fine but bg is black
					registerAnimationCallback(animCallback)
				}
			clRoot.background = bg

			// setup viewPager
			// todo read selected item from prefs, nullify/dec/inc it upon delete, save it in prefs in onDestroy
			val adapter = WeatherInfoAdapter()
			val adapterDataObserver = object: RecyclerView.AdapterDataObserver() {
				override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
					if(itemCount == 1) {
						viewModel.selected = positionStart
						// move to newly added item
						pager.post {
							pager.currentItem = positionStart
						}
					} else if(viewModel.selected != null) { // else, we are prolly reloading fragment or sth
						pager.post {
							pager.currentItem = viewModel.selected!!
						}
					}
				}
			}
			adapter.registerAdapterDataObserver(adapterDataObserver)
			val onPageChangeCallback = object: OnPageChangeCallback() {
				override fun onPageSelected(pos: Int) {
					if(adapter.itemCount == 0) return
					viewModel.selected = pos
					val info = adapter.getItem(pos)
					tvLastUpdate.text = getString(
						R.string.f_last_update,
						DateUtils.getRelativeTimeSpanString(info.updateTime.toEpochMilli())
					)
					when(info.isDaytime) {
						true -> bg.apply { // animate to day
							if(currentPlayTime in 1..999) { // it's playing and going to night
								// let it play & smoothly jump forward after night
								currentPlayTime = animCallback.midPoint * 2 - currentPlayTime
								animCallback.toDay = true
							} else if(currentPlayTime == animCallback.midPoint)
								resume()
							// else, it's either 0 (day) or on its way to day already
						}
						false -> bg.apply { // animate to night
							if(currentPlayTime == 0L) // it's stopped/not started
								start()
							else if(currentPlayTime > animCallback.midPoint) { // it's playing and going to day
								// let it play & smoothly jump back before night
								currentPlayTime = animCallback.midPoint * 2 - currentPlayTime
								animCallback.toDay = false
							}
							// else, it's on its way to night already
						}
					}
				}
			}
			pager.adapter = adapter
			pager.registerOnPageChangeCallback(onPageChangeCallback)
			viewModel.infoList.observe(viewLifecycleOwner) {
				adapter.submitList(it)
				if(it.isEmpty()) {
					tvEmptyPager.visibility = View.VISIBLE
					swipeRefresh.visibility = View.GONE // to prevent swipe
					tvLastUpdate.visibility = View.GONE
				} else {
					tvEmptyPager.visibility = View.GONE
					swipeRefresh.visibility = View.VISIBLE
					tvLastUpdate.visibility = View.VISIBLE
				}
			}

			// setup swipeRefresh
			swipeRefresh.setOnRefreshListener {
				viewModel.updateSelected()
			}
			viewModel.refreshing.observe(viewLifecycleOwner) {
				swipeRefresh.isRefreshing = it
			}

			// setup error handling
			viewModel.error.observe(viewLifecycleOwner) {error ->
				Toast.makeText(
					requireContext(),
					when(error) {
						// obvious timeout (default 2.5 s)
						//is TimeoutError -> R.string.error_timed_out
						// no internet (in fact, no dns)
						//is NoConnectionError -> R.string.error_no_internet
						// server didn't like the request for some reason
						//is ServerError -> R.string.error_server_error
						// failed to parse response string to json
						//is ParseError -> R.string.error_parsing_resp
						// there is AuthFailureError as well from volley
						else -> R.string.error_unknown
					},
					Toast.LENGTH_LONG
				).show()
			}

			// setup toolbar
			toolbar.setOnMenuItemClickListener {item ->
				when(item.itemId) {
					R.id.action_add_location -> findNavController().navigate(R.id.action_open_map)
					R.id.action_delete -> viewModel.deleteSelected()
					R.id.action_delete_all -> viewModel.deleteAll()
					R.id.action_settings -> findNavController().navigate(R.id.action_settings)
					else -> return@setOnMenuItemClickListener false
				}
				true
			}
		}
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val args: HomeFragmentArgs by navArgs()
		args.latLng?.let {
			viewModel.addNewLocation("${it.latitude},${it.longitude}")
		}
		arguments?.clear() // fixes stupid re-reading of the args
	}
}