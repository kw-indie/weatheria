package asceapps.weatheria.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.map
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import asceapps.weatheria.R
import asceapps.weatheria.databinding.FragmentSavedLocationsBinding
import asceapps.weatheria.ui.adapter.SavedLocationsAdapter
import asceapps.weatheria.ui.viewmodel.MainViewModel

class SavedLocationsFragment: Fragment() {

	private val mainVM: MainViewModel by activityViewModels()

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		return FragmentSavedLocationsBinding.inflate(inflater, container, false).apply {
			toolbar.apply {
				setNavigationOnClickListener {
					findNavController().navigateUp()
				}
				setOnMenuItemClickListener {item ->
					when(item.itemId) {
						R.id.action_delete_all -> mainVM.deleteAll()
						R.id.action_settings -> findNavController().navigate(R.id.action_open_settings)
						else -> return@setOnMenuItemClickListener false
					}
					true
				}
			}

			val touchHelper = ItemTouchHelper(
				object: ItemTouchHelper.SimpleCallback(
					ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
				) {
					override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
						super.onSelectedChanged(viewHolder, actionState)
						// called when item is being dragged
						if(actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
							viewHolder?.apply {
								val scale = 1.05f
								itemView.animate()
									.scaleX(scale)
									.scaleY(scale)
							}
						}
					}

					// called when item is dropped
					override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
						super.clearView(recyclerView, viewHolder)
						val scale = 1f
						viewHolder.itemView.animate()
							.scaleX(scale)
							.scaleY(scale)
					}

					override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
						target: RecyclerView.ViewHolder): Boolean {
						val a = recyclerView.adapter as SavedLocationsAdapter
						val location = a.getItem(viewHolder.bindingAdapterPosition)
						val toPos = target.bindingAdapterPosition
						mainVM.reorder(location, toPos)
						// todo i think returning true here will trigger 2 updates one direct, one from liveData update
						return false
					}

					override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
				}
			)
			val adapter = SavedLocationsAdapter(
				onDeleteClick = {
					mainVM.delete(it)
				},
				onItemClick = {
					// todo
				},
				onHandleTouch = {
					touchHelper.startDrag(it)
				}
			)
			touchHelper.attachToRecyclerView(rvLocations)
			rvLocations.apply {
				this.adapter = adapter
				val layoutManager = layoutManager as LinearLayoutManager
				val divider = DividerItemDecoration(context, layoutManager.orientation)
				addItemDecoration(divider)
				setHasFixedSize(true)
			}
			mainVM.weatherInfoList
				.map {list -> list.map {it.location}}
				.observe(viewLifecycleOwner) {
					adapter.submitList(it)
					if(it.isEmpty()) {
						tvNoLocations.visibility = View.VISIBLE
						rvLocations.visibility = View.GONE
					} else {
						tvNoLocations.visibility = View.GONE
						rvLocations.visibility = View.VISIBLE
					}
					toolbar.menu.findItem(R.id.action_delete_all).isEnabled = it.isNotEmpty()
				}
		}.root
	}
}