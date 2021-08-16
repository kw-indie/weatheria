package asceapps.weatheria.ui.fragment

import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import asceapps.weatheria.R
import asceapps.weatheria.data.model.WeatherInfo
import asceapps.weatheria.data.repo.Error
import asceapps.weatheria.data.repo.Loading
import asceapps.weatheria.data.repo.Success
import asceapps.weatheria.databinding.FragmentMyLocationsBinding
import asceapps.weatheria.ui.adapter.MyLocationsAdapter
import asceapps.weatheria.ui.viewmodel.MainViewModel
import asceapps.weatheria.util.observe

class MyLocationsFragment: Fragment() {

	private val mainVM: MainViewModel by activityViewModels()
	private var emptyList = true

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		// turns out this can be called from here as well
		setHasOptionsMenu(true)

		val binding = FragmentMyLocationsBinding.inflate(inflater, container, false)

		val myLocationsAdapter = MyLocationsAdapter(object: MyLocationsAdapter.ItemCallback {
			override fun onDeleteClick(info: WeatherInfo) {
				mainVM.delete(info)
			}

			override fun onItemClick(pos: Int) {
				mainVM.selectedPos = pos
				findNavController().navigateUp()
			}

			override fun onReorder(info: WeatherInfo, toPos: Int) {
				mainVM.reorder(info, toPos)
			}
		})
		binding.locationList.apply {
			adapter = myLocationsAdapter
		}

		mainVM.weatherInfoList.observe(viewLifecycleOwner) {
			when(it) {
				is Loading -> {
					// todo show loading anim
				}
				is Success -> {
					// todo cancel loading anim
					val list = it.data
					myLocationsAdapter.submitList(list)
					val isEmpty = list.isEmpty()
					binding.tvNoLocations.isVisible = isEmpty
					binding.locationList.isVisible = !isEmpty
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
		inflater.inflate(R.menu.my_locations_menu, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when(item.itemId) {
			R.id.action_refresh_all -> mainVM.refreshAll().observe(viewLifecycleOwner) {
				when(it) {
					is Loading -> {
						// todo show loading anim
					}
					else -> {
						// todo cancel loading anim
						if(it is Error) {
							when(it.t) {
								// todo copy errors from home refresh
							}
						}
					}
				}
			}
			R.id.action_delete_all -> mainVM.deleteAll()
			// todo add refresh all
			else -> return super.onOptionsItemSelected(item)
		}
		return true
	}

	override fun onPrepareOptionsMenu(menu: Menu) {
		super.onPrepareOptionsMenu(menu)
		menu.setGroupEnabled(R.id.bulk_actions, !emptyList)
	}
}