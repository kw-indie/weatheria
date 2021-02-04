package asceapps.weatheria.ui.fragment

import android.Manifest
import android.content.Context
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import asceapps.weatheria.R
import asceapps.weatheria.databinding.FragmentSearchBinding
import asceapps.weatheria.ui.adapter.SearchAdapter
import asceapps.weatheria.ui.viewmodel.MainViewModel
import asceapps.weatheria.ui.viewmodel.SearchViewModel
import asceapps.weatheria.util.hideKeyboard
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.MapView
import com.google.android.libraries.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class SearchFragment: Fragment() {

	companion object {

		const val LOCATION_KEY = "location_key"
	}

	private val mainVM: MainViewModel by activityViewModels()
	private val viewModel: SearchViewModel by viewModels()
	private lateinit var mapView: MapView
	private lateinit var googleMap: GoogleMap

	private lateinit var locationPermissionRequester: ActivityResultLauncher<String>

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// todo make this callbackFlow?
		// need to register these in onCreate
		locationPermissionRequester = registerForActivityResult(
			ActivityResultContracts.RequestPermission()
		) {
			when(it) {
				true -> afterGettingLocationPermission()
				else -> showMessage(R.string.error_location_denied)
			}
		}

		// todo request location permission cuz otherwise map won't show myLocation button
	}

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		return FragmentSearchBinding.inflate(inflater, container, false).apply {
			// binding vm to view
			vm = viewModel

			searchLayout.setStartIconOnClickListener {
				findNavController().navigateUp()
			}

			val defaultZoom = 10f
			mapView = map.apply {
				onCreate(savedInstanceState)
				getMapAsync {map ->
					map.apply {
						googleMap = map
						try {
							isMyLocationEnabled = true
						} catch(ignore: SecurityException) {
						}
						uiSettings.apply {
							isZoomControlsEnabled = true
							isRotateGesturesEnabled = false
							isTiltGesturesEnabled = false
							isIndoorEnabled = false
							isMapToolbarEnabled = false
						}
						setOnMyLocationButtonClickListener {
							onGetMyLocationClick()
							true
						}
						setOnMapClickListener {pos ->
							animateCamera(CameraUpdateFactory.newLatLngZoom(pos, defaultZoom))
						}
					}
				}
			}

			val adapter = SearchAdapter {
				mainVM.addNewLocation(it)
			}
			rvResults.apply {
				this.adapter = adapter
				val layoutManager = rvResults.layoutManager as LinearLayoutManager
				val divider = DividerItemDecoration(context, layoutManager.orientation)
				addItemDecoration(divider)
				setHasFixedSize(true)
			}

			viewModel.myLocation.observe(viewLifecycleOwner) {
				etSearch.setText("${it.latitude},${it.longitude}")
				googleMap.animateCamera(CameraUpdateFactory
					.newLatLngZoom(LatLng(it.latitude, it.longitude), defaultZoom)
				)
			}
			viewModel.result.observe(viewLifecycleOwner, adapter::submitList)
			viewModel.error.observe(viewLifecycleOwner) {
				Toast.makeText(
					requireContext(),
					// todo did we cover other reasons?
					when(it) {
						is NullPointerException -> R.string.error_location_not_found
						else -> R.string.error_unknown
					},
					Toast.LENGTH_LONG
				).show()
			}
			// fixme find better way to go back on success
			mainVM.savedLocationsList
				.drop(1) // first time load
				.onEach { // go back once new value is added successfully
					findNavController().navigateUp()
				}.launchIn(viewLifecycleOwner.lifecycleScope)
		}.root
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
		mapView.onStop()
	}

	override fun onDestroy() {
		super.onDestroy()
		locationPermissionRequester.unregister()
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

	private fun onGetMyLocationClick() {
		hideKeyboard(requireView())
		// todo disable button when already in progress
		val permission = Manifest.permission.ACCESS_COARSE_LOCATION
		when {
			ContextCompat.checkSelfPermission(requireContext(), permission) == PERMISSION_GRANTED ->
				afterGettingLocationPermission()
			// todo case: showRationale
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> locationPermissionRequester.launch(permission)
			else -> showMessage(R.string.error_location_denied)
		}
	}

	private fun afterGettingLocationPermission() {
		val lm = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
		// if location service is enabled
		if(lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ||
			lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			showMessage(R.string.getting_location)
			viewModel.getMyLocation(
				LocationServices.getFusedLocationProviderClient(requireContext())
			)
		} else {
			showMessage(R.string.error_location_disabled)
		}
	}
}