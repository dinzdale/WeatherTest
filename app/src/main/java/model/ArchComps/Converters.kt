package model.ArchComps

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import model.Weather

/**
 * Created by garyjacobs on 1/12/18.
 */

class Converters() {

    @TypeConverter
    fun toArrayList(weatherS: String): ArrayList<Weather> {
//        val gson = Gson()
//        val weatherListStringsType = object : TypeToken<ArrayList<String>>() {}.type
//        val weatherListStrings: ArrayList<String> = gson.fromJson(weatherS, weatherListStringsType)
//        val weatherType = object : TypeToken<Weather>() {}.type
//        val weatherList = weatherListStrings.map { gson.fromJson<Weather>(it, weatherType) }
//        return ArrayList(weatherList)
        val token = object : TypeToken<ArrayList<Weather>>() {}
        return Gson().fromJson<ArrayList<Weather>>(weatherS,token.type)
    }

    @TypeConverter
    fun toString(list: ArrayList<Weather>): String {
//        val newlist = list.map { it.toString() }
//        return Gson().toJson(newlist)
        val token = object : TypeToken<ArrayList<Weather>>() {}
        return Gson().toJson(list, token.type)
    }

}