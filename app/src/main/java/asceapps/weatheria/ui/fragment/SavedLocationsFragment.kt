package asceapps.weatheria.ui.fragment

import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import asceapps.weatheria.R
import asceapps.weatheria.databinding.FragmentSavedLocationsBinding
import asceapps.weatheria.ui.adapter.SavedLocationsAdapter
import asceapps.weatheria.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map

class SavedLocationsFragment : Fragment() {

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
		return FragmentSavedLocationsBinding.inflate(inflater, container, false).apply {
			val adapter = SavedLocationsAdapter(
				onDeleteClick = {
					mainVM.delete(it)
				},
				onItemClick = {
					// todo
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
				onReorder = { location, to ->
					mainVM.reorder(location, to)
				}
			)
			rvLocations.apply {
				this.adapter = adapter
				val layoutManager = layoutManager as LinearLayoutManager
				val divider = DividerItemDecoration(context, layoutManager.orientation)
				addItemDecoration(divider)
				setHasFixedSize(true)
			}
			viewLifecycleOwner.lifecycleScope.launchWhenResumed {
				mainVM.weatherInfoList
					.map { list -> list.map { it.location } }
					.collect {
						adapter.submitList(it)
						val isEmpty = it.isEmpty()
						tvNoLocations.isVisible = isEmpty
						rvLocations.isVisible = !isEmpty
						if (emptyList != isEmpty) {
							emptyList = isEmpty
							requireActivity().invalidateOptionsMenu()
						}
					}
			}
		}.root
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.saved_locations_menu, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.action_delete_all -> mainVM.deleteAll()
			else -> return super.onOptionsItemSelected(item)
		}
		return true
	}

	override fun onPrepareOptionsMenu(menu: Menu) {
		super.onPrepareOptionsMenu(menu)
		menu.findItem(R.id.action_delete_all).isEnabled = !emptyList
	}
}