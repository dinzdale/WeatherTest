package com.garyjacobs.weathertest;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.renderscript.RenderScript;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.util.LruCache;
import android.widget.ImageView;

import com.squareup.okhttp.ResponseBody;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.HttpsURLConnection;

import network.GetImageData;
import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;


/**
 * Created by gjacobs on 11/5/15.
 */
public class ImageManager {
    private ExecutorService fixedThreadPool;
    private final MemoryCache memoryCache;

    public ImageManager(int maxSize) {
        memoryCache = MemoryCache.getInstance(maxSize);
        fixedThreadPool = Executors.newFixedThreadPool(10);
    }

    public void setImage(String icon, ImageView image) {
        Bitmap bitmap;
        if ((bitmap = memoryCache.get(icon)) == null) {
            loadImageViewOld(icon, image);
            //loadImageViewNew(icon, image);
        } else {
            image.setImageBitmap(bitmap);
        }
    }

    public void kill() {
        fixedThreadPool.shutdownNow();
    }


    private void loadImageViewOld(final String icon, final ImageView image) {
        fixedThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final Bitmap bitmap;
                    String baseURL = image.getResources().getString(R.string.openweathermap_base_url);
                    URL bmURL = new URL(baseURL + "/img/w/" + icon);
                    HttpURLConnection httpURLConnection = (HttpURLConnection) bmURL.openConnection();
                    InputStream inputStream = httpURLConnection.getInputStream();
                    bitmap = BitmapFactory.decodeStream(inputStream);
                    ImageManager.this.memoryCache.set(icon, bitmap);
                    inputStream.close();
                    image.post(new Runnable() {
                        @Override
                        public void run() {
                            image.setImageBitmap(bitmap);
                        }
                    });

                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }

            }
        });
    }

    /**
     * Using Retrofit to load images.
     * Probably best to use Picaso
     *
     * @param icon
     * @param image
     */

    private void loadImageViewNew(final String icon, final ImageView image) {
        Retrofit.Builder builder = new Retrofit.Builder();
        builder.addConverterFactory(GsonConverterFactory.create());
        builder.baseUrl(image.getResources().getString(R.string.openweathermap_base_url));
        Retrofit retrofit = builder.build();

        GetImageData getImageData = retrofit.create(GetImageData.class);
        Call<ResponseBody> call = getImageData.getIcon(icon, image.getResources().getString(R.string.openweathermap_appid));

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Response<ResponseBody> response, Retrofit retrofit) {
                try {
                    Bitmap bitmap = BitmapFactory.decodeStream(response.body().byteStream());
                    ImageManager.this.memoryCache.set(icon, bitmap);
                    image.setImageBitmap(bitmap);
                } catch (IOException ioe) {

                }
            }

            @Override
            public void onFailure(Throwable throwable) {

            }
        });

    }
}
