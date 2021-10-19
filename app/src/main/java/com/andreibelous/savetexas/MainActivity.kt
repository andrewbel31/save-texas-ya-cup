package com.andreibelous.savetexas

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.andreibelous.savetexas.data.LocationDataSource
import com.andreibelous.savetexas.data.MapDataSource
import com.andreibelous.savetexas.data.OrientationDataSource
import com.andreibelous.savetexas.feature.MapFeature
import com.andreibelous.savetexas.mapper.NewsToViewAction
import com.andreibelous.savetexas.mapper.StateToViewModel
import com.andreibelous.savetexas.mapper.UiEventToWish
import com.andreibelous.savetexas.view.MainView
import com.badoo.binder.Binder
import com.badoo.binder.using
import com.badoo.mvicore.android.lifecycle.CreateDestroyBinderLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import io.reactivex.disposables.CompositeDisposable

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private val permissions =
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

    private var map: GoogleMap? = null
    private val mapFragment by lazy {
        supportFragmentManager.findFragmentById(R.id.map)?.cast<SupportMapFragment>()
    }

    private val disposables: CompositeDisposable = CompositeDisposable()
    private var mapFeature: MapFeature? = null

    private val locationDataSource by lazy { LocationDataSource(this).also { disposables += it } }

    private var dialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mapFragment?.getMapAsync(this)

        if (!hasPermissions(permissions)) {
            startRequesting()
        } else {
            // granted
        }
    }

    private fun startRequesting() {
        val shouldShowRationale =
            permissions.any { ActivityCompat.shouldShowRequestPermissionRationale(this, it) }

        if (shouldShowRationale) {
            showDialog()
            return
        }

        requestPermissions()
    }

    private fun showDialog() {
        dialog?.dismiss()
        AlertDialog.Builder(this)
            .setTitle("Нет необходимых разрешений")
            .setMessage("Без этих разрешений приложение не сможет работать :(")
            .setPositiveButton("дать разрешения") { _, _ ->
                dismissDialog()
                requestPermissions()
            }
            .setNegativeButton("отмена") { _, _ ->
                dismissDialog()
//                finish()
            }
            .setCancelable(true)
            .create()
            .also { dialog = it }
            .show()
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE)
    }

    private fun dismissDialog() {
        dialog?.dismiss()
        dialog = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val granted = mutableSetOf<String>()
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            permissions.forEachIndexed { index, permission ->
                if (grantResults[index] == PackageManager.PERMISSION_GRANTED) {
                    granted += permission
                }
            }
        }

        if (granted.containsAll(permissions.toList())) {
            // granted
        } else {
            showDialog()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        locationDataSource.onActivityResult(requestCode, resultCode, data)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        val orientationDataSource = OrientationDataSource(this) { locationDataSource.location }
        val mapDataSource = MapDataSource()

        disposables +=
            locationDataSource.locationUpdates
                .filter { it.isPresent }
                .firstElement()
                .subscribe(
                    { googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(it.get(), 10f)) },
                    {}
                )

        val view =
            MainView(
                root = this,
                googleMap = googleMap,
                getHeading = { orientationDataSource.heading },
                getLocation = { locationDataSource.location }
            )

        val feature =
            MapFeature(mapDataSource).also {
                disposables += it
                mapFeature = it
            }

        Binder(CreateDestroyBinderLifecycle(lifecycle)).apply {
            bind(view to feature using UiEventToWish)
            bind(feature to view using StateToViewModel)
            bind(feature.news to view::execute.asConsumer() using NewsToViewAction)
            bind(view to ::handleUiEvent.asConsumer())
        }
    }

    private fun handleUiEvent(event: MainView.Event) {
        when (event) {
            is MainView.Event.SendByEmailClicked -> share()
        }
    }

    private fun share() {
        val points =
            mapFeature?.state?.points ?: return

        val sb = StringBuilder()

        points.forEach {
            sb.append("Имя: ${it.type.str}, id: ${it.id}, местоположение: ${it.location}")
            sb.append("\n\n")
        }

        sendEmail(sb.toString())
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun sendEmail(text: String) {
        val uri = Uri.parse("mailto:")
            .buildUpon()
            .appendQueryParameter("subject", "Информация об объектах на карте")
            .appendQueryParameter("body", text)
            .build()

        val emailIntent = Intent(Intent.ACTION_SENDTO, uri)
        try {
            startActivity(Intent.createChooser(emailIntent, "Отправить e-mail"))
        } catch (anfe: ActivityNotFoundException) {

        }
    }

    private fun hasPermissions(permissions: Array<String>): Boolean =
        permissions.all {
            ActivityCompat
                .checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    override fun onDestroy() {
        dismissDialog()
        disposables.dispose()
        super.onDestroy()
    }

    private companion object {

        private const val PERMISSIONS_REQUEST_CODE = 101
    }
}
