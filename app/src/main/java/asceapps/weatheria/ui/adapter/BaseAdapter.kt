package asceapps.weatheria.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import asceapps.weatheria.BR
import asceapps.weatheria.data.base.IDed

abstract class BaseAdapter<T: IDed, B: ViewDataBinding>:
	ListAdapter<T, BaseAdapter.BindingHolder<B>>(HashCallback<T>()) {

	init {
		stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingHolder<B> {
		val binding = DataBindingUtil.inflate<B>(
			LayoutInflater.from(parent.context), viewType, parent, false
		)
		val holder = BindingHolder(binding)
		onHolderCreated(holder)
		return holder
	}

	override fun onBindViewHolder(holder: BindingHolder<B>, position: Int) {
		val item = getItem(position)
		with(holder.binding) {
			setVariable(BR.item, item)
			//onBindHolder(holder, item)
			executePendingBindings()
		}
	}

	abstract override fun getItemViewType(position: Int): Int
	open fun onHolderCreated(holder: BindingHolder<B>) {}
	//open fun onBindHolder(holder: BindingHolder<B>, item: T) {}

	private class HashCallback<T: IDed>: DiffUtil.ItemCallback<T>() {

		override fun areItemsTheSame(oldT: T, newT: T) = oldT.id == newT.id
		override fun areContentsTheSame(oldT: T, newT: T) = oldT.hashCode() == newT.hashCode()
	}

	class BindingHolder<B: ViewDataBinding>(val binding: B): RecyclerView.ViewHolder(binding.root)
}