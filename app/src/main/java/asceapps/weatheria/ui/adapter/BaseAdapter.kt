package asceapps.weatheria.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import asceapps.weatheria.shared.data.base.Listable

abstract class BaseAdapter<T: Listable, B: ViewBinding>:
	ListAdapter<T, BaseAdapter.BindingHolder<T, B>>(HashCallback<T>()) {

	init {
		stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
	}

	final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingHolder<T, B> {
		val binding = createBinding(LayoutInflater.from(parent.context), parent)
		val holder = BindingHolder<T, B>(binding)
		onHolderCreated(holder)
		return holder
	}

	final override fun onBindViewHolder(holder: BindingHolder<T, B>, position: Int) {
		val item = getItem(position)
		holder.item = item
		onHolderBound(holder, item)
	}

	abstract fun createBinding(inflater: LayoutInflater, parent: ViewGroup): B
	open fun onHolderCreated(holder: BindingHolder<T, B>) {}
	open fun onHolderBound(holder: BindingHolder<T, B>, item: T) {}

	private class HashCallback<T: Listable>: DiffUtil.ItemCallback<T>() {

		override fun areItemsTheSame(oldT: T, newT: T) = oldT.id == newT.id
		override fun areContentsTheSame(oldT: T, newT: T) = oldT.hash == newT.hash
	}

	class BindingHolder<T: Listable, B: ViewBinding>(val binding: B): RecyclerView.ViewHolder(binding.root) {
		var item: T? = null
	}
}