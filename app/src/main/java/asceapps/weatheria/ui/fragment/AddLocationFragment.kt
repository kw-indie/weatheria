package asceapps.weatheria.ui.fragment

import android.content.Context
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.location.LocationManagerCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import asceapps.weatheria.R
import asceapps.weatheria.data.repo.Result
import asceapps.weatheria.data.repo.SettingsRepo
import asceapps.weatheria.databinding.FragmentAddLocationBinding
import asceapps.weatheria.ui.adapter.SearchAdapter
import asceapps.weatheria.ui.viewmodel.AddLocationViewModel
import asceapps.weatheria.ui.viewmodel.MainViewModel
import asceapps.weatheria.util.*
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.MapView
import com.google.android.libraries.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// todo move to util or sth
private const val latLngFormat = "%1$.3f,%2$.3f"

@AndroidEntryPoint
class AddLocationFragment: Fragment() {

	private val mainVM: MainViewModel by activityViewModels()
	private val addLocationVM: AddLocationViewModel by viewModels()
	@Inject
	lateinit var settingsRepo: SettingsRepo

	private lateinit var searchMenuItem: MenuItem
	private lateinit var searchView: SearchView
	private lateinit var mapView: MapView
	private lateinit var googleMap: GoogleMap

	// need to register this anywhere before onCreateView
	private val permissionRequester = createPermissionRequester { if(it) onPermissionGranted() }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		setLocationAccuracyHigh(settingsRepo.isLocationAccuracyHigh)

		val binding = FragmentAddLocationBinding.inflate(inflater, container, false)

		val searchAdapter = SearchAdapter {
			mainVM.addNewLocation(it)
			findNavController().navigateUp()
		}
		binding.rvResults.apply {
			adapter = searchAdapter
			val layoutManager = layoutManager as LinearLayoutManager
			val divider = DividerItemDecoration(context, layoutManager.orientation)
			addItemDecoration(divider)
			setHasFixedSize(true)
		}

		val maxZoom = 13f
		val searchZoom = 11f
		mapView = binding.map.apply {
			onCreate(savedInstanceState)
			getMapAsync { map ->
				map.apply {
					googleMap = this
					setMaxZoomPreference(maxZoom)
					uiSettings.apply {
						isMyLocationButtonEnabled = false // we have our own
						isZoomControlsEnabled = true
						isRotateGesturesEnabled = false
						isTiltGesturesEnabled = false
						isIndoorEnabled = false
						isMapToolbarEnabled = false
					}
					setOnMapClickListener { latLng ->
						animateCamera(CameraUpdateFactory.newLatLng(latLng))
					}
					setOnCameraIdleListener {
						val query = if(cameraPosition.zoom >= searchZoom) {
							with(cameraPosition.target) {
								latLngFormat.format(latitude, longitude)
							}
						} else {
							searchAdapter.submitList(emptyList())
							""
						}
						searchView.apply {
							// this does not submit empty queries regardless of passed value
							setQuery(query, false)
						}
					}
				}
			}
		}
		binding.btnMyLocation.setOnClickListener {
			onMyLocationClick()
		}

		addLocationVM.searchResult.observe(viewLifecycleOwner) {
			when(it) {
				is Result.Loading -> {
					// todo show loading anim
				}
				is Result.Success -> {
					val list = it.data
					searchAdapter.submitList(list)
					val isEmpty = list.isEmpty()
					binding.tvNoResult.isVisible = isEmpty
					binding.rvResults.isVisible = !isEmpty
					if(!isEmpty)
						binding.rvResults.smoothScrollToPosition(0)
				}
				is Result.Error -> {
					// this shouldn't happen
				}
			}
		}
		addLocationVM.deviceLocation.observe(viewLifecycleOwner) {
			when(it) {
				is Result.Loading -> {
					// todo show loading anim
				}
				is Result.Success -> {
					with(it.data) {
						googleMap.animateCamera(
							CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), maxZoom)
						)
					}
				}
				is Result.Error -> {
					showMessage(
						// todo did we cover other reasons?
						when(it.t) {
							is NullPointerException -> R.string.error_location_not_found
							else -> R.string.error_unknown
						}
					)
				}
			}
		}
		addLocationVM.ipGeolocation.observe(viewLifecycleOwner) {
			when(it) {
				is Result.Loading -> {
					// todo show loading anim
				}
				is Result.Success -> {
					val (lat, lng) = it.data.split(",").map { d -> d.toDouble() }
					googleMap.animateCamera(
						// todo move zoom to const
						CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), maxZoom)
					)
				}
				is Result.Error -> {
					// todo retrofit errors from mainActivity
					//showMessage(R.string.error_unknown)
				}
			}
		}

		return binding.root
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.add_location_menu, menu)
		searchMenuItem = menu.findItem(R.id.action_search)
		searchView = (searchMenuItem.actionView as SearchView).apply {
			setIconifiedByDefault(false)
			queryHint = getString(R.string.hint_search)
			onTextSubmitFlow().observe(viewLifecycleOwner) {
				addLocationVM.setQuery(it)
			}
		}
	}

	override fun onStart() {
		super.onStart()
		mapView.onStart()
	}

	override fun onResume() {
		super.onResume()
		mapView.onResume()
	}

	override fun onPause() {
		super.onPause()
		mapView.onPause()
	}

	override fun onStop() {
		super.onStop()
		hideKeyboard(requireView())
		mapView.onStop()
	}

	override fun onDestroy() {
		super.onDestroy()
		mapView.onDestroy()
		mapView.removeAllViews()
		permissionRequester.unregister()
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		mapView.onSaveInstanceState(outState)
	}

	override fun onLowMemory() {
		super.onLowMemory()
		mapView.onLowMemory()
	}

	// todo share
	private fun showMessage(resId: Int) {
		Toast.makeText(requireContext(), resId, Toast.LENGTH_LONG).show()
	}

	private fun onMyLocationClick() {
		if(settingsRepo.useDeviceForLocation) {
			updateDeviceLocation()
		} else {
			addLocationVM.updateIpGeolocation()
		}
	}

	private fun updateDeviceLocation() {
		when {
			isLocationPermissionGranted(requireContext()) -> {
				onPermissionGranted()
			}
			shouldShowLocationPermissionRationale(requireActivity()) -> {
				AlertDialog.Builder(requireContext())
					.setTitle(R.string.request_rationale_title)
					.setMessage(R.string.location_request_rationale)
					.setPositiveButton(R.string.give_permission) { _, _ ->
						// if we are here, it's guaranteed api 23+, no need to check
						requestPermission()
					}
					.setNegativeButton(R.string.dismiss, null)
					.create()
					.show()
			}
			else -> { // does not have permission, not first time, but user still would like to use device
				requestPermission()
			}
		}
	}

	private fun requestPermission() {
		// no need to check for android version. this is always only run on M+
		requestLocationPermission(permissionRequester)
	}

	private fun onPermissionGranted() {
		val lm = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
		if(LocationManagerCompat.isLocationEnabled(lm)) {
			addLocationVM.updateDeviceLocation()
		} else {
			showMessage(R.string.error_location_disabled)
		}
	}
}