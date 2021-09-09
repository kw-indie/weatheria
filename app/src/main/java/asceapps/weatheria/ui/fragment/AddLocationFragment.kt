package asceapps.weatheria.ui.fragment

import android.Manifest
import android.annotation.SuppressLint
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
import asceapps.weatheria.shared.data.result.Error
import asceapps.weatheria.shared.data.result.Loading
import asceapps.weatheria.shared.data.result.Success
import asceapps.weatheria.ui.adapter.AddLocationAdapter
import asceapps.weatheria.ui.viewmodel.AddLocationViewModel
import asceapps.weatheria.util.Formatter
import com.google.android.gms.location.LocationRequest
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.ktx.bytesDownloaded
import com.google.android.play.core.ktx.status
import com.google.android.play.core.ktx.totalBytesToDownload
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddLocationFragment: Fragment() {

	private val addLocationVM: AddLocationViewModel by viewModels()

	private lateinit var searchMenuItem: MenuItem
	private lateinit var searchView: SearchView
	private lateinit var binding: FragmentAddLocationBinding

	// need to register this anywhere before onCreateView
	private val permissionRequester = registerForActivityResult(
		ActivityResultContracts.RequestMultiplePermissions(),
		::permissionRequestCallback
	)

	private lateinit var installProgressSnackbar: Snackbar
	private lateinit var downloadMapMenuItem: MenuItem
	private var mapView: MapView? = null
	private val moduleName = "gmap"
	private val fragmentQualifiedName = "asceapps.weatheria.$moduleName.MapsFragment"

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		// turns out this can be called from here as well
		setHasOptionsMenu(true)

		binding = FragmentAddLocationBinding.inflate(inflater, container, false)

		installProgressSnackbar = Snackbar.make(container!!, "", Snackbar.LENGTH_INDEFINITE)
		addLocationVM.moduleInstallProgress.observe(viewLifecycleOwner) { state ->
			downloadMapMenuItem.isEnabled = state.status !in DOWNLOADING..INSTALLING
			@SuppressLint("SwitchIntDef")
			when(state.status) {
				PENDING -> setProgressMessage(false, R.string.starting_download)
				DOWNLOADING -> {
					// per docs, this will only show if the app is really being downloaded from play store
					val p = (state.bytesDownloaded * 100 / state.totalBytesToDownload).toInt()
					val sizeText = android.text.format.Formatter.formatShortFileSize(
						requireContext(),
						state.totalBytesToDownload()
					)
					setProgressMessage(
						false,
						R.string.f_2s_p,
						getString(R.string.f_2s, getString(R.string.downloading), sizeText),
						Formatter.percent(p)
					)
				}
				INSTALLING -> setProgressMessage(false, R.string.installing)
				INSTALLED -> {
					setProgressMessage(true, R.string.installed)
					requireActivity().invalidateOptionsMenu()
				}
				FAILED -> setProgressMessage(true, R.string.install_failed)
			}
		}

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
						// todo cover actual reasons. should be same as any other network call
						// better make 'add' and 'fetch info' separate actions
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
			maxWidth = resources.getDimensionPixelSize(R.dimen._192sdp)
			queryHint = getString(R.string.action_search)
			onSubmit { addLocationVM.search(it) }
		}

		downloadMapMenuItem = menu.findItem(R.id.action_download_map)
	}

	override fun onPrepareOptionsMenu(menu: Menu) {
		// a very convenient place to call this:
		// - gets called when fragment is loaded, can be recalled via invalidate on module change
		if(addLocationVM.isModuleInstalled(moduleName)) {
			downloadMapMenuItem.title = getString(R.string.action_uninstall_map)
			addMapFragment()
		} else {
			downloadMapMenuItem.title = getString(R.string.action_download_map)
			removeMapFragment()
		}
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when(item.itemId) {
			R.id.action_get_my_location -> onMyLocationClick()
			R.id.action_download_map -> onDownloadMapMenuItemClicked()
			else -> return false
		}
		return true
	}

	override fun onStop() {
		super.onStop()
		hideKeyboard(requireView())
	}

	override fun onDestroy() {
		super.onDestroy()
		permissionRequester.unregister()
	}

	private fun addMapFragment() {
		if(binding.mapContainer.childCount != 0) return // already loaded
		binding.mapContainer.isVisible = true

		val mapListener = object: MapViewListener {
			override fun onMapFocus(lat: Double, lng: Double) {
				updateSearchQuery(Formatter.coords(lat, lng))
			}

			override fun onMapOutOfFocus() {
				// we used to clear recycler view result as well here, but they are expensive to get,
				// so we will keep them
				updateSearchQuery("")
			}
		}
		val mapsFragment = Class.forName(fragmentQualifiedName)
			.getConstructor(MapViewListener::class.java)
			.newInstance(mapListener) as Fragment
		mapView = mapsFragment as MapView
		childFragmentManager.beginTransaction()
			.add(R.id.map_container, mapsFragment)
			.commitNow()
	}

	private fun removeMapFragment() {
		if(binding.mapContainer.childCount == 0) return // already removed
		childFragmentManager.beginTransaction()
			.remove(mapView as Fragment)
			.commitNow()

		binding.mapContainer.isVisible = false
	}

	// todo share
	private fun showMessage(resId: Int) {
		Toast.makeText(requireContext(), resId, Toast.LENGTH_LONG).show()
	}

	private fun setProgressMessage(terminalMsg: Boolean, resId: Int, vararg args: Any) {
		installProgressSnackbar.apply {
			setText(getString(resId, *args))
			if(!isShownOrQueued) show()
			if(terminalMsg) {
				view.postDelayed(::dismiss, 3000L)
			}
		}
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
					.setNegativeButton(R.string.deny, null)
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
			// this flow closes in 2 emissions: loading + a result
			addLocationVM.getDeviceLocation(ctx, accuracy).observe(viewLifecycleOwner) {
				when(it) {
					is Loading -> {
						// todo show loading anim
					}
					is Success -> {
						with(it.data) {
							if(mapView == null) {
								updateSearchQuery(Formatter.coords(latitude, longitude))
							} else {
								// this causes onMapFocus() to be called, which itself contains a call to
								// updateSearchQuery()
								mapView!!.animateCamera(latitude, longitude)
							}
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

	private fun onDownloadMapMenuItemClicked() {
		if(addLocationVM.isModuleInstalled(moduleName)) {
			// confirmed that uninstall happens on next manual restart of app
			// disable to avoid repetitive clicks, enabling again might not be necessary
			addLocationVM.uninstallModule(moduleName)
			showMessage(R.string.uninstall_note)
			requireActivity().invalidateOptionsMenu()
		} else {
			AlertDialog.Builder(requireContext())
				.setTitle(R.string.module_title_gmap)
				.setMessage(R.string.gmaps_download_message)
				.setNeutralButton(android.R.string.cancel, null)
				.setPositiveButton(R.string.download) { _, _ ->
					addLocationVM.installModule(moduleName)
					// skip showing message here cuz we got 'pending' state covering that part
				}
				.show()
		}
	}

	private fun updateSearchQuery(q: String) {
		if(q.isEmpty()) {
			searchMenuItem.collapseActionView()
		} else {
			searchMenuItem.expandActionView()
		}
		searchView.setQuery(q, false)
	}
}

interface MapViewListener {

	fun onMapFocus(lat: Double, lng: Double)
	fun onMapOutOfFocus()
}

interface MapView {

	fun animateCamera(lat: Double, lng: Double)
}