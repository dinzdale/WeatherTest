package com.garyjacobs.weathertest;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.squareup.otto.Subscribe;

import Events.ForecastListSelected;
import widgets.ComboBox;

public class MainActivity extends WeatherActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static int FORECASTDATA_FETCHED = 1;
    private GoogleApiClient mGoogleAPIClient;
    FrameLayout weatherListContainer;
    FrameLayout weatherDetailsContainer;
    private ComboBox cityForecastCB;
    boolean twoPane;
    boolean isBound = false;
    IBinder iBinder;
    Messenger outBoundMessenger;
    Messenger inBoundMessenger;
    Message outBoundMessage;

    @Override
    protected void onStart() {
        mGoogleAPIClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleAPIClient.disconnect();
        super.onStop();
    }

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
        mGoogleAPIClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        FrameLayout fl = (FrameLayout) findViewById(R.id.main_frame_layout);
        cityForecastCB = (ComboBox) findViewById(R.id.location_cb);
        cityForecastCB.setClientClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //currentZip = ((TextView) v).getText().toString();
                if (isBound) {
                    Message message = Message.obtain();
                    message.replyTo = inBoundMessenger;
                    message.what = FetchForecastService.FETCH_CITY_FORECAST;
                    Bundle bundle = new Bundle();
                    bundle.putString("FETCH_CITY_FORECAST", cityForecastCB.getCurrentLocation());
                    message.setData(bundle);
                    try {
                        outBoundMessenger.send(message);
                    } catch (RemoteException re) {
                        Log.d(null, re.getMessage(), re);
                    }
                }

            }
        });
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
                    Bundle bundle = new Bundle();
                    bundle.putString("FETCH_CITY_FORECAST", cityForecastCB.getCurrentLocation());
                    outBoundMessage.setData(bundle);
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
            iBinder = service;
            isBound = true;
            outBoundMessenger = new Messenger(iBinder);
            inBoundMessenger = new Messenger(new IncomingHandler());
            Message msg = Message.obtain();
            msg.replyTo = inBoundMessenger;
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
        } else {
            super.onBackPressed();
        }

    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleAPIClient);
        if (lastLocation != null) {
           
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}

