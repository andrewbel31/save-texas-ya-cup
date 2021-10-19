package com.andreibelous.savetexas

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.content.res.AppCompatResources

class LocationOverlay
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    init {
        setWillNotDraw(false)
    }

    private val headingPaint =
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            color = Color.parseColor("#237BFF")
            colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        }

    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private val headingIcon: Bitmap =
        BitmapFactory.decodeResource(context.resources, R.drawable.map_location_default_view_angle)
    private val navigationIcon =
        AppCompatResources
            .getDrawable(context, R.drawable.map_location_default)!!
            .cast<Drawable>()
            .toBitmap()!!

    var headingProvider: (() -> Float?)? = null
    var pointProvider: (() -> Point?)? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val heading = headingProvider?.invoke()
        val point = pointProvider?.invoke()

        if (
            heading != null
            && point != null
            && point.x > 0
            && point.x < width
            && point.y > 0
            && point.y < height
        ) {
            val x = point.x.toFloat()
            val y = point.y.toFloat()
            canvas.save()
            canvas.rotate(heading - 180, x, y)
            canvas.drawBitmap(
                headingIcon,
                (x - headingIcon.width / 2),
                (y - headingIcon.height / 2),
                headingPaint
            )
            canvas.restore()

            canvas.drawBitmap(
                navigationIcon,
                (x - navigationIcon.width / 2),
                (y - navigationIcon.height / 2),
                bitmapPaint
            )
        }

        invalidate()
    }
}