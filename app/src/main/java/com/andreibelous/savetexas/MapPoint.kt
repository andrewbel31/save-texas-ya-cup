package com.andreibelous.savetexas

import androidx.annotation.DrawableRes
import com.google.android.gms.maps.model.LatLng

class MapPoint(
    val id: String,
    val type: Type,
    val location: LatLng
) {

    enum class Type(val str: String) {
        POWER_PYLON("столб ЛЭП"),
        STREETLIGHT("уличный фонарь"),
        TREE("дерево"),
        MAILBOX("почтовый ящик"),
        HYDRANT("пожарный гидрант")
    }
}

@DrawableRes
fun MapPoint.Type.toResId() =
    when (this) {
        MapPoint.Type.POWER_PYLON -> R.drawable.energy
        MapPoint.Type.STREETLIGHT -> R.drawable.light
        MapPoint.Type.TREE -> R.drawable.tree
        MapPoint.Type.MAILBOX -> R.drawable.mail
        MapPoint.Type.HYDRANT -> R.drawable.hydrant
    }