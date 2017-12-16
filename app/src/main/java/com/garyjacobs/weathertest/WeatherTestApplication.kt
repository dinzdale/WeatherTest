package com.garyjacobs.weathertest

import android.app.Application
import  android.location.Address
import com.squareup.otto.Bus
import com.squareup.picasso.Picasso
import model.Forecast

/**
 * Created by gjacobs on 11/1/15.
 */
class WeatherTestApplication : Application() {
    var forecast: Forecast? = null
    var location: Address? = null
    lateinit var imageManager: ImageManager
    lateinit var bus: Bus


    override fun onCreate() {
        super.onCreate()

        // create Otto bus
        imageManager = ImageManager(50)
        bus = Bus()

        // set up Picasso
        val builder = Picasso.Builder(this)
        builder.indicatorsEnabled(this.resources.getBoolean(R.bool.picasso_debug))
        //builder.memoryCache();
        Picasso.setSingletonInstance(builder.build())
    }
}
