package com.andreibelous.savetexas.mapper

import com.andreibelous.savetexas.feature.MapFeature
import com.andreibelous.savetexas.view.MainView

object UiEventToWish : (MainView.Event) -> MapFeature.Wish? {

    override fun invoke(event: MainView.Event): MapFeature.Wish? =
        when (event) {
            is MainView.Event.MapPointCreated -> MapFeature.Wish.SaveMapPoint(event.point)
            is MainView.Event.ShowResults -> MapFeature.Wish.ShowResults
            is MainView.Event.SendByEmailClicked -> null
        }
}