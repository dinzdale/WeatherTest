package com.garyjacobs.weathertest

import android.app.Service
import android.arch.lifecycle.ViewModelProviders
import android.arch.persistence.room.Room
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.*
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.LocationSource
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import model.ArchComps.WeatherDB
import model.CurrentWeather
import model.Forecast
import model.Mapquest.GeocodeData
import model.formattedDouble
import network.GetForecastData
import network.GetGeocodeInfo
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.function.Consumer


//import retrofit.*

/**
 * Created by garyjacobs on 11/24/17.
 */
class LocaterService : Service() {

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
    private lateinit var googleAPIClient: GoogleApiClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var outboundmessenger: Messenger
    private var isBound = false
    private var apiConnected = true
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var weatherDB: WeatherDB

    override fun onCreate() {
        super.onCreate()
        application = getApplication() as WeatherTestApplication
        initLocationServices()
        weatherDB = WeatherDB.getInstance(this)!!
        registerReceiver(broadcastReceiver, IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION))
    }

    private val inBoundMessenger = Messenger(InboundHandler())

    inner class InboundHandler : Handler() {
        override fun handleMessage(incomingMessage: Message?) {
            incomingMessage?.let {
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
                                                getForecast(addresses[0].latitude, addresses[0].longitude)
                                                        .subscribe { forecast, throwable ->
                                                            forecast?.let {
                                                                val bundle = Bundle()
                                                                bundle.putParcelableArray("LOCATION", addresses)
                                                                bundle.putDouble("lat", it.lattitude)
                                                                bundle.putDouble("lon", it.longitude)
                                                                bundle.putString("title", it.city.name)
                                                                outboundmessenger.sendMessage(REQUESTEDCURRENTLOCATION, bundle)
                                                            } ?: handleError(REQUESTEDCURRENTLOCATION, throwable.message)
                                                        }
                                            } ?: handleError(REQUESTEDCURRENTLOCATION, throwable.message)
                                        } ?: handleError(REQUESTEDCURRENTLOCATION, "Error getting current location")
                            }
                            REQUESTLOCATION -> incomingMessage.data.getString("LOCATION")?.let {
                                getGeocodeLocation(it)
                                        ?.subscribe { addresses, throwable ->
                                            addresses?.let {
                                                val bundle = Bundle()
                                                bundle.putParcelableArray("LOCATION", it)
                                                if (addresses.size == 1) {
                                                    getForecast(it[0].latitude, it[0].longitude)
                                                            .subscribe { forecast, throwable ->
                                                                forecast?.let {
                                                                    bundle.putDouble("lat", forecast.lattitude)
                                                                    bundle.putDouble("lon", forecast.longitude)
                                                                    bundle.putString("title", forecast.city.name)
                                                                    outboundmessenger.sendMessage(REQUESTEDLOCATION, bundle)
                                                                } ?: handleError(REQUESTEDLOCATION, throwable.message)
                                                            }
                                                } else {
                                                    outboundmessenger.sendMessage(REQUESTEDLOCATION, bundle)
                                                }
                                            } ?: handleError(REQUESTEDLOCATION, throwable.message)
                                        }
                            }

                            REQUESTCURRENTWEATHERCURRENTLOCATION -> {
                                if (networkCall) {
                                    fusedLocationProviderClient
                                            .lastLocation
                                            .addOnSuccessListener {
                                                it.getReverseGeocodeLocation()
                                                        ?.subscribe { addresses, throwable ->
                                                            addresses?.let {
                                                                getCurrentWeather(addresses[0].latitude, addresses[0].longitude)
                                                                        .subscribe { currentWeather, throwable ->
                                                                            currentWeather?.let {
                                                                                // test db
                                                                                //weatherDB.weatherDao().insertCurrentWeather(it)
                                                                                val bundle = Bundle()
                                                                                bundle.putDouble("lat", currentWeather.coord.lat.formattedDouble())
                                                                                bundle.putDouble("lon", currentWeather.coord.lon.formattedDouble())
                                                                                bundle.putString("title", currentWeather.name)
                                                                                outboundmessenger.sendMessage(REQUESTEDCURRENTWEATHERCURRENTLOCATION, bundle)

                                                                            } ?: handleError(REQUESTEDCURRENTWEATHERCURRENTLOCATION, throwable.message)
                                                                        }
                                                            } ?: handleError(REQUESTEDCURRENTWEATHERCURRENTLOCATION, throwable.message)
                                                        }
                                            }
                                } else {
                                    // should be in database already
                                    outboundmessenger.sendMessage(REQUESTEDCURRENTWEATHERCURRENTLOCATION, incomingMessage.data)
                                }
                            }
                            REQUESTCURRENTWEATHERWITHLOCATION -> incomingMessage.data.getString("LOCATION")?.let {
                                getGeocodeLocation(it)
                                        ?.subscribe { addresses, throwable ->
                                            addresses?.let {
                                                val bundle = Bundle()
                                                bundle.putParcelableArray("LOCATION", addresses)
                                                if (addresses.size == 1) {
                                                    getCurrentWeather(addresses[0].latitude, addresses[0].longitude)
                                                            .subscribe { currentWeather, _ ->
                                                                bundle.putDouble("lat", currentWeather.coord.lat.formattedDouble())
                                                                bundle.putDouble("lon", currentWeather.coord.lon.formattedDouble())
                                                                bundle.putString("title", currentWeather.name)
                                                                outboundmessenger.sendMessage(REQUESTEDCURRENTWEATHERWITHLOCATION, bundle)
                                                            }

                                                } else outboundmessenger.sendMessage(REQUESTEDCURRENTWEATHERWITHLOCATION, bundle)
                                            } ?: handleError(REQUESTEDCURRENTWEATHERWITHLOCATION, throwable.message)
                                        } ?: handleError(REQUESTEDCURRENTWEATHERWITHLOCATION, "Could not get location")
                            } ?: handleError(REQUESTEDCURRENTWEATHERWITHLOCATION, "Invalid address entered")
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
                .doOnNext {
                    it.lattitude = latitude
                    it.longitude = longitude
                    weatherDB.weatherDao().insertForecast(it)
                }
                .observeOn(AndroidSchedulers.mainThread())
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
                .getCurrrentWeatherByCoords(latitude, longitude, res.getString(R.string.openweathermap_appid))
                .subscribeOn(Schedulers.io())
                .doOnNext { weatherDB.weatherDao().insertCurrentWeather(it) }
                .observeOn(AndroidSchedulers.mainThread())
                .singleOrError()
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

    private fun getGeocodeLocation(location: String): Single<Array<Address>>? {
        return Retrofit.Builder()
                .baseUrl(application.resources.getString(R.string.mapquest_geocode_base_url))
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
                .create(GetGeocodeInfo::class.java)
                .getGeocodedAddress(location, application.resources.getString(R.string.mapquest_consumer_key))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .map { it.toAddresses() }
                .singleOrError()

    }

    private fun Location.coordsToString() = "${latitude.formattedDouble()},${longitude.formattedDouble()}"

    private fun Location.getReverseGeocodeLocation(): Single<Array<Address>>? {
        return Retrofit.Builder()
                .baseUrl(application.resources.getString(R.string.mapquest_geocode_base_url))
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
                .create(GetGeocodeInfo::class.java)
                .getReverseGeocodedAddress(coordsToString(), application.resources.getString(R.string.mapquest_consumer_key))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .map { it.toAddresses() }
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
                    val connMgr = context!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    connMgr.activeNetworkInfo?.let {} ?: outboundmessenger.sendMessage(NOINTERENT)
                }
            }
        }
    }


}