package model.ArchComps

import Events.getCurrentWeather
import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.content.Context
import com.garyjacobs.weathertest.WeatherTestApplication
import model.CurrentWeather

/**
 * Created by garyjacobs on 1/9/18.
 */
data class CurrentWeatherViewModel(val application: WeatherTestApplication, val lat: Double, val lon: Double) : AndroidViewModel(application) {
    val currentWeatherDB = WeatherDB.getInstance(getApplication())

    var currentWeatherList: LiveData<List<CurrentWeather>>? = null
        get() {
            if (field == null) {
                field = currentWeatherDB!!.weatherDao().loadCurrentWeather(lat, lon)
            }
            return field
        }
    var allCurrentWeatherList : LiveData<List<CurrentWeather>> ? = null
    get() {
        if (field == null) {
            field = currentWeatherDB!!.weatherDao().loadAllCurrentWeather()
        }
        return field
    }
}

class CurrentWeatherViewModelFactory(val application: WeatherTestApplication, val lat: Double, val lon: Double) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return CurrentWeatherViewModel(application, lat, lon) as T
    }
}