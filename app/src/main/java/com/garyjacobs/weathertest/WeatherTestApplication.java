package com.garyjacobs.weathertest;

import android.app.Application;

import com.squareup.otto.Bus;
import com.squareup.picasso.Picasso;

import model.Forecast;

/**
 * Created by gjacobs on 11/1/15.
 */
public class WeatherTestApplication extends Application {
    Forecast forecast;
    ImageManager imageManager;
    Bus bus;

    @Override
    public void onCreate() {
        super.onCreate();

        // create Otto bus
        imageManager = new ImageManager(50);
        bus = new Bus();

        // set up Picasso
        Picasso.Builder builder = new Picasso.Builder(this);
        builder.indicatorsEnabled(this.getResources().getBoolean(R.bool.picasso_debug));
        //builder.memoryCache();
        Picasso.setSingletonInstance(builder.build());
    }

    public Forecast getForecast() {
        return forecast;
    }

    public void setForecast(Forecast forecast) {
        this.forecast = forecast;
    }

    public ImageManager getImageManager() {
        return imageManager;
    }

    public Bus getBus() {
        return bus;
    }
}
