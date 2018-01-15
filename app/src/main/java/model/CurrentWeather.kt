package model

import android.arch.persistence.room.*
import model.ArchComps.Converters

/**
 * Created by garyjacobs on 12/18/17.
 */
@Entity(tableName = "currentweather")
@TypeConverters(Converters::class)
data class CurrentWeather(var void: Unit? = null) {
    @PrimaryKey
    @Embedded
    lateinit var coord: Coordinates
    @Ignore
    lateinit var sys: Sys
    lateinit var weather: ArrayList<Weather>
    lateinit var base: String
    @Embedded
    lateinit var main: CurrentTemp
    @Embedded
    lateinit var wind: Wind
    @Ignore
    lateinit var clouds: Clouds
    var dt = 0.toInt()
    var id = 0.toInt()
    var visibility = 0.toInt()

    lateinit var name: String
    var cod = 0.toInt()
}

data class Clouds(var all: Int = 0.toInt())