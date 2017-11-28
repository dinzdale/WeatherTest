package model

/**
 * Created by gjacobs on 10/31/15.
 */
data class City(val void : Any) {
    var id: Int = 0
    lateinit var name: String
    lateinit var coord: Coordinates
    lateinit var country: String
    var population: Int = 0
}
