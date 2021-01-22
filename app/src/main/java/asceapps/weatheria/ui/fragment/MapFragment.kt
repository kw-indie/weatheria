package asceapps.weatheria.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import asceapps.weatheria.R
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.MapView

class MapFragment: Fragment() {

	private lateinit var mapView: MapView
	private lateinit var map: GoogleMap

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View = inflater.inflate(R.layout.fragment_map, container, false).apply {

		findViewById<Toolbar>(R.id.toolbar).apply {
			setNavigationOnClickListener {
				findNavController().navigateUp()
			}
			setOnMenuItemClickListener {item ->
				when(item.itemId) {
					R.id.action_ok -> {
						setFragmentResult(
							SearchFragment.LOCATION_KEY,
							bundleOf(SearchFragment.LOCATION_KEY to map.cameraPosition.target)
						)
						findNavController().navigateUp()
					}
					else -> return@setOnMenuItemClickListener false
				}
				true
			}
		}

		mapView = findViewById<MapView>(R.id.map_view).apply {
			onCreate(savedInstanceState)
			getMapAsync {
				it.apply {
					map = it
					uiSettings.apply {
						isMyLocationButtonEnabled = false
						isRotateGesturesEnabled = false
						isTiltGesturesEnabled = false
						isIndoorEnabled = false
						isMapToolbarEnabled = false
					}
					setOnMapClickListener {pos ->
						animateCamera(CameraUpdateFactory.newLatLng(pos))
					}
				}
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
		mapView.onStop()
	}

	override fun onDestroy() {
		super.onDestroy()
		mapView.onDestroy()
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		mapView.onSaveInstanceState(outState)
	}

	override fun onLowMemory() {
		super.onLowMemory()
		mapView.onLowMemory()
	}
}