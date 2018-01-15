package model.ArchComps

import android.arch.persistence.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import model.Forecast
import model.ForecastDetails
import model.Weather

/**
 * Created by garyjacobs on 1/12/18.
 */

class Converters() {

    @TypeConverter
    fun toWeatherArrayList(weatherS: String): ArrayList<Weather> {
        val token = object : TypeToken<ArrayList<Weather>>() {}
        return Gson().fromJson<ArrayList<Weather>>(weatherS, token.type)
    }

    @TypeConverter
    fun toWeatherString(list: ArrayList<Weather>): String {
        val token = object : TypeToken<ArrayList<Weather>>() {}
        return Gson().toJson(list, token.type)
    }

    @TypeConverter
    fun toForecastDetailsArrayList(forecastDetailsS: String): ArrayList<ForecastDetails> {
        val token = object : TypeToken<ArrayList<ForecastDetails>>() {}
        return Gson().fromJson<ArrayList<ForecastDetails>>(forecastDetailsS, token.type)
    }

    @TypeConverter
    fun toForecastDetailsString(list: ArrayList<ForecastDetails>): String {
        val token = object : TypeToken<ArrayList<ForecastDetails>>() {}
        return Gson().toJson(list, token.type)
    }
}