package com.garyjacobs.weathertest;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import model.Forecast;
import network.GetForecastData;
import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * Created by gjacobs on 11/2/15.
 */
public class FetchForecastService extends Service {


    public static final int SET_FAHRENHEIGHT = 1;
    public static final int SET_CELCIUS = 2;
    public static final int FETCH_CITY_FORECAST = 3;
    public static final int REGISTER_CLIENT = 4;
    public static final int UNREGISTER_CLIENT = 5;
    //
    public static final int CLIENT_REGISTERED = 100;
    public static final int CITY_FORECAST_FETCHED = 101;

    Messenger inBoundMessenger = new Messenger(new IncomingHandler());
    Messenger outBoundMessenger;
    Message outBoundMessage;

    @Override
    public void onCreate() {
        //
    }

    class IncomingHandler extends Handler {

        @Override
        public void handleMessage(final Message msg) {
            outBoundMessage = Message.obtain();
            switch (msg.what) {
                case REGISTER_CLIENT:
                    outBoundMessenger = msg.replyTo;
                    outBoundMessage.what = CLIENT_REGISTERED;
                    try {
                        outBoundMessenger.send(outBoundMessage);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case UNREGISTER_CLIENT:
                    outBoundMessenger = null;
                    break;
                case SET_FAHRENHEIGHT:
                    break;
                case SET_CELCIUS:
                    break;
                case FETCH_CITY_FORECAST:
                    Bundle bundle = msg.getData();
                    String zipcode = bundle.getString("FETCH_CITY_FORECAST");
                    if (zipcode.length() > 0) {
                        DoNewNetworkCall(zipcode);
                    }
                    break;
            }
        }
    }

    private void DoOldNetworkCall() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(getString(R.string.openweathermap_base_url) + "/data/2.5/forecast/daily?q=Philadelphia&mode=json&units=imperial&cnt=16&appid=0ff4cd732ec220998352961a3c4f2980");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                    BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String nxtLine;
                    while ((nxtLine = br.readLine()) != null) {
                        sb.append(nxtLine);
                    }
                    br.close();
                    Gson gson = new Gson();
                    ((WeatherTestApplication) getApplication()).setForecast(gson.fromJson(sb.toString(), Forecast.class));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                outBoundMessage = Message.obtain();
                outBoundMessage.what = CITY_FORECAST_FETCHED;
                try {
                    outBoundMessenger.send(outBoundMessage);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void DoNewNetworkCall(String zip) {
        Retrofit.Builder builder = new Retrofit.Builder();
        builder.baseUrl(getString(R.string.openweathermap_base_url));
        builder.addConverterFactory(GsonConverterFactory.create());
        Retrofit retrofit = builder.build();
        GetForecastData getForecastData = retrofit.create(GetForecastData.class);
        //Call<Forecast> call = getForecastData.getPhillyForecast();
        // Call<Forecast> call = getForecastData.getForecast(zip, "imperial", getString(R.string.openweathermap_appid));
        Call<Forecast> call = getForecastData.getForecastByZip(zip, "imperial", getString(R.string.openweathermap_appid));

        call.enqueue(new Callback<Forecast>() {
            @Override
            public void onResponse(Response<Forecast> response, Retrofit retrofit) {
                ((WeatherTestApplication) getApplication()).setForecast(response.body());
                outBoundMessage = Message.obtain();
                outBoundMessage.what = CITY_FORECAST_FETCHED;
                try {
                    outBoundMessenger.send(outBoundMessage);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                Toast.makeText(FetchForecastService.this, "Retrofit FAIL", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return inBoundMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }
}
