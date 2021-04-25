package asceapps.weatheria.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import asceapps.weatheria.R
import asceapps.weatheria.data.repo.Error
import asceapps.weatheria.data.repo.Loading
import asceapps.weatheria.data.repo.Success
import asceapps.weatheria.databinding.FragmentLocationsBinding
import asceapps.weatheria.ui.adapter.LocationsAdapter
import asceapps.weatheria.ui.viewmodel.MainViewModel
import asceapps.weatheria.util.observe

class LocationsFragment : Fragment() {

	private val mainVM: MainViewModel by activityViewModels()
	private var emptyList = true

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
	}

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {

		val binding = FragmentLocationsBinding.inflate(inflater, container, false)
		val locationsAdapter = LocationsAdapter(
			onDeleteClick = {
				mainVM.delete(it.location)
			},
			onItemClick = { _, pos ->
				mainVM.setSelectedLocation(pos)
				findNavController().navigateUp()
			},
			onStartDrag = {
				val scale = 1.05f
				it.animate()
					.scaleX(scale)
					.scaleY(scale)
			},
			onEndDrag = {
				val scale = 1f
				it.animate()
					.scaleX(scale)
					.scaleY(scale)
			},
			onReorder = { info, to ->
				mainVM.reorder(info.location, to)
			}
		)
		binding.rvLocations.apply {
			adapter = locationsAdapter
			val layoutManager = layoutManager as LinearLayoutManager
			val divider = DividerItemDecoration(context, layoutManager.orientation)
			addItemDecoration(divider)
			setHasFixedSize(true)
		}
		mainVM.weatherInfoList.observe(viewLifecycleOwner) {
			when(it) {
				is Loading -> {
					// todo show loading anim
				}
				is Success -> {
					// todo cancel loading anim
					val list = it.data
					locationsAdapter.submitList(list)
					val isEmpty = list.isEmpty()
					binding.tvNoLocations.isVisible = isEmpty
					binding.rvLocations.isVisible = !isEmpty
					if(emptyList != isEmpty) {
						emptyList = isEmpty
						requireActivity().invalidateOptionsMenu()
					}
				}
				is Error -> {
					// todo does this ever happen?
				}
			}
		}

		return binding.root
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.locations_menu, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when(item.itemId) {
			R.id.action_delete_all -> mainVM.deleteAll()
			// todo add refresh all
			else -> return super.onOptionsItemSelected(item)
		}
		return true
	}

	override fun onPrepareOptionsMenu(menu: Menu) {
		super.onPrepareOptionsMenu(menu)
		menu.findItem(R.id.action_delete_all).isEnabled = !emptyList
	}
}