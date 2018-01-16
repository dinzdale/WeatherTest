package model.ArchComps

import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.garyjacobs.weathertest.WeatherTestApplication
import model.CurrentWeather
import model.Forecast

/**
 * Created by garyjacobs on 1/9/18.
 */
data class WeatherViewModel(val application: WeatherTestApplication, val lat: Double, val lon: Double) : AndroidViewModel(application) {
    val currentWeatherDB = WeatherDB.getInstance(getApplication())

    var currentWeatherList: LiveData<List<CurrentWeather>>? = null
        get() {
            if (field == null) {
                field = currentWeatherDB!!.weatherDao().loadCurrentWeather(lat, lon)
            }
            return field
        }
    var allCurrentWeatherList: LiveData<List<CurrentWeather>>? = null
        get() {
            if (field == null) {
                field = currentWeatherDB!!.weatherDao().loadAllCurrentWeather()
            }
            return field
        }

    var forecastList: LiveData<List<Forecast>>? = null
        get() {
            if (field == null) {
                field = currentWeatherDB!!.weatherDao().loadForecast(lat, lon)
            }
            return field
        }

}


class WeatherViewModelFactory(val application: WeatherTestApplication, val lat: Double, val lon: Double) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return WeatherViewModel(application, lat, lon) as T
    }
}