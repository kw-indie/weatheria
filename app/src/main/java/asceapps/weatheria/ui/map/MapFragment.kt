package asceapps.weatheria.ui.map

import android.Manifest.permission
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import asceapps.weatheria.R
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.MapView
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.Marker
import com.google.android.libraries.maps.model.MarkerOptions

class MapFragment: Fragment() {

	private lateinit var mapView: MapView
	private lateinit var map: GoogleMap
	private lateinit var marker: Marker
	private lateinit var locationPermissionRequester: ActivityResultLauncher<Array<String>>

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		@SuppressLint("MissingPermission")
		locationPermissionRequester = registerForActivityResult(
			ActivityResultContracts.RequestMultiplePermissions()
		) {afterLocationPermission(it.containsValue(true))}
	}

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val root = inflater.inflate(R.layout.fragment_map, container, false)

		root.findViewById<Toolbar>(R.id.toolbar).apply {
			setNavigationOnClickListener {
				findNavController().navigateUp()
			}
			setOnMenuItemClickListener {item ->
				when(item.itemId) {
					R.id.action_ok -> findNavController().navigate(
						MapFragmentDirections.actionOkNewLocation(marker.position)
					)
					R.id.action_give_permission -> beforeLocationPermission()
					else -> return@setOnMenuItemClickListener false
				}
				true
			}
		}

		mapView = root.findViewById(R.id.map_view)
		mapView.apply {
			onCreate(savedInstanceState)
			getMapAsync(::onMapReady)
		}

		return root
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
		mapView.onDestroy()
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

	private fun onMapReady(map: GoogleMap) {
		map.apply {
			this@MapFragment.map = this
			val london = LatLng(51.5098, -0.1181)
			marker = addMarker(MarkerOptions().position(london))

			moveCamera(CameraUpdateFactory.newLatLngZoom(london, 12f))
			setOnMapClickListener {moveCamera(CameraUpdateFactory.newLatLng(it))}
			setOnCameraIdleListener {marker.position = cameraPosition.target}

			beforeLocationPermission()
		}

		beforeLocationPermission()
	}

	private fun beforeLocationPermission() {
		val activity = requireActivity()
		// if permission is granted
		if(ActivityCompat.checkSelfPermission(activity, PERMISSIONS[0]) == PERMISSION_GRANTED ||
			ActivityCompat.checkSelfPermission(activity, PERMISSIONS[1]) == PERMISSION_GRANTED) {
			afterLocationPermission(true)
		} else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			locationPermissionRequester.launch(PERMISSIONS)
		} else {
			showMessage(R.string.error_location_denied)
		}
	}

	private fun afterLocationPermission(granted: Boolean) {
		val context = requireContext()
		map.apply {
			if(granted) {
				uiSettings.isMyLocationButtonEnabled = true
				@SuppressLint("MissingPermission")
				isMyLocationEnabled = true
				val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
				setOnMyLocationButtonClickListener {
					// if location service is enabled
					if(lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ||
						lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
						false
					} else {
						showMessage(R.string.error_location_disabled)
						true
					}
				}
			} else {
				uiSettings.isMyLocationButtonEnabled = false
				showMessage(R.string.error_location_denied)
			}
		}
	}

	// todo share this method?
	private fun showMessage(strResId: Int) {
		Toast.makeText(requireContext(), strResId, Toast.LENGTH_LONG).show()
	}

	companion object {

		private val PERMISSIONS: Array<String> = arrayOf(
			permission.ACCESS_COARSE_LOCATION,
			permission.ACCESS_FINE_LOCATION
		)
	}
}