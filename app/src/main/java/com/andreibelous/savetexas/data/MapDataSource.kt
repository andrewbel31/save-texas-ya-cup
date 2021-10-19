package com.andreibelous.savetexas.data

import com.andreibelous.savetexas.MapPoint
import com.andreibelous.savetexas.cast
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import io.reactivex.Completable
import io.reactivex.Observable

class MapDataSource {

    private val dataBase = Firebase.database

    val mapPointsUpdates: Observable<List<MapPoint>> =
        Observable.create { emitter ->
            dataBase.reference
                .addValueEventListener(
                    object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            try {

                                val points = mutableListOf<MapPoint>()
                                for (point in snapshot.children) {
                                    val id = point.child("id").value!!.cast<String>()

                                    val type =
                                        when (point.child("type").value!!.cast<String>()) {
                                            "TREE" -> MapPoint.Type.TREE
                                            "HYDRANT" -> MapPoint.Type.HYDRANT
                                            "STREETLIGHT" -> MapPoint.Type.STREETLIGHT
                                            "MAILBOX" -> MapPoint.Type.MAILBOX
                                            "POWER_PYLON" -> MapPoint.Type.POWER_PYLON
                                            else -> return
                                        }

                                    val location = point.child("location")
                                    val latitude = location.child("latitude").value!!.cast<Double>()
                                    val longitude =
                                        location.child("longitude").value!!.cast<Double>()

                                    points.add(MapPoint(id, type, LatLng(latitude, longitude)))
                                }

                                emitter.onNext(points)
                            } catch (e: Exception) {
                                emitter.onError(e)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            emitter.onError(error.toException())
                        }
                    }
                )
        }

    fun saveMapPoint(point: MapPoint): Completable =
        Completable.fromAction {
            val ref = dataBase.getReference(point.id)
            ref.setValue(point)
        }
}