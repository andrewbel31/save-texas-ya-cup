package com.andreibelous.savetexas.view.results

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.andreibelous.savetexas.MapPoint
import com.andreibelous.savetexas.cast
import com.andreibelous.savetexas.dp
import com.google.android.gms.maps.model.LatLng

class ResultsView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    private val itemsAdapter = Adapter(listOf())

    init {
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val cornerRadius = context.dp(24f)
                outline.setRoundRect(
                    0,
                    0,
                    view.width,
                    (view.height + cornerRadius).toInt(),
                    cornerRadius
                )
            }
        }
        setBackgroundColor(Color.WHITE)
        adapter = itemsAdapter
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }

    fun bind(model: ResultsViewModel) {
        itemsAdapter.setItems(model.toItems())
    }

    private fun ResultsViewModel.toItems(): List<ResultListItem> {
        val items = mutableListOf<ResultListItem>()

        if (data.isNotEmpty()) {
            items.add(ResultListItem.HeaderItem)
            items.add(ResultListItem.ShareItem(shareClickAction))
            data.forEach { items.add(ResultListItem.MapItem(it, locationClickAction)) }
            items.add(ResultListItem.ShareItem(shareClickAction))
        } else {
            items.add(ResultListItem.EmptyItem)
        }

        return items
    }
}

sealed interface ResultListItem {

    object HeaderItem : ResultListItem

    data class MapItem(
        val point: MapPoint,
        val clickAction: (LatLng) -> Unit
    ) : ResultListItem

    data class ShareItem(val clickAction: () -> Unit) : ResultListItem

    object EmptyItem : ResultListItem
}

private class Adapter(
    private var items: List<ResultListItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    @SuppressLint("NotifyDataSetChanged")
    fun setItems(items: List<ResultListItem>) {
        this.items = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            HEADER_VIEW_TYPE -> HeaderViewHolder(HeaderView(parent.context))
            MAP_ITEM_VIEW_TYPE -> MapItemViewHolder(MapItemView(parent.context))
            SHARE_VIEW_TYPE -> ShareViewHolder(ShareView(parent.context))
            EMPTY_VIEW_TYPE -> EmptyViewHolder(EmptyView(parent.context))
            else -> throw Exception("unsupported view type $viewType")
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ResultListItem.HeaderItem -> Unit
            is ResultListItem.MapItem -> holder.cast<MapItemViewHolder>().bind(item)
            is ResultListItem.ShareItem -> holder.cast<ShareViewHolder>().bind(item)
            is ResultListItem.EmptyItem -> Unit
        }
    }

    override fun getItemViewType(position: Int): Int = items[position].toViewType()

    override fun getItemCount(): Int = items.size

    private fun ResultListItem.toViewType() =
        when (this) {
            is ResultListItem.HeaderItem -> HEADER_VIEW_TYPE
            is ResultListItem.MapItem -> MAP_ITEM_VIEW_TYPE
            is ResultListItem.ShareItem -> SHARE_VIEW_TYPE
            is ResultListItem.EmptyItem -> EMPTY_VIEW_TYPE
        }

    private class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    private class MapItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(model: ResultListItem.MapItem) {
            itemView.cast<MapItemView>().bind(model.point, model.clickAction)
        }
    }

    private class ShareViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(model: ResultListItem.ShareItem) {
            itemView.cast<ShareView>().bind { model.clickAction() }
        }
    }

    private class EmptyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private companion object {

        private const val HEADER_VIEW_TYPE = 1
        private const val MAP_ITEM_VIEW_TYPE = 2
        private const val SHARE_VIEW_TYPE = 3
        private const val EMPTY_VIEW_TYPE = 4
    }
}


data class ResultsViewModel(
    val data: List<MapPoint>,
    val shareClickAction: () -> Unit,
    val locationClickAction: (LatLng) -> Unit
)