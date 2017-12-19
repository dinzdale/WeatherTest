package network

import android.location.Address
import model.CurrentWeather
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Created by garyjacobs on 12/19/17.
 */
class GetGeocodeInfo {
@GET("geocoding/v1/reverse/outputFormat?json")
fun getReverseGeocodedAddress(@Query("location") location: String,@Query("key") key: String): Call<Address>

