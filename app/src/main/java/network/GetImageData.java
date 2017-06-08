package network;

import android.graphics.Bitmap;

import com.garyjacobs.weathertest.R;
import com.squareup.okhttp.ResponseBody;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import retrofit.Call;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.http.Url;

/**
 * Created by gjacobs on 11/11/15.
 */
public interface GetImageData {
    @GET("/img/w/{icon}")
    Call<ResponseBody> getIcon(@Path("icon") String icon, @Query("appid") String appid);

   @GET("{url}")
    Call<ResponseBody> getIconFromUrl(String url);

}
