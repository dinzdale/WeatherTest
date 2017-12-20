package model.Mapquest

/**
 * Created by garyjacobs on 12/19/17.
 */
data class Locations(val void: Unit) {
    var street: String = ""
    var adminArea6: String = ""
    var adminArea6Type: String = ""
    var adminArea5: String = ""
    var adminArea5Type: String = ""
    var adminArea4: String = ""
    var adminArea4Type: String = ""
    var adminArea3: String = ""
    var adminArea3Type: String = ""
    var adminArea1: String = ""
    var adminArea1Type: String = ""
    var postalCode: String = ""
    var geocodeQualityCode: String = ""
    var geocodeQuality: String = ""
    var dragPoint: Boolean = false
    var sideOfStreet: String = ""
    var linkId: String = ""
    var unknownInput: String = ""
    var type: String = ""
    var latLng: LatLng = LatLng(0.toFloat(), 0.toFloat())
    var displayLatLng: LatLng = LatLng(0.toFloat(), 0.toFloat())
    var mapUrl: String = ""
    var nearestIntersection = object {
        var streetDisplayName: String = ""
        var distanceMeters: String = ""
        var latLng = object {
            var longitude: Float = 0.toFloat()
            var latitude: Float = 0.toFloat()
        }
        var label: String = ""
    }
    val roadMetadata = object {
        lateinit var speedLimitUnits: String
        var tollRoad: Boolean? = null
        var speedLimit: Int = 0
    }
}

data class LatLng(val lat: Float = 0.toFloat(), val lng: Float = 0.toFloat())
