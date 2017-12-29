package com.garyjacobs.weathertest

import Events.*
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.current_weather.*
import model.getWindDirection
import widgets.SlideMotion
import widgets.doSlideAnimation
import widgets.getAlphaAnimator

/**
 * Created by garyjacobs on 12/18/17.
 */
class CurrentWeatherFragment : Fragment() {

    lateinit var myActivity: WeatherActivity
    lateinit var extendForecastAnimation: ObjectAnimator

    companion object {
        val TAG = CurrentWeatherFragment::class.java.simpleName
        var me: CurrentWeatherFragment? = null
        fun getInstance(): CurrentWeatherFragment {
            if (me == null) {
                me = CurrentWeatherFragment()
            }
            return me!!
        }
    }

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
            val windDirection = it.wind.deg.toInt()

            wind.text = myActivity.resources.getString(R.string.current_wind, it.wind.speed.toInt(), getWindDirection(windDirection))

            // setup up map in background
            current_weather_map.onCreate(savedInstanceState)

            current_weather_map.getMapAsync(object : OnMapReadyCallback {

                override fun onMapReady(googleMap: GoogleMap?) {
                    googleMap?.let {
                        val latlon = LatLng(myActivity.weatherApplication.location.latitude, myActivity.weatherApplication.location.longitude)
                        it.moveCamera(CameraUpdateFactory.newLatLngZoom(latlon, 10.toFloat()))
                        it.addMarker(MarkerOptions()
                                .position(latlon))
                        googleMap.setOnMapClickListener {
                            if (cw_cardview.visibility != View.VISIBLE)
                                doSlideAnimation(cw_cardview, SlideMotion.SLIDEINDOWNRIGHT)
                            myActivity.weatherApplication.bus.post(MapClickedEvent())
                        }
                    }
                }
            })

            getFlingObervable(cw_cardview)
                    .subscribe {
                        doSlideAnimation(cw_cardview, SlideMotion.SLIDEOUTUPLEFT)
                    }

            getSingleTapObservable(extended_forcast)
                    .subscribe {
                        myActivity.weatherApplication.bus.post(CurrentWeatherSelectedEvent())
                    }

            extendForecastAnimation = getAlphaAnimator(extended_forcast)

        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        current_weather_map?.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        current_weather_map?.onStart()
    }

    override fun onResume() {
        super.onResume()
        current_weather_map?.onResume()
        extendForecastAnimation.start()

    }

    override fun onPause() {
        super.onPause()
        current_weather_map?.onPause()
        extendForecastAnimation.cancel()
    }

    override fun onStop() {
        super.onStop()
        current_weather_map?.onStop()
        extendForecastAnimation.cancel()
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