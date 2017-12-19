package com.garyjacobs.weathertest

import android.app.Service
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.*
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.LocationSource
import model.CurrentWeather
import model.Forecast
import network.GetForecastData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


//import retrofit.*

/**
 * Created by garyjacobs on 11/24/17.
 */
class LocaterService : Service() {

    companion object {
        val CONNECTING = -1
        val REQUESTCURRENTLOCATION = 3
        val REQUESTEDCURRENTLOCATION = -3
        val REQUESTLOCATION = 4
        val REQUESTEDLOCATION = -4
        val REQUESTCURRENTWEATHERCURRENTLOCATION = 5
        val REQUESTEDCURRENTWEATHERCURRENTLOCATION = -5
        val REQUESTCURRENTWEATHERWITHLOCATION = 6
        val REQUESTEDCURRENTWEATHERWITHLOCATION = -6
    }

    private lateinit var application: WeatherTestApplication
    private lateinit var googleAPIClient: GoogleApiClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var outboundmessenger: Messenger
    private var isBound = false
    private var apiConnected = false
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate() {
        super.onCreate()
        application = getApplication() as WeatherTestApplication
        initLocationServices()
    }

    private val inBoundMessenger = Messenger(InboundHandler())

    inner class InboundHandler : Handler() {
        override fun handleMessage(incomingMessage: Message?) {
            incomingMessage?.let {
                outboundmessenger = it.replyTo
                val bundle = Bundle()
                if (apiConnected) {
                    when (incomingMessage.what) {
                        REQUESTCURRENTLOCATION -> {
                            fusedLocationProviderClient
                                    .lastLocation
                                    .addOnSuccessListener { lastLocation ->
                                        lastLocation?.let {
                                            val addressList = Geocoder(this@LocaterService).getFromLocation(it.latitude, it.longitude, 1)
                                            application.location = addressList[0]
                                            bundle.putParcelableArray("LOCATION", addressList.toTypedArray())
                                            FetchForecastServiceForcastData(REQUESTEDCURRENTLOCATION, bundle)
                                        }
                                    }
                        }
                        REQUESTLOCATION -> {
                            val requestedLocal = incomingMessage.data.getString("LOCATION")
                            requestedLocal?.let {
                                val addressList = Geocoder(this@LocaterService).getFromLocationName(it, 5)
                                bundle.putParcelableArray("LOCATION", addressList.toTypedArray())
                                if (addressList.size == 1) {
                                    application.location = addressList[0]
                                    FetchForecastServiceForcastData(REQUESTEDLOCATION, bundle)
                                } else {
                                    outboundmessenger.sendMessage(REQUESTEDLOCATION, bundle)
                                }
                            }
                        }
                        REQUESTCURRENTWEATHERCURRENTLOCATION -> {
                            fusedLocationProviderClient
                                    .lastLocation
                                    .addOnSuccessListener { lastLocation ->
                                        lastLocation?.let({
                                            try {
                                                val addressList = Geocoder(this@LocaterService).getFromLocation(it.latitude, it.longitude, 1)
                                                application.location = addressList[0]
                                                bundle.putParcelableArray("LOCATION", addressList.toTypedArray())
                                                FetchCurrentWeather(REQUESTEDCURRENTWEATHERCURRENTLOCATION, bundle)
                                            } catch (ex: Exception) {
                                                bundle.putBoolean("STATUS", false)
                                                bundle.putString("ERROR", ex.message)
                                                outboundmessenger.sendMessage(REQUESTEDCURRENTWEATHERCURRENTLOCATION, bundle)
                                            }
                                        })
                                    }
                        }
                        REQUESTCURRENTWEATHERWITHLOCATION -> fusedLocationProviderClient
                                .lastLocation
                                .addOnSuccessListener { lastLocation ->
                                    lastLocation?.let {
                                        val requestedLocal = incomingMessage.data.getString("LOCATION")
                                        try {
                                            val addressList = Geocoder(this@LocaterService).getFromLocationName(requestedLocal, 5)
                                            bundle.putParcelableArray("LOCATION", addressList.toTypedArray())
                                            if (addressList.size == 1) {
                                                application.location = addressList[0]
                                                FetchCurrentWeather(REQUESTEDLOCATION, bundle)
                                            } else {
                                                outboundmessenger.sendMessage(REQUESTEDLOCATION, bundle)
                                            }
                                        } catch (ex: Exception) {
                                            bundle.putBoolean("STATUS",false)
                                            bundle.putString("ERROR",ex.message)
                                            outboundmessenger.sendMessage(REQUESTEDCURRENTWEATHERWITHLOCATION)
                                        }
                                    }
                                }
                        else -> {

                        }
                    }
                } else {
                    outboundmessenger.sendMessage(CONNECTING)
                }
            }
        }
    }

    override fun onBind(intent: Intent?) = inBoundMessenger.binder

    override fun onUnbind(intent: Intent?) = false


    private fun initLocationServices() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        googleAPIClient = GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(connectionCallBackListener)
                .addOnConnectionFailedListener(connectionFailedListener)
                .build()

        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval((10 * 1000).toLong())        // 10 seconds, in milliseconds
                .setFastestInterval((1 * 1000).toLong()) // 1 second, in milliseconds

        googleAPIClient.connect()

    }

    private val connectionCallBackListener = object : GoogleApiClient.ConnectionCallbacks {
        override fun onConnected(bundle: Bundle?) {
            apiConnected = true
        }

        override fun onConnectionSuspended(p0: Int) {
            apiConnected = false
        }
    }


    private val locationChangeListner = object : LocationSource.OnLocationChangedListener {
        override fun onLocationChanged(newLocation: Location?) {
            newLocation?.let {

            }
        }
    }
    private val connectionFailedListener = object : GoogleApiClient.OnConnectionFailedListener {
        override fun onConnectionFailed(connectionResult: ConnectionResult) {
        }
    }


    private fun FetchForecastServiceForcastData(what: Int, bundle: Bundle) {
        val addresses = bundle.get("LOCATION") as Array<Address>
        val res = application.resources
        val builder = Retrofit.Builder()
        builder.baseUrl(res.getString(R.string.openweathermap_base_url))
        builder.addConverterFactory(GsonConverterFactory.create())
        val retrofit = builder.build()
        val getForecastData = retrofit.create(GetForecastData::class.java)
        val call = getForecastData.getForecastByCoords(addresses[0].latitude, addresses[0].longitude, res.getString(R.string.openweathermap_appid))

        val response = call.enqueue(object : Callback<Forecast> {
            override fun onResponse(call: Call<Forecast>?, response: Response<Forecast>?) {
                response?.let {
                    application.forecast = it.body()
                    bundle.putBoolean("STATUS", true)
                    outboundmessenger.sendMessage(what, bundle)
                }
            }

            override fun onFailure(call: Call<Forecast>?, throwable: Throwable?) {
                throwable?.let {
                    bundle.putBoolean("STATUS", false)
                    bundle.putString("ERROR", it.message)
                    outboundmessenger.sendMessage(what, bundle)
                }
            }
        })

    }

    private fun FetchCurrentWeather(what: Int, bundle: Bundle) {
        val addresses = bundle.get("LOCATION") as Array<Address>
        val res = application.resources
        val builder = Retrofit.Builder()
        builder.baseUrl(res.getString(R.string.openweathermap_base_url))
        builder.addConverterFactory(GsonConverterFactory.create())
        val retrofit = builder.build()
        val getForecastData = retrofit.create(GetForecastData::class.java)
        val call = getForecastData.getCurrrentWeatherByCoords(addresses[0].latitude, addresses[0].longitude, res.getString(R.string.openweathermap_appid))
        call.enqueue(object : Callback<CurrentWeather> {
            override fun onFailure(call: Call<CurrentWeather>?, t: Throwable?) {
                bundle.putBoolean("STATUS", false)
                bundle.putString("ERROR", t!!.message)
                outboundmessenger.sendMessage(what, bundle)
            }

            override fun onResponse(call: Call<CurrentWeather>?, response: Response<CurrentWeather>?) {
                response?.let {
                    application.currentWeather = it.body()
                    bundle.putBoolean("STATUS", true)
                    outboundmessenger.sendMessage(what, bundle)
                }
            }
        })
    }


    private fun Messenger.sendMessage(what: Int, bundle: Bundle? = null, outMessenger: Messenger = outboundmessenger, inMessenger: Messenger = inBoundMessenger) {
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
}