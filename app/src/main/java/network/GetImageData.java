package network;

import android.graphics.Bitmap;

import com.garyjacobs.weathertest.R;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;


/**
 * Created by gjacobs on 11/11/15.
 */
public interface GetImageData {
    @GET("/img/w/{icon}")
    Call<ResponseBody> getIcon(@Path("icon") String icon, @Query("appid") String appid);

   @GET("{url}")
    Call<ResponseBody> getIconFromUrl(String url);

}
