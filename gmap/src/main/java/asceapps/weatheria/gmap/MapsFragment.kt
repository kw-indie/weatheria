package asceapps.weatheria.gmap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import asceapps.weatheria.ui.fragment.MapView
import asceapps.weatheria.ui.fragment.MapViewListener
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.SupportMapFragment
import com.google.android.libraries.maps.model.LatLng

class MapsFragment(private val listener: MapViewListener): Fragment(), MapView {

	private lateinit var googleMap: GoogleMap
	private val focusZoom = 10f
	private val maxZoom = 13f

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		return inflater.cloneInContext(requireContext().applicationContext)
			.inflate(R.layout.fragment_maps, container, false)
	}

	override fun animateCamera(lat: Double, lng: Double) {
		googleMap.animateCamera(
			CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), maxZoom)
		)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
		mapFragment?.getMapAsync {
			it.apply {
				googleMap = this
				setMaxZoomPreference(maxZoom)
				uiSettings.isMyLocationButtonEnabled = false
				setOnMapClickListener { latLng ->
					animateCamera(CameraUpdateFactory.newLatLng(latLng))
				}
				var notifiedOutOfFocus = false
				setOnCameraIdleListener {
					if(cameraPosition.zoom >= focusZoom) {
						notifiedOutOfFocus = false
						val target = cameraPosition.target
						listener.onMapFocus(target.latitude, target.longitude)
					} else if(!notifiedOutOfFocus) {
						notifiedOutOfFocus = true
						listener.onMapOutOfFocus()
					}
				}
			}
		}
	}
}