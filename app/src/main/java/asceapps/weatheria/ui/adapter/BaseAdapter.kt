package asceapps.weatheria.ui.adapter

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import asceapps.weatheria.BR
import asceapps.weatheria.data.base.IDed

abstract class BaseAdapter<T: IDed>: ListAdapter<T, BaseAdapter.BindingHolder>(HashCallback<T>()) {

	init {
		stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
	}

	override fun onBindViewHolder(holder: BindingHolder, position: Int) {
		val item = getItem(position)
		with(holder.binding) {
			setVariable(BR.item, item)
			executePendingBindings()
		}
	}

	private class HashCallback<T: IDed>: DiffUtil.ItemCallback<T>() {

		override fun areItemsTheSame(oldT: T, newT: T) = oldT.id == newT.id
		override fun areContentsTheSame(oldT: T, newT: T) = oldT.hashCode() == newT.hashCode()
	}

	class BindingHolder(val binding: ViewDataBinding): RecyclerView.ViewHolder(binding.root)
}