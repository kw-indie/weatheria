package asceapps.weatheria.ui.fragment

import android.Manifest
import android.content.Context
import android.location.LocationManager
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import asceapps.weatheria.R
import asceapps.weatheria.databinding.FragmentAddLocationBinding
import asceapps.weatheria.ext.addDividers
import asceapps.weatheria.ext.hideKeyboard
import asceapps.weatheria.ext.observe
import asceapps.weatheria.ext.onSubmit
import asceapps.weatheria.shared.data.repo.Error
import asceapps.weatheria.shared.data.repo.Loading
import asceapps.weatheria.shared.data.repo.Success
import asceapps.weatheria.ui.adapter.AddLocationAdapter
import asceapps.weatheria.ui.viewmodel.AddLocationViewModel
import com.google.android.gms.location.LocationRequest
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.MapView
import com.google.android.libraries.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class AddLocationFragment: Fragment() {

	private val addLocationVM: AddLocationViewModel by viewModels()

	private lateinit var searchMenuItem: MenuItem
	private lateinit var searchView: SearchView
	private lateinit var mapView: MapView
	private lateinit var googleMap: GoogleMap

	// todo move to util or sth
	private val latLngFormat = "%1$.3f,%2$.3f"
	private val maxZoom = 13f
	private val searchZoom = 10f

	// need to register this anywhere before onCreateView
	private val permissionRequester = registerForActivityResult(
		ActivityResultContracts.RequestMultiplePermissions(),
		::permissionRequestCallback
	)

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		// turns out this can be called from here as well
		setHasOptionsMenu(true)

		val binding = FragmentAddLocationBinding.inflate(inflater, container, false)

		val addLocationAdapter = AddLocationAdapter(onItemClick = { loc ->
			addLocationVM.add(loc).observe(viewLifecycleOwner) {
				when(it) {
					is Loading -> {
						// todo show loading anim
					}
					is Success -> {
						// todo maybe stay to add more?
						findNavController().navigateUp()
					}
					is Error -> {
						// todo cover actual reasons
						showMessage(R.string.error_unknown)
					}
				}
			}
		})
		binding.rvResult.apply {
			setHasFixedSize(true)
			addDividers()
			adapter = addLocationAdapter
		}

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
								// if numbers aren't in arabic (1234..) api doesn't understand
								latLngFormat.format(Locale.US, latitude, longitude)
							}
						} else {
							addLocationAdapter.submitList(emptyList())
							""
						}
						searchView.apply {
							// this does not submit empty queries regardless of passed value
							setQuery(query, true)
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
				is Loading -> {
					// todo show loading anim
				}
				is Success -> {
					val list = it.data
					addLocationAdapter.submitList(list)
					val isEmpty = list.isEmpty()
					binding.tvNoResult.isVisible = isEmpty
					binding.rvResult.isVisible = !isEmpty
					if(!isEmpty) binding.rvResult.scrollToPosition(0)
				}
				is Error -> {
					// todo copy actual reasons from 'refresh' in HomeFragment
					showMessage(R.string.error_unknown)
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
			onSubmit { addLocationVM.search(it) }
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
		if(addLocationVM.useDeviceForLocation) {
			requirePermission()
		} else {
			addLocationVM.searchByIP()
		}
	}

	private fun requirePermission() {
		val permissions = arrayOf(
			if(addLocationVM.useHighAccuracyLocation) Manifest.permission.ACCESS_FINE_LOCATION
			else Manifest.permission.ACCESS_COARSE_LOCATION
		)
		permissionRequester.launch(permissions)
	}

	private fun permissionRequestCallback(map: Map<String, Boolean>) {
		val response = map.entries.first()
		val permission = response.key
		val isGranted = response.value
		when {
			isGranted -> onPermissionGranted()
			ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), permission) ->
				AlertDialog.Builder(requireContext())
					.setTitle(R.string.request_rationale_title)
					.setMessage(R.string.location_request_rationale)
					.setPositiveButton(R.string.give_permission) { _, _ -> requirePermission() }
					.setNegativeButton(R.string.dismiss, null)
					.create()
					.show()
			// permission permanently denied, ask user to manually enable from settings
			else -> showMessage(R.string.error_location_denied)
		}
	}

	private fun onPermissionGranted() {
		val ctx = requireContext()
		val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
		val accuracy =
			if(addLocationVM.useHighAccuracyLocation) LocationRequest.PRIORITY_HIGH_ACCURACY
			else LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
		if(LocationManagerCompat.isLocationEnabled(lm)) {
			addLocationVM.getDeviceLocation(ctx, accuracy).observe(viewLifecycleOwner) {
				when(it) {
					is Loading -> {
						// todo show loading anim
					}
					is Success -> {
						with(it.data) {
							googleMap.animateCamera(
								CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), maxZoom)
							)
						}
					}
					is Error -> {
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
		} else {
			showMessage(R.string.error_location_disabled)
		}
	}
}