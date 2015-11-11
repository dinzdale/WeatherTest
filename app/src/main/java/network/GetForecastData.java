package network;

import com.garyjacobs.weathertest.R;

import java.net.URL;

import model.Forecast;
import retrofit.Call;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * Created by gjacobs on 10/31/15.
 */
public interface GetForecastData {
    @GET("/data/2.5/forecast/daily?mode=json&cnt=16")
    Call<Forecast> getForecast(@Query("q") String city, @Query("units") String units, @Query("appid") String appid);

    @GET("/data/2.5/forecast/daily?q=Philadelphia&mode=json&units=imperial&cnt=16&appid=0ff4cd732ec220998352961a3c4f2980")
    Call<Forecast> getPhillyForecast();
}
