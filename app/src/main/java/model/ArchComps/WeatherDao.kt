package model.ArchComps

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*
import model.CurrentWeather
import model.Forecast

/**
 * Created by garyjacobs on 1/10/18.
 */
@Dao
interface WeatherDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCurrentWeather(currentWeather: CurrentWeather)

    @Query("DELETE FROM currentweather")
    fun deleteCurrentWeather()

    @Query("SELECT * from CurrentWeather")
    fun loadAllCurrentWeather(): LiveData<List<CurrentWeather>>

    @Query("SELECT * from CurrentWeather where lat=:lat AND lon=:lon")
    fun loadCurrentWeather(lat: Double, lon: Double): LiveData<List<CurrentWeather>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertForecast(forecast: Forecast)

    @Query("SELECT * from Forecast where lattitude=:lat AND longitude=:lon")
    fun loadForecast(lat: Double, lon: Double): LiveData<List<Forecast>>

}