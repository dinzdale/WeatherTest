package model.ArchComps

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider


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