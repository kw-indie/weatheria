package asceapps.weatheria.ui.search

import asceapps.weatheria.data.LocationEntity

fun interface OnLocationClickListener {

	fun onLocationClick(l: LocationEntity)
}