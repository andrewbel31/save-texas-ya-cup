package com.andreibelous.savetexas.view.results

import android.content.Context
import android.graphics.Outline
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.LinearLayout
import com.andreibelous.savetexas.R
import com.andreibelous.savetexas.dp

class HeaderView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        inflate(context, R.layout.results_view_header_item, this)
        orientation = VERTICAL
        gravity = Gravity.CENTER
        layoutParams =
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
    }

    private val topImage = findViewById<View>(R.id.header_item_top_image).apply {
        this.clipToOutline = true
        this.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val radius = context.dp(4f)
                outline.setRoundRect(0, 0, view.width, view.height, radius)
            }
        }
    }
}