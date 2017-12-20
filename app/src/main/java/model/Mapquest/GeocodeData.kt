package model.Mapquest

import android.location.Address
import java.util.*

/**
 * Created by garyjacobs on 12/19/17.
 */
data class GeocodeData(val void: Unit) {
    var info: Info = Info(Unit)
    var options = object {
        var maxResults = 0
        var thumbMaps = false
        var ignoreLatLngInput = false
    }
     var results: Array<Results> = arrayOf()

    fun toAddresses(): Array<Address> = Array(results[0].locations.size,
            { index ->
                val nxtAddress = Address(Locale.getDefault())
                val location = results[0].locations[index]
                nxtAddress.latitude = location.latLng.lat.toDouble()
                nxtAddress.longitude = location.latLng.lng.toDouble()
                nxtAddress.postalCode = location.postalCode
                nxtAddress.countryName = location.adminArea1
                nxtAddress.url = location.mapUrl
                nxtAddress.setAddressLine(0, location.street)
                nxtAddress.setAddressLine(1, location.adminArea6)
                nxtAddress.setAddressLine(2, location.adminArea5)
                nxtAddress.setAddressLine(3, location.adminArea4)
                nxtAddress.setAddressLine(4, location.adminArea3)
                nxtAddress
            })
}