package com.garyjacobs.weathertest

import Events.CurrentWeatherSelectedEvent
import android.app.Fragment
import android.os.Bundle
import android.support.annotation.FloatRange
import android.util.Range
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.current_weather.*

/**
 * Created by garyjacobs on 12/18/17.
 */
class CurrentWeatherFragment : Fragment() {
    val windirectionMap = hashMapOf<IntRange, String>(348..360 to "N",
            0..11 to "N",
            12..33 to "NNE",
            34..56 to "NE",
            57..78 to "E",
            79..101 to "ESE",
            102..123 to "SE",
            124..146 to "SSE",
            147..168 to "S",
            169..191 to "SSW",
            192..213 to "SW",
            214..236 to "WSW",
            237..258 to "W",
            259..281 to "WNW",
            282..303 to "NW",
            304..326 to "NW",
            327..348 to "NNW",
            349..360 to "N")

    lateinit var myActivity: WeatherActivity

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        myActivity = activity as WeatherActivity
        return inflater!!.inflate(R.layout.current_weather, null)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        myActivity.weatherApplication.currentWeather?.let {

            myActivity.weatherApplication.imageManager.setImage(it.weather[0].icon, current_weather_icon)
            city.text = it.name
            description.text = it.weather[0].description
            current_temp.text = it.main.temp.toInt().toString()
            low_temp.text = myActivity.resources.getString(R.string.current_low, it.main.temp_min.toInt())
            high_temp.text = myActivity.resources.getString(R.string.current_high, it.main.temp_max.toInt())
            val windDirection = it.wind.deg
            val matchedKey = windirectionMap.keys.filter {
                it.contains(windDirection)
            } [0]

            wind.text = myActivity.resources.getString(R.string.current_wind, it.wind.speed.toInt(), windirectionMap.get(matchedKey))

            // setup up map in background
            current_weather_map.onCreate(savedInstanceState)
            current_weather_map.getMapAsync(object : OnMapReadyCallback {

                override fun onMapReady(googleMapApi: GoogleMap?) {
                    googleMapApi?.let {
                        val latlon = LatLng(myActivity.weatherApplication.location.latitude, myActivity.weatherApplication.location.longitude)
                        it.moveCamera(CameraUpdateFactory.newLatLngZoom(latlon, 10.toFloat()))
                        it.addMarker(MarkerOptions()
                                .position(latlon))
                    }
                }
            })
            cw_constraint_layout.setOnLongClickListener {
                myActivity.weatherApplication.bus.post(CurrentWeatherSelectedEvent())
                true
            }
        }

    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        current_weather_map.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        current_weather_map.onStart()
    }

    override fun onResume() {
        super.onResume()
        current_weather_map.onResume()
    }

    override fun onPause() {
        super.onPause()
        current_weather_map.onPause()
    }

    override fun onStop() {
        super.onStop()
        current_weather_map.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        current_weather_map?.let { current_weather_map.onDestroy() }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        current_weather_map?.let {
            current_weather_map.onLowMemory()
        }
    }
}