package model

/**
 * Created by gjacobs on 10/31/15.
 */
data class Weather(val void: Any) {
    var id: Int = 0
    lateinit var main: String
    lateinit var description: String
    lateinit var icon: String
}
