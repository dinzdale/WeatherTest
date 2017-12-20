package model

/**
 * Created by gjacobs on 10/31/15.
 */
data class Forecast(val void: Unit) {
    lateinit var city: City
    lateinit var cod: String
    var message: Float = 0.toFloat()
    var cnt: Int = 0
    lateinit var list: List<ForecastDetails>
}
