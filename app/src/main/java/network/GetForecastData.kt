package network

import android.content.Context
import android.content.res.Resources
import android.location.Address
import android.os.Message
import android.os.RemoteException
import android.widget.Toast
import com.garyjacobs.weathertest.R
import com.garyjacobs.weathertest.WeatherTestApplication
import io.reactivex.Observable
import model.CurrentWeather

import java.net.URL

import model.Forecast
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query


/**
 * Created by gjacobs on 10/31/15.
 */
interface GetForecastData {

    @get:GET("/data/2.5/forecast/daily?q=Philadelphia&mode=json&units=imperial&cnt=16&appid=0ff4cd732ec220998352961a3c4f2980")
    val phillyForecast: Observable<Forecast>

    @GET("/data/2.5/forecast/daily?mode=json&cnt=16")
    fun getForecast(@Query("q") city: String, @Query("units") units: String, @Query("appid") appid: String): Observable<Forecast>

    @GET("/data/2.5/forecast/daily/?mode=json")
    fun getForecastByZip(@Query("zip") zip: String, @Query("units") units: String, @Query("appid") appid: String): Observable<Forecast>

    @GET("/data/2.5/forecast/daily/?mode=json")
    fun getForecastByCoords(@Query("lat") lat: Double, @Query("lon") lon: Double, @Query("appid") appid: String, @Query("cnt") cnt: Int = 16, @Query("units") units: String = "imperial"): Observable<Forecast>

    @GET("data/2.5/weather?mode=json")
    fun getCurrrentWeatherByCoords(@Query("lat") lat: Float, @Query("lon") lon: Float, @Query("appid") appid: String, @Query("units") units: String = "imperial"): Observable<CurrentWeather>

}
