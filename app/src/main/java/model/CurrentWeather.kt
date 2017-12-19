package model

/**
 * Created by garyjacobs on 12/18/17.
 */
data class CurrentWeather(val void: Any) {
    lateinit var coord: Coordinates
    lateinit var weather: Array<Weather>
    lateinit var base: String
    lateinit var main: CurrentTemp
    var visibility = 0.toInt()
    lateinit var wind: Wind
    lateinit var clouds: Clouds
    var dt = 0.toInt()
    lateinit var sys: Sys
    var id = 0.toInt()
    lateinit var name: String
    var cod = 0.toInt()
}

data class Clouds(var all: Int = 0.toInt())