package model

/**
 * Created by garyjacobs on 12/18/17.
 */
data class CurrentTemp(val void: Unit? = null) {
    var temp = 0.toFloat()
    var pressure = 0.toInt()
    var humidity = 0.toInt()
    var temp_min = 0.toFloat()
    var temp_max = 0.toFloat()
}