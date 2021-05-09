package asceapps.weatheria.ui.adapter

import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import asceapps.weatheria.R
import asceapps.weatheria.data.model.WeatherInfo
import asceapps.weatheria.databinding.ItemWeatherInfoBinding

class PagerAdapter(
	private val onPosChanged: (pos: Int, info: WeatherInfo) -> Unit
): BaseAdapter<WeatherInfo, ItemWeatherInfoBinding>() {

	override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
		// since we want all listeners/observers live as long as the adapter is attached,
		// we decided to not bother unregister/remove them and let them die with it
		val dataObserver = object: RecyclerView.AdapterDataObserver() {
			override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
				if(itemCount == 1)
					recyclerView.scrollToPosition(positionStart)
			}
		}
		registerAdapterDataObserver(dataObserver)
		val pagerSnapHelper = PagerSnapHelper()
		val scrollListener = object: RecyclerView.OnScrollListener() {
			override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
				if(newState == RecyclerView.SCROLL_STATE_IDLE) {
					val lm = recyclerView.layoutManager ?: return
					val snapView = pagerSnapHelper.findSnapView(lm) ?: return
					val snapPos = lm.getPosition(snapView)
					onPosChanged(snapPos, getItem(snapPos))
				}
			}
		}
		pagerSnapHelper.attachToRecyclerView(recyclerView)
		recyclerView.addOnScrollListener(scrollListener)
	}

	override fun getItemViewType(position: Int) = R.layout.item_weather_info

	public override fun getItem(position: Int): WeatherInfo {
		return super.getItem(position)
	}
}