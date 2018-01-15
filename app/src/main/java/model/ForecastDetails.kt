package model

/**
 * Created by gjacobs on 10/31/15.
 */
data class ForecastDetails(val void: Unit) {
    var dt: Int = 0
    lateinit var temp: Temperature
    var pressure: Float = 0.toFloat()
    var humidity: Int = 0
    lateinit var weather: ArrayList<Weather>
    var speed: Float = 0.toFloat()
    var deg: Int = 0
    var clouds: Int = 0
}
