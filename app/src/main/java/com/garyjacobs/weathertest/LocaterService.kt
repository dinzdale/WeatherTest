package com.garyjacobs.weathertest

import android.app.Service
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.*
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.LocationSource
import com.google.android.gms.tasks.OnSuccessListener
import model.CurrentWeather
import model.Forecast
import model.Mapquest.GeocodeData
import network.GetForecastData
import network.GetGeocodeInfo
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*


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
                                        lastLocation.getReverseGeocodeLocation()?.enqueue(object : retrofit2.Callback<GeocodeData> {
                                            override fun onFailure(call: Call<GeocodeData>?, t: Throwable?) {
                                            }

                                            override fun onResponse(call: Call<GeocodeData>?, response: Response<GeocodeData>?) {
                                                response?.body()?.let {
                                                    val addresses = it.toAddresses()
                                                    bundle.putParcelableArray("LOCATION", addresses)
                                                    FetchForecastServiceForcastData(REQUESTEDCURRENTLOCATION, bundle)
                                                }
                                            }
                                        })
                                    }
                        }
                        REQUESTLOCATION -> {
                            val requestedLocal = incomingMessage.data.getString("LOCATION")
                            requestedLocal?.let {
                                getGeocodeLocation(it)?.enqueue(object : retrofit2.Callback<GeocodeData> {
                                    override fun onFailure(call: Call<GeocodeData>?, t: Throwable?) {
                                    }

                                    override fun onResponse(call: Call<GeocodeData>?, response: Response<GeocodeData>?) {
                                        response?.body()?.let {
                                            val addresses = it.toAddresses()
                                            bundle.putParcelableArray("LOCATION", addresses)
                                            if (addresses.size == 1) {
                                                application.location = addresses[0]
                                                FetchForecastServiceForcastData(REQUESTEDLOCATION, bundle)
                                            } else {
                                                outboundmessenger.sendMessage(REQUESTEDLOCATION, bundle)
                                            }
                                        }
                                    }
                                })
                            }
                        }
                        REQUESTCURRENTWEATHERCURRENTLOCATION -> fusedLocationProviderClient
                                .lastLocation
                                .addOnSuccessListener {
                                    it.getReverseGeocodeLocation()!!.enqueue(object : retrofit2.Callback<GeocodeData> {
                                        override fun onResponse(call: Call<GeocodeData>?, response: Response<GeocodeData>?) {
                                            response?.body()?.let {
                                                if (response.isSuccessful) {
                                                    val addresses = it.toAddresses()
                                                    application.location = addresses[0]
                                                    bundle.putParcelableArray("LOCATION", addresses)
                                                    getCurrentWeather(addresses[0].latitude, addresses[0].latitude)?.execute()?.let {
                                                        if (it.isSuccessful) {
                                                            application.currentWeather = it.body()
                                                            outboundmessenger.sendMessage(REQUESTEDCURRENTWEATHERCURRENTLOCATION, bundle)
                                                        } else handleError(REQUESTEDCURRENTWEATHERCURRENTLOCATION, "Could not get current weather ERROR:${it.code()}")
                                                    }
                                                } else handleError(REQUESTEDCURRENTWEATHERCURRENTLOCATION, "Error trying to reverse geocode ERROR:${response.code()}")
                                            } ?: handleError(REQUESTEDCURRENTWEATHERCURRENTLOCATION)
                                        }
                                        override fun onFailure(call: Call<GeocodeData>?, t: Throwable?) {
                                            handleError(REQUESTEDCURRENTWEATHERCURRENTLOCATION, t?.message)
                                        }
                                    })
                                }

                        REQUESTCURRENTWEATHERWITHLOCATION -> {
                            incomingMessage.data.getString("LOCATION")?.let {
                                getGeocodeLocation(it)!!.enqueue(object : retrofit2.Callback<GeocodeData> {
                                    override fun onResponse(call: Call<GeocodeData>?, response: Response<GeocodeData>?) {
                                        response?.body()?.let {
                                            if (response.isSuccessful) {
                                                val addresses = it.toAddresses()
                                                bundle.putParcelableArray("LOCATION", addresses)
                                                if (addresses.size == 1) {
                                                    application.location = addresses[0]
                                                    getCurrentWeather(addresses[0].latitude, addresses[0].longitude).execute()?.let {
                                                        if (it.isSuccessful) {
                                                            application.currentWeather = it.body()
                                                            outboundmessenger.sendMessage(REQUESTEDCURRENTWEATHERWITHLOCATION, bundle)
                                                        }
                                                    }
                                                } else {
                                                    outboundmessenger.sendMessage(REQUESTCURRENTWEATHERWITHLOCATION, bundle)
                                                }
                                            }
                                        } ?: handleError(REQUESTEDCURRENTWEATHERCURRENTLOCATION, "Error")


                                    }

                                    override fun onFailure(call: Call<GeocodeData>?, t: Throwable?) {
                                        handleError(REQUESTCURRENTWEATHERCURRENTLOCATION, "Invalid Location Entered")
                                    }
                                })
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

    private fun handleError(messageID: Int, errorMessage: String? = "ERROR ENCOUNTERED") {
        val bundle = Bundle()
        bundle.putBoolean("STATUS", false)
        bundle.putString("ERROR", errorMessage)
        outboundmessenger.sendMessage(messageID, bundle)
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

    private fun getCurrentWeather(latitude: Double, longitude: Double): Call<CurrentWeather> {
        val res = application.resources
        val retrofit = Retrofit.Builder()
                .baseUrl(res.getString(R.string.openweathermap_base_url))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        val getForecastData = retrofit.create(GetForecastData::class.java)
        return getForecastData.getCurrrentWeatherByCoords(latitude, longitude, res.getString(R.string.openweathermap_appid))
    }


    private fun Messenger.sendMessage(what: Int, bundle: Bundle? = null, status: Boolean = true, outMessenger: Messenger = outboundmessenger, inMessenger: Messenger = inBoundMessenger) {
        val outboundmessage = Message.obtain()
        outboundmessage.what = what
        outboundmessage.replyTo = inMessenger
        bundle?.let {
            bundle.putBoolean("STATUS", status)
            outboundmessage.data = bundle
        }
        try {
            outMessenger.send(outboundmessage)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun getGeocodeLocation(location: String): Call<GeocodeData>? {
        val retrofit = Retrofit.Builder()
                .baseUrl(application.resources.getString(R.string.mapquest_geocode_base_url))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        val getGeoCodeInfo = retrofit.create(GetGeocodeInfo::class.java)
        return getGeoCodeInfo.getGeocodedAddress(location, application.resources.getString(R.string.mapquest_consumer_key))
    }

    private fun Location.coordsToString() = "${latitude},${longitude}"

    private fun Location.getReverseGeocodeLocation(): Call<GeocodeData>? {
        val retrofit = Retrofit.Builder()
                .baseUrl(application.resources.getString(R.string.mapquest_geocode_base_url))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        val getGeocodeInfo = retrofit.create(GetGeocodeInfo::class.java)
        return getGeocodeInfo.getReverseGeocodedAddress(coordsToString(), application.resources.getString(R.string.mapquest_consumer_key))
    }

    private fun <T> runInBackground(call: Call<T>, toucheTurtle: () -> Unit) {
        Thread(object : Runnable {
            override fun run() {
                call.execute()?.let { toucheTurtle }
            }
        }).start()
    }

}