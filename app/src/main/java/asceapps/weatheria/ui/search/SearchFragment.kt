package asceapps.weatheria.ui.search

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
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import asceapps.weatheria.R
import asceapps.weatheria.databinding.FragmentSearchBinding
import asceapps.weatheria.util.hideKeyboard
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchFragment: Fragment() {

	companion object {

		const val LOCATION_KEY = "location_key"
	}

	private val viewModel: SearchViewModel by activityViewModels()
	private val locationPermissions = arrayOf(
		Manifest.permission.ACCESS_FINE_LOCATION,
		Manifest.permission.ACCESS_COARSE_LOCATION
	)
	private lateinit var locationPermissionRequester: ActivityResultLauncher<Array<String>>

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// listen for selected locations from MapFragment (auto clears after read)
		setFragmentResultListener(LOCATION_KEY) {_, bundle ->
			val location = bundle[LOCATION_KEY] as? LatLng
			location?.apply {
				viewModel.query.value = "$latitude,$longitude"
			}
		}

		// need to register these in onCreate
		locationPermissionRequester = registerForActivityResult(
			ActivityResultContracts.RequestMultiplePermissions()
		) {
			if(it.containsValue(true))
				afterGettingLocationPermission()
			else
				showMessage(R.string.error_location_denied)
		}
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
			myLocation.setOnClickListener {
				onGetMyLocationClick()
			}
			openMap.setOnClickListener {
				findNavController().navigate(R.id.mapFragment)
			}

			val adapter = SearchResultAdapter {
				viewModel.addNewLocation(it)
			}
			rvResults.apply {
				this.adapter = adapter
				val layoutManager = rvResults.layoutManager as LinearLayoutManager
				val divider = DividerItemDecoration(context, layoutManager.orientation)
				addItemDecoration(divider)
				setHasFixedSize(true)
			}

			viewModel.result.observe(viewLifecycleOwner) {
				if(it.isNullOrEmpty()) {
					myLocation.visibility = View.VISIBLE
					openMap.visibility = View.VISIBLE
				} else {
					myLocation.visibility = View.GONE
					openMap.visibility = View.GONE
				}
				adapter.submitList(it)
			}
			viewModel.error.observe(viewLifecycleOwner) {error ->
				Toast.makeText(
					requireContext(),
					when(error) {
						else -> R.string.error_unknown
					},
					Toast.LENGTH_LONG
				).show()
			}
			var first = true
			viewModel.savedLocations.observe(viewLifecycleOwner) {
				if(first) {
					first = false
				} else {
					findNavController().navigateUp()
				}
			}
		}.root
	}

	override fun onDestroy() {
		super.onDestroy()
		locationPermissionRequester.unregister()
	}

	// todo share
	private fun showMessage(resId: Int) {
		Toast.makeText(requireContext(), resId, Toast.LENGTH_LONG).show()
	}

	private fun onGetMyLocationClick() {
		hideKeyboard(requireView())
		// todo disable button when already in progress
		when {
			// todo coarse location is enough for us
			locationPermissions.any {
				ContextCompat.checkSelfPermission(requireContext(), it) == PERMISSION_GRANTED
			} -> {
				afterGettingLocationPermission()
			}
			// todo case: showRationale
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
				locationPermissionRequester.launch(locationPermissions)
			}
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