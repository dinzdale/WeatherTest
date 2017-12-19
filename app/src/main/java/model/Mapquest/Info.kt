package model.Mapquest

/**
 * Created by garyjacobs on 12/19/17.
 */
class Info(val void: Unit) {

    var statuscode = 0
    var copyright = object {
        lateinit var text : String
        lateinit var imageUrl : String
        lateinit var imageAltText : String
    }
    lateinit var messages: Array<String>
}