package model

/**
 * Created by garyjacobs on 12/18/17.
 */
class Sys(val void: Unit) {
    var type = 0.toInt()
    var id = 0.toInt()
    var message = 0.toFloat()
    lateinit var country: String
    var sunrise = 0.toInt()
    var sunset = 0.toInt()
}