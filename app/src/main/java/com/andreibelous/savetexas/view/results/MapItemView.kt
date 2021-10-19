package com.andreibelous.savetexas.view.results

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.andreibelous.savetexas.MapPoint
import com.andreibelous.savetexas.R
import com.andreibelous.savetexas.toResId
import com.badoo.mvicore.modelWatcher
import com.google.android.gms.maps.model.LatLng

class MapItemView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        inflate(context, R.layout.results_view_phase_item, this)
        layoutParams =
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        background =
            RippleDrawable(
                ColorStateList.valueOf(Color.BLACK),
                null,
                ShapeDrawable(RectShape())
            )
    }

    private val image = findViewById<ImageView>(R.id.map_item_image)
    private val location = findViewById<TextView>(R.id.map_item_location)

    fun bind(point: MapPoint, clickAction: (LatLng) -> Unit) {
        setOnClickListener { clickAction.invoke(point.location) }
        modelWatcher(point)
    }

    @SuppressLint("SetTextI18n")
    private val modelWatcher = modelWatcher<MapPoint> {
        watch(MapPoint::type) { image.setImageResource(it.toResId()) }
        watch(MapPoint::location) { location.text = "lat ${it.latitude}\nlon ${it.longitude}" }
    }
}