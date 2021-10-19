package com.andreibelous.savetexas.view

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.andreibelous.savetexas.*
import com.andreibelous.savetexas.view.MainView.Event
import com.andreibelous.savetexas.view.results.ResultsView
import com.andreibelous.savetexas.view.results.ResultsViewModel
import com.badoo.mvicore.modelWatcher
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.ObservableSource
import io.reactivex.functions.Consumer
import java.util.*

class MainView(
    private val root: AppCompatActivity,
    private val googleMap: GoogleMap,
    private var getHeading: () -> Float?,
    private val getLocation: () -> LatLng?,
    private val events: PublishRelay<Event> = PublishRelay.create()
) : Consumer<MainViewModel>, ObservableSource<Event> by events {

    sealed interface Event {

        data class MapPointCreated(val point: MapPoint) : Event
        object ShowResults : Event
        object SendByEmailClicked : Event
    }

    private val overlayView = root.findViewById<LocationOverlay>(R.id.location_overlay).apply {
        headingProvider = getHeading
        pointProvider = {
            val location = getLocation.invoke()
            if (location != null) {
                googleMap.projection.toScreenLocation(location)
            } else {
                null
            }
        }
    }

    private var dialog: AlertDialog? = null

    private val resultsView = root.findViewById<ResultsView>(R.id.results_view)
    private val dimOverlay =
        root.findViewById<View>(R.id.dim_overlay)
            .apply {
                alpha = 0.0f
                gone()
            }
    private val behaviour = BottomSheetBehavior.from(resultsView).apply {
        addBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState) {
                        BottomSheetBehavior.STATE_COLLAPSED -> dimOverlay.gone()
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    val progress = maxOf(slideOffset, 0f)
                    dimOverlay.alpha = progress
                }
            }
        )
    }

    private val buttonResults = root.findViewById<TextView>(R.id.label_results).apply {
        val radii = context.dp(24f)
        val stroke = context.dp(2f)
        val radiiArr = floatArrayOf(radii, radii, radii, radii, radii, radii, radii, radii)
        background =
            RippleDrawable(
                ColorStateList.valueOf(Color.WHITE),
                GradientDrawable().apply {
                    color = ColorStateList.valueOf(Color.WHITE)
                    setStroke(stroke.toInt(), Color.BLACK)
                    cornerRadii = radiiArr
                },
                ShapeDrawable(RoundRectShape(radiiArr, null, null))
            )

        setOnClickListener { events.accept(Event.ShowResults) }
    }

    init {
        root.lifecycle.subscribe {
            dismissDialog()
        }
        googleMap.setOnMapLongClickListener { latLng ->
            overlayView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            showAddObjectDialog(latLng)
        }

        dimOverlay.setOnClickListener { behaviour.state = BottomSheetBehavior.STATE_COLLAPSED }
    }

    private fun dismissDialog() {
        dialog?.dismiss()
        dialog = null
    }

    private fun showAddObjectDialog(latLng: LatLng) {
        dismissDialog()
        val values = MapPoint.Type.values()
        val names = values.map { it.str }.toTypedArray()

        var selected = 0

        AlertDialog.Builder(root)
            .setTitle("Добавить объект")
            .setSingleChoiceItems(names, selected) { _, item -> selected = item }
            .setPositiveButton("OK") { _, _ ->
                events.accept(
                    Event.MapPointCreated(
                        point = MapPoint(
                            id = UUID.randomUUID().toString(),
                            type = values[selected],
                            location = latLng
                        )
                    )
                )
                dismissDialog()
            }
            .setNegativeButton("отмена") { _, _ ->
                dismissDialog()
            }
            .setCancelable(true)
            .create()
            .also { dialog = it }
            .show()
    }

    override fun accept(vm: MainViewModel) {
        modelWatcher(vm)
    }

    private val modelWatcher = modelWatcher<MainViewModel> {
        watch(MainViewModel::points) { points ->
            googleMap.clear()
            points.forEach {
                googleMap.addMarker(
                    MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromResource(it.type.toResId()))
                        .title(it.type.str)
                        .position(it.location)
                        .visible(true)
                )
            }
        }
    }

    fun execute(action: Action) {
        when (action) {
            is Action.HandleError -> {
                Toast.makeText(
                    root,
                    "Error = ${action.throwable}",
                    Toast.LENGTH_LONG
                ).show()
            }
            is Action.ShowResults -> {
                resultsView.bind(
                    ResultsViewModel(
                        data = action.points.orEmpty(),
                        shareClickAction = { events.accept(Event.SendByEmailClicked) },
                        locationClickAction = {
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 14f))
                            behaviour.state = BottomSheetBehavior.STATE_COLLAPSED
                        }
                    )
                )
                showResults()
            }
        }
    }

    private fun showResults() {
        dimOverlay.visible()
        behaviour.state = BottomSheetBehavior.STATE_EXPANDED
    }

    sealed interface Action {

        data class HandleError(val throwable: Throwable) : Action
        data class ShowResults(val points: List<MapPoint>?) : Action
    }
}

data class MainViewModel(
    val points: List<MapPoint>
)