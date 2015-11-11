package com.garyjacobs.weathertest;

import android.app.Application;

import com.squareup.otto.Bus;

import model.Forecast;

/**
 * Created by gjacobs on 11/1/15.
 */
public class WeatherTestApplication extends Application {
    Forecast forecast;
    ImageManager imageManager;
    Bus bus;

    public WeatherTestApplication() {
        imageManager = new ImageManager(50);
        bus = new Bus();
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
