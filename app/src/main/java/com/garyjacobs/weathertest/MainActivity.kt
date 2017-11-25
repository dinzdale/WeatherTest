package com.garyjacobs.weathertest

import android.Manifest
import android.app.FragmentManager
import android.app.FragmentTransaction
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.squareup.otto.Subscribe

import java.io.IOException

import Events.ForecastListSelected
import android.content.Context
import android.widget.ProgressBar
import widgets.ComboBox

class MainActivity : WeatherActivity(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private var mGoogleAPIClient: GoogleApiClient? = null
    private var locationRequest: LocationRequest? = null
    lateinit var weatherListContainer: FrameLayout
    lateinit var weatherDetailsContainer: FrameLayout
    lateinit var progress_bar: ProgressBar
    lateinit var cityForecastCB: ComboBox
    var twoPane: Boolean = false
    internal var isBound = false
    lateinit var iBinder: IBinder
    lateinit var outBoundMessenger: Messenger
    lateinit var inBoundMessenger: Messenger
    lateinit var outLocaterMessenger : Messenger
    lateinit var inLocaterMessenter: Messenger

    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            isBound = true
            outBoundMessenger = Messenger(service)
            inBoundMessenger = Messenger(IncomingHandler())
            cityForecastCB.visibility = View.VISIBLE
            outBoundMessenger.sendMessage(FetchForecastService.REGISTER_CLIENT)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false
        }
    }

    private val locaterServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            outLocaterMessenger
        }
    }
    override fun onStart() {
        super.onStart()
        // check if user has granted location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), 100)
        } else {
            mGoogleAPIClient!!.connect()
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mGoogleAPIClient!!.connect()
        }
    }

    override fun onStop() {
        super.onStop()
        //mGoogleAPIClient!!.disconnect()
    }

    override fun onResume() {
        super.onResume()
        weatherApplication.getBus().register(this)
    }

    override fun onPause() {
        super.onPause()
        weatherApplication.getBus().unregister(this)
    }

    @Subscribe
    fun OnItemSelected(forecastListSelected: ForecastListSelected) {
        if (twoPane) {
            LoadForecastDetailsFragment(forecastListSelected.selectedItem, R.id.weather_details_container, false)
        } else {
            LoadForecastDetailsFragment(forecastListSelected.selectedItem, R.id.weather_list_container, true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mGoogleAPIClient = GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build()

        // Create the LocationRequest object
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval((10 * 1000).toLong())        // 10 seconds, in milliseconds
                .setFastestInterval((1 * 1000).toLong()) // 1 second, in milliseconds
        progress_bar = findViewById(R.id.progress_bar) as ProgressBar
        val frameLayout = findViewById(R.id.main_frame_layout) as FrameLayout
        cityForecastCB = findViewById(R.id.location_cb) as ComboBox
        cityForecastCB?.setClientClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                if (isBound) {
                    cityForecastCB.getCurrentText()?.let {
                        val bundle = Bundle()
                        bundle.putString("FETCH_CITY_FORECAST", it)
                        outBoundMessenger.sendMessage(FetchForecastService.FETCH_CITY_FORECAST, bundle)
                    }
                }
            }
        })
        val v = layoutInflater.inflate(R.layout.weather_panel_layout, null) as View
        frameLayout.addView(v)
        twoPane = v.findViewById(R.id.weather_details_container) != null
    }


    inner class IncomingHandler : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                FetchForecastService.CLIENT_REGISTERED -> {
                    cityForecastCB.getCurrentText()?.let {
                        val bundle = Bundle()
                        bundle.putString("FETCH_CITY_FORECAST", it)
                        outBoundMessenger.sendMessage(FetchForecastService.FETCH_CITY_FORECAST, bundle)
                    }
                }
                FetchForecastService.CITY_FORECAST_FETCHED -> {
                    progress_bar.visibility = View.INVISIBLE

                    LoadWeatherListFragment()
                    if (twoPane) {
                        LoadForecastDetailsFragment(0, R.id.weather_details_container, false)
                    }
                    supportActionBar!!.title = weatherApplication.getForecast().city.name
                }
            }
        }
    }


    fun Messenger.sendMessage(what: Int, bundle: Bundle? = null, outMessenger: Messenger = outBoundMessenger, inMessenger: Messenger = inBoundMessenger) {
        val outboundmessage = Message.obtain()
        outboundmessage.what = what
        outboundmessage.replyTo = inMessenger
        bundle?.let { outboundmessage.data = bundle }
        try {
            outMessenger.send(outboundmessage)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun LoadWeatherListFragment() {
        val fm = fragmentManager
        fm.beginTransaction().replace(R.id.weather_list_container, WeatherListFragment()).commit()
    }

    private fun LoadForecastDetailsFragment(position: Int, containerID: Int, incCount: Boolean) {
        val fm = fragmentManager
        val ft = fm.beginTransaction()
        ft.replace(containerID, ForecastDetailsFragment.newInstance(position))
        if (incCount) {
            ft.addToBackStack("WeatherDetails")
        }
        ft.commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        val id = item.itemId


        return if (id == R.id.action_settings) {
            true
        } else super.onOptionsItemSelected(item)

    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(mServiceConnection)
    }

    override fun onBackPressed() {
        val fm = fragmentManager
        val fragCount = fm.backStackEntryCount
        if (fragCount > 0) {
            fm.popBackStack()
        } else {
            super.onBackPressed()
        }

    }

    override fun onConnected(connectionHint: Bundle?) {

        val lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleAPIClient)
        if (lastLocation != null) {
            handleNewLocation(lastLocation)
        } else {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleAPIClient, locationRequest, this)
        }

    }

    override fun onConnectionSuspended(i: Int) {
        Toast.makeText(this, "onConnectionSuspended val: $i", Toast.LENGTH_LONG)
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Toast.makeText(this, "onConnectionSuspended val: ${connectionResult.toString()}", Toast.LENGTH_LONG)
    }

    override fun onLocationChanged(location: Location) {
        handleNewLocation(location)
    }

    private fun handleNewLocation(newLocation: Location) {
        val geocoder = Geocoder(this)
        try {
            val addressList = geocoder.getFromLocation(newLocation.latitude, newLocation.longitude, 1)
            val address = addressList[0]
            cityForecastCB!!.setText(address.postalCode)
            if (!isBound) {
                // start ForcastFetcher service
                val intent = Intent(this, FetchForecastService::class.java)
                bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)
            }
        } catch (ioe: IOException) {
            Toast.makeText(this, "Error obtaining zipcode", Toast.LENGTH_LONG)
        }

    }

    companion object {

        var FORECASTDATA_FETCHED = 1
    }
}

