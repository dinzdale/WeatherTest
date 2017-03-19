package com.garyjacobs.weathertest;

import android.support.v7.app.AppCompatActivity;
import android.view.Menu;

/**
 * Created by gjacobs on 11/11/15.
 */
public class WeatherActivity extends AppCompatActivity {
    public WeatherTestApplication getWeatherApplication() {
        return (WeatherTestApplication) getApplication();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }
}
