package model.Mapquest

/**
 * Created by garyjacobs on 12/19/17.
 */
data class GeocodeData(val void: Unit) {
    lateinit var info: Info
    var options = object {
        var maxResults = 0
        var thumbMaps = false
        var ignoreLatLngInput = false
    }
    lateinit var results: Array<Results>
}