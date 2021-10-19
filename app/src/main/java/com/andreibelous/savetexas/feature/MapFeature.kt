package com.andreibelous.savetexas.feature

import com.andreibelous.savetexas.MapPoint
import com.andreibelous.savetexas.data.MapDataSource
import com.andreibelous.savetexas.feature.MapFeature.Wish
import com.andreibelous.savetexas.toObservable
import com.badoo.mvicore.element.Actor
import com.badoo.mvicore.element.Bootstrapper
import com.badoo.mvicore.element.NewsPublisher
import com.badoo.mvicore.element.Reducer
import com.badoo.mvicore.feature.BaseFeature
import com.badoo.mvicore.feature.Feature
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers

class MapFeature(
    private val mapDataSource: MapDataSource,
) : Feature<Wish, MapState, MapFeature.News> by BaseFeature(
    initialState = MapState(),
    bootstrapper = BootstrapperImpl(mapDataSource),
    wishToAction = Action::ExecuteWish,
    actor = ActorImpl(mapDataSource),
    reducer = ReducerImpl(),
    newsPublisher = NewsPublisherImpl()
) {

    sealed interface Wish {

        data class SaveMapPoint(val point: MapPoint) : Wish
        object ShowResults : Wish
    }

    sealed interface News {

        data class ErrorHappened(val throwable: Throwable) : News
        data class ShowResults(val points: List<MapPoint>?) : News
    }

    private sealed interface Action {

        data class ExecuteWish(val wish: Wish) : Action
        data class HandlePointsUpdated(val points: List<MapPoint>) : Action
    }

    private sealed interface Effect {

        data class MapPointsUpdated(val points: List<MapPoint>) : Effect
        data class ErrorHappened(val throwable: Throwable) : Effect
        data class ResultsLoaded(val points: List<MapPoint>?) : Effect
    }

    private class ActorImpl(
        private val mapDataSource: MapDataSource,
    ) : Actor<MapState, Action, Effect> {

        override fun invoke(state: MapState, action: Action): Observable<out Effect> =
            when (action) {
                is Action.HandlePointsUpdated -> Effect.MapPointsUpdated(action.points)
                    .toObservable<Effect>()
                is Action.ExecuteWish -> executeWish(state, action.wish)
            }.onErrorReturn { Effect.ErrorHappened(it) }
                .observeOn(AndroidSchedulers.mainThread())

        private fun executeWish(state: MapState, wish: Wish): Observable<Effect> =
            when (wish) {
                is Wish.SaveMapPoint ->
                    mapDataSource
                        .saveMapPoint(wish.point)
                        .andThen(Observable.empty())
                is Wish.ShowResults -> Effect.ResultsLoaded(state.points).toObservable<Effect>()
            }.observeOn(AndroidSchedulers.mainThread())
    }

    private class ReducerImpl() : Reducer<MapState, Effect> {

        override fun invoke(state: MapState, effect: Effect): MapState =
            when (effect) {
                is Effect.MapPointsUpdated -> state.copy(points = effect.points)
                is Effect.ErrorHappened,
                is Effect.ResultsLoaded -> state
            }
    }

    private class BootstrapperImpl(
        private val mapDataSource: MapDataSource
    ) : Bootstrapper<Action> {

        override fun invoke(): Observable<Action> =
            mapDataSource
                .mapPointsUpdates
                .map { Action.HandlePointsUpdated(it) }
    }

    private class NewsPublisherImpl : NewsPublisher<Action, Effect, MapState, News> {

        override fun invoke(action: Action, effect: Effect, state: MapState): News? =
            when (effect) {
                is Effect.ErrorHappened -> News.ErrorHappened(effect.throwable)
                is Effect.ResultsLoaded -> News.ShowResults(effect.points)
                else -> null
            }
    }
}