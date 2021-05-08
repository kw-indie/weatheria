package asceapps.weatheria.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import asceapps.weatheria.data.model.WeatherInfo
import asceapps.weatheria.databinding.ItemWeatherInfoBinding

class PagerAdapter(
	private val onPosChanged: (pos: Int, info: WeatherInfo) -> Unit
): BaseAdapter<WeatherInfo, PagerAdapter.ViewHolder>() {

	private var recyclerView: RecyclerView? = null
	private val pagerSnapHelper = PagerSnapHelper()
	private val dataObserver = object: RecyclerView.AdapterDataObserver() {
		override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
			if(itemCount == 1)
				recyclerView?.scrollToPosition(positionStart)
		}
	}
	private val scrollListener = object: RecyclerView.OnScrollListener() {
		override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
			if(newState == RecyclerView.SCROLL_STATE_IDLE) {
				val lm = recyclerView.layoutManager ?: return
				val snapView = pagerSnapHelper.findSnapView(lm) ?: return
				val snapPos = lm.getPosition(snapView)
				onPosChanged(snapPos, getItem(snapPos))
			}
		}
	}

	override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
		this.recyclerView = recyclerView
		pagerSnapHelper.attachToRecyclerView(recyclerView)
		registerAdapterDataObserver(dataObserver)
		recyclerView.addOnScrollListener(scrollListener)
	}

	override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
		recyclerView.removeOnScrollListener(scrollListener)
		unregisterAdapterDataObserver(dataObserver)
		pagerSnapHelper.attachToRecyclerView(null)
		this.recyclerView = null
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		return ViewHolder(
			ItemWeatherInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
		)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		with(holder.binding) {
			info = getItem(position)
			executePendingBindings()
		}
	}

	public override fun getItem(position: Int): WeatherInfo {
		return super.getItem(position)
	}

	class ViewHolder(val binding: ItemWeatherInfoBinding): RecyclerView.ViewHolder(binding.root)
}