package com.garyjacobs.weathertest

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Address
import android.location.Location
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.*
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import model.ArchComps.WeatherDB
import model.CurrentWeather
import model.Forecast
import model.formattedDouble
import network.GetForecastData
import network.GetGeocodeInfo
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory


//import retrofit.*

/**
 * Created by garyjacobs on 11/24/17.
 */
class LocaterService : Service(), CoroutineScope by MainScope() {

    companion object {
        val NOINTERENT = -1
        val LOSTWIFI = -2
        val CONNECTING = 1
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

    //private lateinit var googleAPIClient: GoogleApiClient
    //private lateinit var locationRequest: LocationRequest
    private lateinit var outboundmessenger: Messenger
    private var isBound = false
    private var apiConnected = false
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var weatherDB: WeatherDB

    override fun onCreate() {
        super.onCreate()
        application = getApplication() as WeatherTestApplication

        weatherDB = WeatherDB.getInstance(this)!!
        registerReceiver(broadcastReceiver, IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION))
        launch {
            initLocationServices(this@LocaterService).collect { locationProvider->
                locationProvider?.also {
                    fusedLocationProviderClient = it
                    apiConnected = true
                } ?: Log.d(null,"Uh Oh...could not get location provider")
            }
        }
    }


    private val inBoundMessenger = Messenger(InboundHandler())

    inner class InboundHandler : Handler(Looper.getMainLooper()) {

        @SuppressLint("MissingPermission")
        override fun handleMessage(incomingMessage: Message) {
            incomingMessage.let {
                val lat = incomingMessage.data.getDouble("lat")
                val lon = incomingMessage.data.getDouble("lon")
                val networkCall = lat + lon == 0.0
                outboundmessenger = it.replyTo
                if (isNetworkedConnected()) {
                    if (apiConnected) {
                        when (incomingMessage.what) {
                            LOSTWIFI -> outboundmessenger.sendMessage(NOINTERENT)
                            REQUESTCURRENTLOCATION -> fusedLocationProviderClient.lastLocation.addOnSuccessListener {
                                it.getReverseGeocodeLocation()
                                    ?.subscribe { addresses, throwable ->
                                        addresses?.let {
                                            val currentLat = addresses[0].latitude
                                            val currentLon = addresses[0].longitude
                                            getForecast(
                                                addresses[0].latitude,
                                                addresses[0].longitude
                                            )
                                                .subscribe { forecast, throwable ->
                                                    forecast?.let {
                                                        val bundle = Bundle()
                                                        bundle.putParcelableArray(
                                                            "LOCATION",
                                                            addresses
                                                        )
                                                        bundle.putDouble("lat", currentLat)
                                                        bundle.putDouble("lon", currentLon)
                                                        outboundmessenger.sendMessage(
                                                            REQUESTEDCURRENTLOCATION,
                                                            bundle
                                                        )
                                                    }
                                                        ?: handleError(
                                                            REQUESTEDCURRENTLOCATION,
                                                            throwable.message
                                                        )
                                                }
                                        }
                                            ?: handleError(
                                                REQUESTEDCURRENTLOCATION,
                                                throwable.message
                                            )
                                    }
                                    ?: handleError(
                                        REQUESTEDCURRENTLOCATION,
                                        "Error getting current location"
                                    )
                            }
                            REQUESTLOCATION -> incomingMessage.data.getString("LOCATION")?.let {
                                getGeocodeLocation(it)
                                    ?.subscribe { addresses, throwable ->
                                        addresses?.let {
                                            val currentLat = addresses[0].latitude
                                            val currentLon = addresses[0].longitude
                                            val bundle = Bundle()
                                            bundle.putParcelableArray("LOCATION", it)
                                            if (addresses.size == 1) {
                                                getForecast(it[0].latitude, it[0].longitude)
                                                    .subscribe { forecast, throwable ->
                                                        forecast?.let {
                                                            bundle.putDouble("lat", currentLat)
                                                            bundle.putDouble("lon", currentLon)
                                                            outboundmessenger.sendMessage(
                                                                REQUESTEDLOCATION,
                                                                bundle
                                                            )
                                                        }
                                                            ?: handleError(
                                                                REQUESTEDLOCATION,
                                                                throwable.message
                                                            )
                                                    }
                                            } else {
                                                outboundmessenger.sendMessage(
                                                    REQUESTEDLOCATION,
                                                    bundle
                                                )
                                            }
                                        } ?: handleError(REQUESTEDLOCATION, throwable.message)
                                    }
                            }

                            REQUESTCURRENTWEATHERCURRENTLOCATION -> {
                                if (networkCall) {
                                    fusedLocationProviderClient
                                        .lastLocation
                                        .addOnFailureListener {
                                            handleError(
                                                REQUESTEDCURRENTWEATHERCURRENTLOCATION,
                                                it.message
                                            )
                                        }
                                        .addOnSuccessListener {
                                            it?.getReverseGeocodeLocation()
                                                ?.let {
                                                    it.subscribe { addresses, throwable ->
                                                        addresses?.let {
                                                            getCurrentWeather(
                                                                addresses[0].latitude,
                                                                addresses[0].longitude
                                                            )
                                                                .subscribe { currentWeather, throwable ->
                                                                    currentWeather?.let {
                                                                        val bundle = Bundle()
                                                                        bundle.putDouble(
                                                                            "lat",
                                                                            currentWeather.coord.lat.formattedDouble()
                                                                        )
                                                                        bundle.putDouble(
                                                                            "lon",
                                                                            currentWeather.coord.lon.formattedDouble()
                                                                        )
                                                                        bundle.putString(
                                                                            "title",
                                                                            currentWeather.name
                                                                        )
                                                                        outboundmessenger.sendMessage(
                                                                            REQUESTEDCURRENTWEATHERCURRENTLOCATION,
                                                                            bundle
                                                                        )

                                                                    }
                                                                        ?: handleError(
                                                                            REQUESTEDCURRENTWEATHERCURRENTLOCATION,
                                                                            throwable.message
                                                                        )
                                                                }
                                                        }
                                                            ?: handleError(
                                                                REQUESTEDCURRENTWEATHERCURRENTLOCATION,
                                                                throwable.message
                                                            )
                                                    }
                                                }
                                                ?: handleError(
                                                    REQUESTEDCURRENTWEATHERCURRENTLOCATION
                                                )
                                        }
                                } else {
                                    // should be in database already
                                    outboundmessenger.sendMessage(
                                        REQUESTEDCURRENTWEATHERCURRENTLOCATION,
                                        incomingMessage.data
                                    )
                                }
                            }
                            REQUESTCURRENTWEATHERWITHLOCATION -> incomingMessage.data.getString("LOCATION")
                                ?.let {
                                    getGeocodeLocation(it)
                                        ?.subscribe { addresses, throwable ->
                                            addresses?.let {
                                                val bundle = Bundle()
                                                bundle.putParcelableArray("LOCATION", addresses)
                                                if (addresses.size == 1) {
                                                    getCurrentWeather(
                                                        addresses[0].latitude,
                                                        addresses[0].longitude
                                                    )
                                                        .subscribe { currentWeather, _ ->
                                                            bundle.putDouble(
                                                                "lat",
                                                                currentWeather.coord.lat.formattedDouble()
                                                            )
                                                            bundle.putDouble(
                                                                "lon",
                                                                currentWeather.coord.lon.formattedDouble()
                                                            )
                                                            bundle.putString(
                                                                "title",
                                                                currentWeather.name
                                                            )
                                                            outboundmessenger.sendMessage(
                                                                REQUESTEDCURRENTWEATHERWITHLOCATION,
                                                                bundle
                                                            )
                                                        }

                                                } else outboundmessenger.sendMessage(
                                                    REQUESTEDCURRENTWEATHERWITHLOCATION,
                                                    bundle
                                                )
                                            }
                                                ?: handleError(
                                                    REQUESTEDCURRENTWEATHERWITHLOCATION,
                                                    throwable.message
                                                )
                                        }
                                        ?: handleError(
                                            REQUESTEDCURRENTWEATHERWITHLOCATION,
                                            "Could not get location"
                                        )
                                }
                                ?: handleError(
                                    REQUESTEDCURRENTWEATHERWITHLOCATION,
                                    "Invalid address entered"
                                )
                            else -> {
                            }
                        }
                    } else {
                        outboundmessenger.sendMessage(CONNECTING)
                    }
                } else {
                    outboundmessenger.sendMessage(NOINTERENT)
                }
            }
        }
    }

    private fun handleError(messageID: Int, errorMessage: String? = "ERROR ENCOUNTERED") {
        val bundle = Bundle()
        bundle.putString("ERROR", errorMessage)
        outboundmessenger.sendMessage(messageID, bundle, status = false)
    }

    override fun onBind(intent: Intent?) = inBoundMessenger.binder

    override fun onUnbind(intent: Intent?): Boolean {
        intent?.let {
            super.onUnbind(it)
        }
        unregisterReceiver(broadcastReceiver)
        return false
    }


    @SuppressLint("MissingPermission")
    private fun initLocationServices(context: Context): Flow<FusedLocationProviderClient?> =
        callbackFlow {
            val locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval((10 * 1000).toLong())        // 10 seconds, in milliseconds
                .setFastestInterval((1 * 1000).toLong()) // 1 second, in milliseconds

            val fusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(context)

            fusedLocationProviderClient.locationAvailability.apply {
                addOnSuccessListener {
                    if (it.isLocationAvailable) {
                        trySend(fusedLocationProviderClient)
                    }
                    else {
                        trySend(null)
                    }
                }
                addOnFailureListener {
                    trySend(null)
                }
            }


                awaitClose {
                }
            }


    private fun getForecast(latitude: Double, longitude: Double): Single<Forecast> {
        val res = application.resources
        return Retrofit.Builder()
            .baseUrl(res.getString(R.string.openweathermap_base_url))
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
            .create(GetForecastData::class.java)
            .getForecastByCoords(latitude, longitude, res.getString(R.string.openweathermap_appid))
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { application.forecast = it }
            .singleOrError()
    }

    fun getCurrentWeather(latitude: Double, longitude: Double): Single<CurrentWeather> {
        val res = application.resources
        return Retrofit.Builder()
            .baseUrl(res.getString(R.string.openweathermap_base_url))
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
            .create(GetForecastData::class.java)
            .getCurrrentWeatherByCoords(
                latitude,
                longitude,
                res.getString(R.string.openweathermap_appid)
            )
            .subscribeOn(Schedulers.io())
            //.doOnNext { application.currentWeather = it }
            .doOnNext { weatherDB.weatherDao().insertCurrentWeather(it) }
            .observeOn(AndroidSchedulers.mainThread())
            .singleOrError()
    }


    private fun Messenger.sendMessage(
        what: Int,
        bundle: Bundle? = null,
        status: Boolean = true,
        outMessenger: Messenger = outboundmessenger,
        inMessenger: Messenger = inBoundMessenger
    ) {
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

    private fun getGeocodeLocation(location: String): Single<Array<Address>>? {
        return Retrofit.Builder()
            .baseUrl(application.resources.getString(R.string.mapquest_geocode_base_url))
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
            .create(GetGeocodeInfo::class.java)
            .getGeocodedAddress(
                location,
                application.resources.getString(R.string.mapquest_consumer_key)
            )
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .map { it.toAddresses() }
            .singleOrError()

    }

    private fun Location.coordsToString() =
        "${latitude.formattedDouble()},${longitude.formattedDouble()}"

    private fun Location.getReverseGeocodeLocation(): Single<Array<Address>>? {
        return Retrofit.Builder()
            .baseUrl(application.resources.getString(R.string.mapquest_geocode_base_url))
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
            .create(GetGeocodeInfo::class.java)
            .getReverseGeocodedAddress(
                coordsToString(),
                application.resources.getString(R.string.mapquest_consumer_key)
            )
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .map { it.toAddresses() }
            // .doOnNext { application.location = it[0] }
            .singleOrError()
    }

    private fun isNetworkedConnected(): Boolean {
        val connMgr = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return connMgr.activeNetworkInfo?.isConnected ?: false
    }

    val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                if (it.action == WifiManager.WIFI_STATE_CHANGED_ACTION) {
                    val connMgr =
                        context!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    connMgr.activeNetworkInfo?.let {} ?: outboundmessenger.sendMessage(NOINTERENT)
                }
            }
        }
    }


}