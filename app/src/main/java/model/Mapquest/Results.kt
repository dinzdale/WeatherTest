package model.Mapquest

/**
 * Created by garyjacobs on 12/19/17.
 */
data class Results(val void: Unit) {
    var providedLocation = object {
        var latLng = object {
            var lat = 0.toFloat()
            var lng = 0.toFloat()
        }
    }
     var locations: Array<Locations> = arrayOf()
}