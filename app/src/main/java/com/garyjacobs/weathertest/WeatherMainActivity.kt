package com.garyjacobs.weathertest

import Events.CurrentWeatherSelectedEvent
import Events.ForecastListSelectedEvent
import android.Manifest
import android.app.Fragment
import android.app.FragmentTransaction
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Address
import android.os.*
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.Toast
import com.squareup.otto.Subscribe
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.combobox.*
import kotlinx.android.synthetic.main.weather_panel_layout.view.*
import model.formatAddress
import widgets.ComboBox

/**
 * Created by garyjacobs on 11/24/17.
 */
class WeatherMainActivity : WeatherActivity() {
    private val inboundMessenger = Messenger(InboundHandler())
    private lateinit var outboundMessenger: Messenger
    private var locaterServiceBound = false
    private var loadCurrentWeather = true
    private var isTwoPane = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        location_cb.setOnClickListener(locationClickListener)
        val view = layoutInflater.inflate(R.layout.weather_panel_layout, null)
        main_frame_layout.addView(view)
        isTwoPane = view.extended_weather_container != null
    }

    val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            supportActionBar?.title = "SERVICE DISCONNECTED"
            locaterServiceBound = false
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            outboundMessenger = Messenger(service)
            outboundMessenger.sendMessage(LocaterService.REQUESTCURRENTWEATHERCURRENTLOCATION)
            locaterServiceBound = true
        }
    }
    private val locationClickListener = object : View.OnClickListener {
        override fun onClick(v: View?) {
            val comboBox = v as ComboBox
            comboBox.getCurrentText()?.let {
                if (it.isNotEmpty() && it.isNotBlank()) {
                    val bundle = Bundle()
                    bundle.putString("LOCATION", it)
                    if (!loadCurrentWeather) {
                        outboundMessenger.sendMessage(LocaterService.REQUESTLOCATION, bundle)
                    } else {
                        outboundMessenger.sendMessage(LocaterService.REQUESTCURRENTWEATHERWITHLOCATION, bundle)
                    }

                } else {
                    if (!loadCurrentWeather) {
                        outboundMessenger.sendMessage(LocaterService.REQUESTCURRENTLOCATION)
                    } else {
                        outboundMessenger.sendMessage(LocaterService.REQUESTCURRENTWEATHERCURRENTLOCATION)
                    }

                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), 100)
        } else {
            val intent = Intent(this, LocaterService::class.java)
            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        val intent = Intent(this, LocaterService::class.java)
        unbindService(serviceConnection)
    }

    inner class InboundHandler : Handler() {
        override fun handleMessage(message: Message?) {
            message?.let {
                when (it.what) {
                    LocaterService.CONNECTING -> {
                        supportActionBar?.title = "Connecting..."
                        //outboundMessenger.sendMessage(LocaterService.REQUESTCURRENTLOCATION)
                        outboundMessenger.sendMessage(LocaterService.REQUESTCURRENTWEATHERCURRENTLOCATION)
                    }
                    LocaterService.REQUESTEDCURRENTLOCATION -> {
                        progress_bar.visibility = View.INVISIBLE
                        location_cb.visibility = View.VISIBLE
                        val addresses = message.data.get("LOCATION") as Array<Address>
                        supportActionBar?.title = addresses[0].formatAddress()
                        if (message.data.get("STATUS") as Boolean) {
                            loadWeatherListFragment()
                        } else {
                            Toast.makeText(this@WeatherMainActivity, message.data.get("ERROR") as String, Toast.LENGTH_LONG)
                        }
                    }
                    LocaterService.REQUESTEDLOCATION -> {
                        val addresses = message.data.get("LOCATION") as Array<Address>
                        if (addresses.size == 1) {
                            supportActionBar?.title = addresses[0].formatAddress()
                            if (message.data.get("STATUS") as Boolean) {
                                loadWeatherListFragment()
                            } else {
                                Toast.makeText(this@WeatherMainActivity, message.data.get("ERROR") as String, Toast.LENGTH_LONG)
                            }
                        } else {
                            location_cb.updateComboBoxSelections(addresses)
                        }
                    }
                    LocaterService.REQUESTEDCURRENTWEATHERCURRENTLOCATION -> {
                        progress_bar.visibility = View.INVISIBLE
                        location_cb.visibility = View.VISIBLE
                        val addresses = message.data.get("LOCATION") as Array<Address>
                        supportActionBar?.title = addresses[0].formatAddress()
                        if (message.data.get("STATUS") as Boolean) {
                            loadCurrentWeatherFragment()
                        } else {
                            Toast.makeText(this@WeatherMainActivity, message.data.get("ERROR") as String, Toast.LENGTH_LONG)
                        }
                    }
                    LocaterService.REQUESTEDCURRENTWEATHERWITHLOCATION -> {
                        val addresses = message.data.get("LOCATION") as Array<Address>
                        if (addresses.size == 1) {
                            supportActionBar?.title = addresses[0].formatAddress()
                            if (message.data.get("STATUS") as Boolean) {
                                loadCurrentWeatherFragment()
                            } else {
                                Toast.makeText(this@WeatherMainActivity, message.data.get("ERROR") as String, Toast.LENGTH_LONG)
                            }
                        } else {
                            location_cb.updateComboBoxSelections(addresses)
                        }
                    }
                    else -> {
                    }
                }

            }
        }
    }

    private fun loadCurrentWeatherFragment() {

//        var currentWeatherFragment = fragmentManager.findFragmentByTag(CurrentWeatherFragment.TAG)
//        if (currentWeatherFragment == null || createdNewCurrentWeatherFrag) {
//            currentWeatherFragment = CurrentWeatherFragment()
//        }

        loadFragment(CurrentWeatherFragment(), CurrentWeatherFragment.TAG, R.id.weather_container, false)

    }

    private fun loadWeatherListFragment() {
//        var weatherListFragment = fragmentManager.findFragmentByTag(WeatherListFragment.TAG)
//        if (weatherListFragment == null) {
//            weatherListFragment = WeatherListFragment.getInstance()
//        }
        var containerID = R.id.weather_container
        if (isTwoPane) {
            loadFragment(WeatherListFragment(), WeatherListFragment.TAG, R.id.extended_weather_container, false)
        } else {
            loadFragment(WeatherListFragment(), WeatherListFragment.TAG, R.id.weather_container, true)
        }
    }

    private fun loadFragment(fragment: Fragment, tag: String, containerID: Int, addToBackStack: Boolean = false) {
        val fragTM: FragmentTransaction = fragmentManager.beginTransaction()
        fragTM.replace(containerID, fragment, tag)
        if (addToBackStack) {
            fragTM.addToBackStack(tag)
        }
        fragTM.commit()
    }


    fun Messenger.sendMessage(what: Int, bundle: Bundle? = null, outMessenger: Messenger = outboundMessenger, inMessenger: Messenger = inboundMessenger) {
        val outboundmessage = Message.obtain()
        outboundmessage.what = what
        outboundmessage.replyTo = inMessenger
        bundle?.let { outboundmessage.data = bundle }
        try {
            outMessenger.send(outboundmessage)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        weatherApplication.bus.register(this)
    }

    override fun onPause() {
        super.onPause()
        weatherApplication.bus.unregister(this)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        loadCurrentWeather = true
    }
    // get selections from list
    //@Subscribe
    //fun ForcastSelected(event: ForecastListSelectedEvent) = LoadForecastDetailsFragment(event.itemSelected)

    // load list
    @Subscribe
    fun CurrentWeatherSelected(event: CurrentWeatherSelectedEvent) {
        loadCurrentWeather = false
        progress_bar.visibility = View.VISIBLE
        location_cb.callOnClick()
    }
}