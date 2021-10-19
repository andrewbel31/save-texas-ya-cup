package com.andreibelous.savetexas

import android.app.Application
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class TexasApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Firebase.database.setPersistenceEnabled(true)
    }
}