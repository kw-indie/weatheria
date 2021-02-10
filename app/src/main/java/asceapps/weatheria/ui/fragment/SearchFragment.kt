package asceapps.weatheria.ui.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.core.location.LocationManagerCompat
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

	private val mainVM: MainViewModel by activityViewModels()
	private val viewModel: SearchViewModel by viewModels()
	private val permission = Manifest.permission.ACCESS_COARSE_LOCATION
	private lateinit var searchMenuItem: MenuItem
	private lateinit var searchView: SearchView
	private lateinit var mapView: MapView
	private lateinit var googleMap: GoogleMap

	// need to register this anywhere before onCreateView
	private val permissionRequester = registerForActivityResult(
		ActivityResultContracts.RequestPermission()
	) {checkLocationPermission(true, false)}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
	}

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		return FragmentSearchBinding.inflate(inflater, container, false).apply {
			val maxZoom = 13f
			val searchZoom = 11f
			mapView = map.apply {
				onCreate(savedInstanceState)
				getMapAsync {map ->
					map.apply {
						googleMap = this
						checkLocationPermission(false, true)
						setMaxZoomPreference(maxZoom)
						uiSettings.apply {
							isZoomControlsEnabled = true
							isRotateGesturesEnabled = false
							isTiltGesturesEnabled = false
							isIndoorEnabled = false
							isMapToolbarEnabled = false
						}
						setOnMyLocationButtonClickListener {
							// todo disable when already in progress
							getLocation()
							true
						}
						setOnMapClickListener {latLng ->
							animateCamera(CameraUpdateFactory.newLatLng(latLng))
						}
						setOnCameraIdleListener {
							val query = if(cameraPosition.zoom >= searchZoom) {
								searchMenuItem.expandActionView()
								with(cameraPosition.target) {
									"%1$.3f,%2$.3f".format(latitude, longitude)
								}
							} else {
								searchMenuItem.collapseActionView()
								""
							}
							searchView.setQuery(query, true)
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
				googleMap.animateCamera(CameraUpdateFactory
					.newLatLngZoom(LatLng(it.latitude, it.longitude), maxZoom)
				)
			}
			viewModel.result.observe(viewLifecycleOwner) {
				adapter.submitList(it)
				if(it.isEmpty()) {
					tvNoResult.visibility = View.VISIBLE
					rvResults.visibility = View.INVISIBLE
				} else {
					tvNoResult.visibility = View.GONE
					rvResults.visibility = View.VISIBLE
					rvResults.smoothScrollToPosition(0)
				}
			}
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

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.search_menu, menu)
		searchMenuItem = menu.findItem(R.id.action_search)
		searchView = searchMenuItem.actionView as SearchView
		searchView.apply {
			setIconifiedByDefault(false)
			queryHint = getString(R.string.search_hint)
			setOnQueryTextListener(object: SearchView.OnQueryTextListener {
				override fun onQueryTextSubmit(query: String?): Boolean {
					clearFocus()
					return true
				}

				override fun onQueryTextChange(newText: String?): Boolean {
					viewModel.query.value = newText
					return true
				}
			})
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
		mapView.onStop()
	}

	override fun onDestroy() {
		super.onDestroy()
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

	private fun checkLocationPermission(tryShowRationale: Boolean, request: Boolean) {
		when {
			ContextCompat.checkSelfPermission(requireContext(), permission) == PERMISSION_GRANTED -> {
				// our method gets called from different contexts, some of them don't guarantee initialization
				if(::googleMap.isInitialized) {
					@SuppressLint("MissingPermission")
					googleMap.isMyLocationEnabled = true
				}
			}
			tryShowRationale &&
				ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), permission) -> {
				AlertDialog.Builder(requireContext())
					.setTitle(R.string.request_rationale_title)
					.setMessage(R.string.location_request_rationale)
					.setPositiveButton(R.string.request_again) {_, _ ->
						requestPermission()
					}
					.setNegativeButton(R.string.dismiss, null)
					.create()
					.show()
			}
			request -> {
				requestPermission()
			}
		}
	}

	private fun requestPermission() {
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			permissionRequester.launch(permission)
		} else {
			showMessage(R.string.error_location_denied)
		}
	}

	private fun getLocation() {
		val lm = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
		if(LocationManagerCompat.isLocationEnabled(lm)) {
			viewModel.getMyLocation(LocationServices.getFusedLocationProviderClient(requireContext()))
		} else {
			showMessage(R.string.error_location_disabled)
		}
	}
}