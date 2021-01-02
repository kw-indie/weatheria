package asceapps.weatheria.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import asceapps.weatheria.R
import asceapps.weatheria.databinding.FragmentSearchBinding
import asceapps.weatheria.util.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchFragment: Fragment() {

	private val viewModel: SearchViewModel by viewModels()

	override fun onCreateView(inflater: LayoutInflater, root: ViewGroup?,
		savedInstanceState: Bundle?): View {
		return FragmentSearchBinding.inflate(inflater, root, false).apply {
			toolbar.setNavigationOnClickListener {
				findNavController().navigateUp()
			}
			searchLayout.setStartIconOnClickListener {
				if(!etSearch.text.isNullOrBlank())
					hideKeyboard(it)
			}
			etSearch.doAfterTextChanged {
				viewModel.query.value = it.toString()
			}

			val adapter = SearchResultAdapter {
				viewModel.addNewLocation(it)
				findNavController().navigateUp() // todo do we really do this?
			}
			rvResults.apply {
				val layoutManager = LinearLayoutManager(context)
				val dividers = DividerItemDecoration(context, layoutManager.orientation)
				this.adapter = adapter
				addItemDecoration(dividers)
			}

			viewModel.searchResult.observe(viewLifecycleOwner) {
				adapter.submitList(it)
				if(it.isEmpty()) {
					tvNoResult.visibility = View.VISIBLE
					rvResults.visibility = View.GONE
				} else {
					tvNoResult.visibility = View.GONE
					rvResults.visibility = View.VISIBLE
				}
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
		}.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val args: SearchFragmentArgs by navArgs()
		args.latLng?.apply {
			viewModel.query.value = "$latitude, $longitude"
		}
		arguments?.clear() // fixes stupid re-reading of the args
	}
}