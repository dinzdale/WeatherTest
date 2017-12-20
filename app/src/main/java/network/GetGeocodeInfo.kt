package network

import android.location.Address
import model.CurrentWeather
import model.Mapquest.GeocodeData
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Created by garyjacobs on 12/19/17.
 */
interface GetGeocodeInfo {
    @GET("geocoding/v1/address?outputFormat=json")
    fun getGeocodedAddress(@Query("location") location: String, @Query("key") key : String, @Query("maxResults") maxResults : Int = 5) : Call<GeocodeData>

    @GET("geocoding/v1/reverse?outputFormat=json")
    fun getReverseGeocodedAddress(@Query("location") location: String, @Query("key") key: String): Call<GeocodeData>
}