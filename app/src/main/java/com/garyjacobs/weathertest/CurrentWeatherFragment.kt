package com.garyjacobs.weathertest

import android.app.Fragment
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.current_weather.*

/**
 * Created by garyjacobs on 12/18/17.
 */
class CurrentWeatherFragment : Fragment() {
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
            wind.text = myActivity.resources.getString(R.string.current_wind, it.wind.speed.toInt())
        }

    }
}