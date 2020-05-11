package com.garyjacobs.weathertest

import Events.*
import android.animation.ObjectAnimator

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.current_weather.*
import model.ArchComps.CurrentWeatherViewModel
import model.ArchComps.CurrentWeatherViewModelFactory
import model.CurrentWeather
import model.getWindDirection
import widgets.SlideMotion
import widgets.doSlideAnimation
import widgets.getAlphaAnimator

/**
 * Created by garyjacobs on 12/18/17.
 */
class CurrentWeatherFragment : Fragment() {

    lateinit var myActivity: WeatherActivity
    var extendForecastAnimation: ObjectAnimator? = null
    lateinit var currentWeatherViewModel: CurrentWeatherViewModel
    var lat: Double = 0.0
    var lon: Double = 0.0

    companion object {
        val TAG = CurrentWeatherFragment::class.java.simpleName
        var me: CurrentWeatherFragment? = null
        fun getInstance(lat: Double, lon: Double): CurrentWeatherFragment {
            val bundle = Bundle()
            bundle.putDouble("lat", lat)
            bundle.putDouble("lon", lon)
            me = CurrentWeatherFragment()
            me!!.arguments = bundle
            return me!!
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        myActivity = activity as WeatherActivity
        savedInstanceState?.let {
            lat = it.getDouble("lat")
            lon = it.getDouble("lon")
        }
        arguments?.let {
            lat = it.getDouble("lat")
            lon = it.getDouble("lon")
        }

        currentWeatherViewModel = ViewModelProvider(this, CurrentWeatherViewModelFactory(myActivity.weatherApplication, lat, lon))
                .get(CurrentWeatherViewModel::class.java)

        return inflater.inflate(R.layout.current_weather, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        currentWeatherViewModel.allCurrentWeatherList?.observe(this.viewLifecycleOwner, object : Observer<List<CurrentWeather>> {
            override fun onChanged(allCurrentWeatherList: List<CurrentWeather>?) {
                allCurrentWeatherList?.let {
                    it.forEachIndexed { index, cw ->
                        Log.d(TAG, "CurrentWeatherDB[$index] = $cw.name")
                    }
                }
            }
        })
        currentWeatherViewModel.currentWeatherList?.observe(this.viewLifecycleOwner, object : Observer<List<CurrentWeather>> {
            override fun onChanged(currentWeatherList: List<CurrentWeather>?) {
                currentWeatherList?.let {
                    if (it.size > 0) {
                        updateUI(it[0])
                    }
                }
            }
        })
        current_weather_map.onCreate(savedInstanceState)
    }


    fun updateUI(currentWeather: CurrentWeather) {

        myActivity.weatherApplication.imageManager.setImage(currentWeather.weather[0].icon, current_weather_icon)
        city.text = currentWeather.name
        description.text = currentWeather.weather[0].description
        current_temp.text = currentWeather.main.temp.toInt().toString()
        low_temp.text = myActivity.resources.getString(R.string.current_low, currentWeather.main.temp_min.toInt())
        high_temp.text = myActivity.resources.getString(R.string.current_high, currentWeather.main.temp_max.toInt())
        val windDirection = currentWeather.wind.deg.toInt()

        wind.text = myActivity.resources.getString(R.string.current_wind, currentWeather.wind.speed.toInt(), getWindDirection(windDirection))

        current_weather_map.getMapAsync(object : OnMapReadyCallback {

            override fun onMapReady(googleMap: GoogleMap?) {
                googleMap?.let {
                    val latlon = LatLng(currentWeather.coord.lat, currentWeather.coord.lon)
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
        extendForecastAnimation?.start()

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        arguments?.let {
            outState.putDouble("lat", it.getDouble("lat"))
            outState.putDouble("lon", it.getDouble("lon"))
        }
        current_weather_map?.onSaveInstanceState(outState)
    }

    override fun onStart() {
        super.onStart()
        current_weather_map?.onStart()
    }

    override fun onResume() {
        super.onResume()
        current_weather_map?.onResume()
    }

    override fun onPause() {
        super.onPause()
        current_weather_map?.onPause()
        extendForecastAnimation?.cancel()
    }

    override fun onStop() {
        super.onStop()
        current_weather_map?.onStop()
        extendForecastAnimation?.cancel()
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