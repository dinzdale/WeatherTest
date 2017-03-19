package com.garyjacobs.weathertest;

import android.Manifest;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.squareup.otto.Subscribe;

import java.io.IOException;
import java.security.Permission;
import java.util.List;

import Events.ForecastListSelected;
import widgets.ComboBox;

public class MainActivity extends WeatherActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    public static int FORECASTDATA_FETCHED = 1;
    private GoogleApiClient mGoogleAPIClient;
    private LocationRequest locationRequest;
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
        super.onStart();
        // check if user has granted location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        } else {
            mGoogleAPIClient.connect();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mGoogleAPIClient.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleAPIClient.disconnect();
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
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        // Create the LocationRequest object
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds


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
            handleNewLocation(lastLocation);
        } else {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleAPIClient, locationRequest, this);
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }

    private void handleNewLocation(Location newLocation) {
        Geocoder geocoder = new Geocoder(this);
        try {
            List<Address> addressList = geocoder.getFromLocation(newLocation.getLatitude(), newLocation.getLongitude(), 1);
            cityForecastCB.setText(addressList.get(0).getLocality());
            if (!isBound) {
                // start ForcastFetcher service
                Intent intent = new Intent(this, FetchForecastService.class);
                bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
            }
        } catch (IOException ioe) {
            Toast.makeText(this, "Error obtaining zipcode", Toast.LENGTH_LONG);
        }
    }
}

