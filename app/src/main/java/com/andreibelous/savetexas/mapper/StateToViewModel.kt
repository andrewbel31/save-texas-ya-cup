package com.andreibelous.savetexas.mapper

import com.andreibelous.savetexas.view.MainViewModel
import com.andreibelous.savetexas.feature.MapState

object StateToViewModel : (MapState) -> MainViewModel {

    override fun invoke(state: MapState): MainViewModel =
        MainViewModel(points = state.points.orEmpty())
}