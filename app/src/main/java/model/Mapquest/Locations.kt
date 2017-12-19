package model.Mapquest

/**
 * Created by garyjacobs on 12/19/17.
 */
data class Locations(val void: Unit) {
    lateinit var street: String
    lateinit var adminArea6: String
    lateinit var adminArea6Type: String
    lateinit var adminArea5: String
    lateinit var adminArea5Type: String
    lateinit var adminArea4: String
    lateinit var adminArea4Type: String
    lateinit var adminArea3: String
    lateinit var adminArea3Type: String
    lateinit var adminArea1: String
    lateinit var adminArea1Type: String
    lateinit var postalCode: String
    lateinit var geocodeQualityCode: String
    lateinit var geocodeQuality: String
    var dragPoint: Boolean = false
    lateinit var sideOfStreet: String
    var linkId: Int = 0
    lateinit var unknownInput: String
    lateinit var type: String
    var latLng = object {
        var lat: Float = 0.toFloat()
        var lng: Float = 0.toFloat()
    }
    var displayLatLng = object {
        var lat: Float = 0.toFloat()
        var lng: Float = 0.toFloat()
    }
    lateinit var mapUrl: String
    var nearestIntersection = object {
        lateinit var streetDisplayName: String
        lateinit var distanceMeters: String
        var latLng = object {
            var longitude: Float = 0.toFloat()
            var latitude: Float = 0.toFloat()
        }
        lateinit var label: String
    }
    val roadMetadata = object {
        lateinit var speedLimitUnits: String
        var tollRoad: Boolean? = null
        var speedLimit: Int = 0
    }
}
