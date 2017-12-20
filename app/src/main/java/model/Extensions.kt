package model

import android.location.Address

/**
 * Created by garyjacobs on 12/19/17.
 */
fun Address.formatAddress() : String {
    /*
    nxtAddress.latitude = location.latLng.lat.toDouble()
    nxtAddress.longitude = location.latLng.lng.toDouble()
    nxtAddress.postalCode = location.postalCode
    nxtAddress.countryName = location.adminArea1
    nxtAddress.url = location.mapUrl
    nxtAddress.setAddressLine(0,location.street)
    nxtAddress.setAddressLine(1,location.adminArea6)
    nxtAddress.setAddressLine(2, location.adminArea5)
    nxtAddress.setAddressLine(3,location.adminArea4)
    nxtAddress.setAddressLine( 4, location.adminArea3)
    */
    return "${getAddressLine(0)} ${getAddressLine(1)} ${getAddressLine(2)} ${getAddressLine(3)} " + "${getAddressLine(4)} $countryName $postalCode"

}