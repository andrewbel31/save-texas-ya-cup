package com.andreibelous.savetexas.mapper

import com.andreibelous.savetexas.view.MainView
import com.andreibelous.savetexas.feature.MapFeature

object NewsToViewAction : (MapFeature.News) -> MainView.Action {

    override fun invoke(news: MapFeature.News): MainView.Action =
        when (news) {
            is MapFeature.News.ErrorHappened -> MainView.Action.HandleError(news.throwable)
            is MapFeature.News.ShowResults -> MainView.Action.ShowResults(news.points)
        }
}