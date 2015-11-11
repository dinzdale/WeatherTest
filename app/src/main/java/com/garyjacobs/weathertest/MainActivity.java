package com.garyjacobs.weathertest;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;


import com.google.gson.Gson;
import com.squareup.otto.Subscribe;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import Events.ForecastListSelected;
import model.Forecast;
import model.ForecastDetails;
import network.GetForecastData;
import retrofit.Call;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

public class MainActivity extends WeatherActivity {

    public static int FORECASTDATA_FETCHED = 1;
    FrameLayout weatherListContainer;
    FrameLayout weatherDetailsContainer;
    boolean twoPane;
    boolean isBound = false;
    Messenger outBoundMessenger;
    Message outBoundMessage;

    @Override
    protected void onResume() {
        super.onResume();
        getWeatherApplication().getBus().register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getWeatherApplication().getBus().unregister(this);
    }

    @Subscribe
    public void OnItemSelected(ForecastListSelected forecastListSelected) {
        if (twoPane) {
            LoadForecastDetailsFragment(forecastListSelected.getSelectedItem(), R.id.weather_details_container, false);
        } else {
            LoadForecastDetailsFragment(forecastListSelected.getSelectedItem(), R.id.weather_list_container, true);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FrameLayout fl = (FrameLayout) findViewById(R.id.main_frame_layout);
        View v = (View) getLayoutInflater().inflate(R.layout.weather_panel_layout, null);
        fl.addView(v);
        twoPane = (FrameLayout) v.findViewById(R.id.weather_details_container) != null;
        Intent intent = new Intent(this, FetchForecastService.class);
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case FetchForecastService.CLIENT_REGISTERED:
                    outBoundMessage = Message.obtain();
                    outBoundMessage.what = FetchForecastService.FETCH_CITY_FORECAST;
                    try {
                        outBoundMessenger.send(outBoundMessage);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case FetchForecastService.CITY_FORECAST_FETCHED:
                    LoadWeatherListFragment();
                    if (twoPane) {
                        LoadForecastDetailsFragment(0, R.id.weather_details_container, false);
                    }
                    getSupportActionBar().setTitle(getWeatherApplication().getForecast().getCity().getName());
                    break;
            }
        }
    }

    private void LoadWeatherListFragment() {
        FragmentManager fm = getFragmentManager();
        fm.beginTransaction().replace(R.id.weather_list_container, new WeatherListFragment()).commit();
    }

    private void LoadForecastDetailsFragment(int position, int containerID, boolean incCount) {
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(containerID, ForecastDetailsFragment.newInstance(position));
        if (incCount) {
            ft.addToBackStack("WeatherDetails");
        }
        ft.commit();
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            isBound = true;
            outBoundMessenger = new Messenger(service);

            Message msg = Message.obtain();
            msg.replyTo = new Messenger(new IncomingHandler());
            msg.what = FetchForecastService.REGISTER_CLIENT;
            try {
                outBoundMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getFragmentManager();
        int fragCount = fm.getBackStackEntryCount();
        if (fragCount > 0) {
            fm.popBackStack();
        }
        else {
            super.onBackPressed();
        }

    }
}

