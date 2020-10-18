package asceapps.weatheria.ui.home

import android.graphics.Color
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.datastore.preferences.preferencesKey
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.SeekableAnimatedVectorDrawable
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import asceapps.weatheria.R
import asceapps.weatheria.data.WeatherInfoRepo
import asceapps.weatheria.databinding.FragmentHomeBinding
import asceapps.weatheria.ui.MainActivity
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class HomeFragment: Fragment() {

	private val viewModel: HomeViewModel by viewModels {
		HomeViewModel.Factory(WeatherInfoRepo.getInstance(requireContext()), this)
	}
	private lateinit var adapter: WeatherInfoAdapter

	// in fragments, use nullable var for binding, as fragments outlive their views
	private var _binding: FragmentHomeBinding? = null
	private val binding get() = _binding!!
	private var onPageChangeCallback: OnPageChangeCallback? = null
	private var adapterDataObserver: RecyclerView.AdapterDataObserver? = null
	private lateinit var bg: SeekableAnimatedVectorDrawable
	private val animCallback = object: SeekableAnimatedVectorDrawable.AnimationCallback() {
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
	private val parentRoot: View? get() = view?.rootView?.findViewById(R.id.root)
	private var oldStatusBarColor = 0

	override fun onCreateView(
		inflater: LayoutInflater, root: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		setHasOptionsMenu(true)
		_binding = FragmentHomeBinding.inflate(inflater, root, false)
		binding.viewModel = viewModel
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		setUpSwipeRefresh()
		setUpBackground()
		setUpPager()
		setUpPreferences()
		setUpErrorHandler()
		val args: HomeFragmentArgs by navArgs()
		args.latLng?.let {
			viewModel.addNewLocation("${it.latitude},${it.longitude}")
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		binding.pager.unregisterOnPageChangeCallback(onPageChangeCallback!!)
		adapter.unregisterAdapterDataObserver(adapterDataObserver!!)
		_binding = null
		onPageChangeCallback = null
		adapterDataObserver = null
	}

	override fun onStart() {
		super.onStart()
		changeRootBackground()
	}

	override fun onStop() {
		super.onStop()
		revertRootBackground()
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.home_menu, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when(item.itemId) {
			R.id.action_add_location ->
				NavHostFragment.findNavController(this).navigate(R.id.action_open_map)
			R.id.action_delete ->
				viewModel.delete(adapter.getItemId(binding.pager.currentItem).toInt())
			R.id.action_delete_all -> viewModel.deleteAll()
			R.id.action_settings ->
				NavHostFragment.findNavController(this).navigate(R.id.action_settings)
			else -> return super.onOptionsItemSelected(item)
		}
		return true
	}

	private fun setUpSwipeRefresh() {
		binding.swipeRefresh.apply {
			isRefreshing = true
			setOnRefreshListener {
				viewModel.update(adapter.getItemId(binding.pager.currentItem).toInt())
			}
		}
		viewModel.refreshing.observe(viewLifecycleOwner) {binding.swipeRefresh.isRefreshing = it}
	}

	private fun changeRootBackground() {
		oldStatusBarColor = requireActivity().window.statusBarColor
		requireActivity().window.statusBarColor = Color.TRANSPARENT
		parentRoot?.background = bg
	}

	private fun revertRootBackground() {
		requireActivity().window.statusBarColor = oldStatusBarColor
		parentRoot?.background = null
	}

	private fun setUpBackground() {
		SeekableAnimatedVectorDrawable.create(requireContext(), R.drawable.bg_daynight)?.apply {
			bg = this
			start() // fixes a bug where everything is correct but background isn't drawn
			stop()
			registerAnimationCallback(animCallback)
		}
	}

	private fun animateToNight() {
		bg.apply {
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

	private fun animateToDay() {
		bg.apply {
			if(currentPlayTime in 1..999) { // it's playing and going to night
				// let it play & smoothly jump forward after night
				currentPlayTime = animCallback.midPoint * 2 - currentPlayTime
				animCallback.toDay = true
			} else if(currentPlayTime == animCallback.midPoint)
				resume()
			// else, it's either 0 (day) or on its way to day already
		}
	}

	private fun setUpPager() {
		adapter = WeatherInfoAdapter()
		adapterDataObserver = object: RecyclerView.AdapterDataObserver() {
			override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
				if(itemCount == 1) binding.pager.post {
					binding.pager.currentItem = positionStart
				}
			}
		}
		adapter.registerAdapterDataObserver(adapterDataObserver!!)
		viewModel.infoList.observe(viewLifecycleOwner) {all ->
			adapter.submitList(all)
			binding.swipeRefresh.isRefreshing = false
			if(all.isNotEmpty()) {
				binding.apply {
					tvEmptyPager.visibility = View.GONE
					swipeRefresh.visibility = View.VISIBLE
					tvLastUpdate.visibility = View.VISIBLE
					if(pager.adapter != adapter) {
						// when list is loaded for the first time
						pager.adapter = adapter
					}
				}
				// todo get selected from prefs
				viewModel.selected?.let {binding.pager.setCurrentItem(it, false)}
			} else {
				binding.apply {
					tvEmptyPager.visibility = View.VISIBLE
					swipeRefresh.visibility = View.GONE
					tvLastUpdate.visibility = View.GONE
				}
			}
		}
		onPageChangeCallback = object: OnPageChangeCallback() {
			override fun onPageSelected(pos: Int) {
				if(adapter.itemCount == 0) return
				viewModel.selected = pos
				val info = adapter.getItem(pos)
				binding.tvLastUpdate.text = getString(
					R.string.f_last_update,
					DateUtils.getRelativeTimeSpanString(info.location.updatedAt)
				)
				// icon name has d or n for day/night
				if('d' in info.current.icon) {
					animateToDay()
				} else {
					animateToNight()
				}
			}
		}
		binding.pager.registerOnPageChangeCallback(onPageChangeCallback!!)
	}

	private fun setUpPreferences() {
		val dataStore = (requireActivity() as MainActivity).dataStore
		val unitsKey = preferencesKey<String>(getString(R.string.key_units))
		val unitsFlow = dataStore.data.map {it[unitsKey] ?: "0"}
		lifecycleScope.launch {
			unitsFlow.collect {
				adapter.metric = it == "0"
			}
		}
	}

	private fun setUpErrorHandler() {
		viewModel.error.observe(viewLifecycleOwner) {error ->
			when(error) {
				// obvious timeout (default 2.5 s)
				//is TimeoutError -> showMessage(R.string.error_timed_out)
				// no internet (in fact, no dns)
				//is NoConnectionError -> showMessage(R.string.error_no_internet)
				// server didn't like the request for some reason
				//is ServerError -> showMessage(R.string.error_server_error)
				// failed to parse response string to json
				//is ParseError -> showMessage(R.string.error_parsing_resp)
				// there is AuthFailureError as well from volley
				else -> showMessage(R.string.error_unknown)
			}
		}
	}

	private fun showMessage(resId: Int) {
		Toast.makeText(requireContext(), resId, Toast.LENGTH_LONG).show()
	}
}