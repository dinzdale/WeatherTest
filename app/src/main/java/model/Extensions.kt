package model

import android.content.Context
import android.location.Address
import android.net.ConnectivityManager
import android.provider.Settings

/**
 * Created by garyjacobs on 12/19/17.
 */
private val windirectionMap = hashMapOf<IntRange, String>(348..360 to "N",
        0..11 to "N",
        12..33 to "NNE",
        34..56 to "NE",
        57..78 to "E",
        79..101 to "ESE",
        102..123 to "SE",
        124..146 to "SSE",
        147..168 to "S",
        169..191 to "SSW",
        192..213 to "SW",
        214..236 to "WSW",
        237..258 to "W",
        259..281 to "WNW",
        282..303 to "NW",
        304..326 to "NW",
        327..348 to "NNW",
        349..360 to "N")

fun getWindDirection(deg: Int): String? {
    val matchedKey = windirectionMap.keys.filter {
        it.contains(deg)
    }[0]
    return windirectionMap.get(matchedKey)
}

fun Address.formatAddress(): String {

    return "${getAddressLine(0)} ${getAddressLine(1)} ${getAddressLine(2)} ${getAddressLine(3)} " + "${getAddressLine(4)} $countryName $postalCode"

}


