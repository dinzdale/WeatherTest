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
import model.CurrentWeather
import model.Forecast
import model.Mapquest.GeocodeData
import network.GetForecastData
import network.GetGeocodeInfo
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.function.Consumer


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
    private var apiConnected = true
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
                if (apiConnected) {
                    when (incomingMessage.what) {
                        REQUESTCURRENTLOCATION -> fusedLocationProviderClient.lastLocation.addOnSuccessListener {
                            it.getReverseGeocodeLocation()
                                    ?.subscribe { addresses, throwable ->
                                        addresses?.let {
                                            getForecast(addresses[0].latitude, addresses[0].longitude)
                                                    ?.subscribe { forecast, throwable ->
                                                        forecast?.let {
                                                            val bundle = Bundle()
                                                            bundle.putParcelableArray("LOCATION", addresses)
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
                                                                outboundmessenger.sendMessage(REQUESTEDLOCATION, bundle)
                                                            } ?: handleError(REQUESTEDLOCATION, throwable.message)
                                                        }
                                            } else {
                                                outboundmessenger.sendMessage(REQUESTEDLOCATION, bundle)
                                            }
                                        } ?: handleError(REQUESTEDLOCATION, throwable.message)
                                    }
                        }

                        REQUESTCURRENTWEATHERCURRENTLOCATION -> fusedLocationProviderClient
                                .lastLocation
                                .addOnSuccessListener {
                                    it.getReverseGeocodeLocation()
                                            ?.subscribe { addresses, throwable ->
                                                addresses?.let {
                                                    getCurrentWeather(addresses[0].latitude.toFloat(), addresses[0].longitude.toFloat())
                                                            .subscribe { currentWeather, throwable ->
                                                                currentWeather?.let {
                                                                    val bundle = Bundle()
                                                                    bundle.putParcelableArray("LOCATION", addresses)
                                                                    outboundmessenger.sendMessage(REQUESTEDCURRENTWEATHERCURRENTLOCATION, bundle)
                                                                } ?: handleError(REQUESTEDCURRENTWEATHERCURRENTLOCATION, throwable.message)
                                                            }
                                                } ?: handleError(REQUESTEDCURRENTWEATHERCURRENTLOCATION, throwable.message)
                                            }
                                }

                        REQUESTCURRENTWEATHERWITHLOCATION -> incomingMessage.data.getString("LOCATION")?.let {
                            getGeocodeLocation(it)
                                    ?.subscribe { addresses, throwable ->
                                        addresses?.let {
                                            val bundle = Bundle()
                                            bundle.putParcelableArray("LOCATION", addresses)
                                            if (addresses.size == 1) {
                                                getCurrentWeather(addresses[0].latitude.toFloat(), addresses[0].longitude.toFloat())
                                                        .subscribe { currentWeather, throwable ->
                                                            application.location = addresses[0]
                                                            application.currentWeather = currentWeather
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
//        googleAPIClient = GoogleApiClient.Builder(this)
//                .addApi(LocationServices.API)
//                .addConnectionCallbacks(connectionCallBackListener)
//                .addOnConnectionFailedListener(connectionFailedListener)
//                .build()
//
//        locationRequest = LocationRequest.create()
//                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
//                .setInterval((10 * 1000).toLong())        // 10 seconds, in milliseconds
//                .setFastestInterval((1 * 1000).toLong()) // 1 second, in milliseconds
//
//        googleAPIClient.connect()

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
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { application.forecast = it }
                .singleOrError()
    }

    private fun getCurrentWeather(latitude: Float, longitude: Float): Single<CurrentWeather> {
        val res = application.resources
        return Retrofit.Builder()
                .baseUrl(res.getString(R.string.openweathermap_base_url))
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
                .create(GetForecastData::class.java)
                .getCurrrentWeatherByCoords(latitude, longitude, res.getString(R.string.openweathermap_appid))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { application.currentWeather = it }
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
                .doOnNext { if (it.size == 1) application.location = it[0] }
                .singleOrError()

    }

    private fun Location.coordsToString() = "${latitude},${longitude}"

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
                .doOnNext { application.location = it[0] }
                .singleOrError()
    }

}