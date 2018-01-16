package com.garyjacobs.weathertest

import Events.*
import android.Manifest
import android.app.AlertDialog
import android.app.Fragment
import android.app.FragmentTransaction
import android.app.ProgressDialog
import android.arch.persistence.room.Room
import android.content.*
import android.content.pm.PackageManager
import android.location.Address
import android.os.*
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.Toast
import com.squareup.otto.Subscribe
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.weather_panel_layout.view.*
import model.ArchComps.WeatherDB
import model.CurrentWeather
import model.formatAddress
import widgets.ComboBox
import widgets.SlideMotion
import widgets.doSlideAnimation

/**
 * Created by garyjacobs on 11/24/17.
 */
class WeatherMainActivity : WeatherActivity() {
    private val TAG = WeatherMainActivity::class.java.simpleName
    private val inboundMessenger = Messenger(InboundHandler())
    private lateinit var outboundMessenger: Messenger
    private var locaterServiceBound = false
    private var loadCurrentWeather = true
    private var isTwoPane = false
    private lateinit var networkWarning: AlertDialog
    private var currentLat: Double = 0.0
    private var currentLon: Double = 0.0
    private var onRestore = false;
    private var currentTitle: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onRestore = savedInstanceState?.let {
            currentLat = savedInstanceState.getDouble("lat")
            currentLon = savedInstanceState.getDouble("lon")
            loadCurrentWeather = savedInstanceState.getBoolean("loadcurrentweather")
            currentTitle = savedInstanceState.getString("title")
            true
        } ?: false

        setContentView(R.layout.activity_main)
        location_cb.setOnClickListener(locationClickListener)
        if (onRestore && !loadCurrentWeather) {
            location_cb.visibility = View.GONE
        }
        location_cb.UserFlingAction = {
            doSlideAnimation(location_cb, SlideMotion.SLIDEOUTUPLEFT)
        }
        val view = layoutInflater.inflate(R.layout.weather_panel_layout, null)
        main_frame_layout.addView(view)
        isTwoPane = view.extended_weather_container != null
        networkWarning = getNetworkConnectionWarning(listener = object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                val bundle = Bundle()
                bundle.putDouble("lat", currentLat)
                bundle.putDouble("lon", currentLon)
                outboundMessenger.sendMessage(LocaterService.REQUESTCURRENTWEATHERCURRENTLOCATION)
            }
        })

    }

    val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            supportActionBar?.title = "SERVICE DISCONNECTED"
            locaterServiceBound = false
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            outboundMessenger = Messenger(service)
            locaterServiceBound = true
            if (!onRestore) {
                val bundle = Bundle()
                bundle.putDouble("lat", currentLat)
                bundle.putDouble("lon", currentLon)
                outboundMessenger.sendMessage(LocaterService.REQUESTCURRENTWEATHERCURRENTLOCATION, bundle)
            } else {
                title = currentTitle
            }
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
                    currentLat = 0.0
                    currentLon = 0.0
                    val bundle = Bundle()
                    bundle.putDouble("lat", currentLat)
                    bundle.putDouble("lon", currentLon)
                    if (!loadCurrentWeather) {
                        outboundMessenger.sendMessage(LocaterService.REQUESTCURRENTLOCATION)
                    } else {
                        outboundMessenger.sendMessage(LocaterService.REQUESTCURRENTWEATHERCURRENTLOCATION, bundle)
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
        unbindService(serviceConnection)
    }

    inner class InboundHandler : Handler() {
        override fun handleMessage(message: Message?) {
            message?.let {
                when (it.what) {
                    LocaterService.NOINTERENT -> {
                        networkWarning.show()
                    }
                    LocaterService.CONNECTING -> {
                        supportActionBar?.title = "Connecting..."
                        outboundMessenger.sendMessage(LocaterService.REQUESTCURRENTWEATHERCURRENTLOCATION)
                    }
                    LocaterService.REQUESTEDCURRENTLOCATION -> {
                        if (message.data.getBoolean("STATUS")) {
                            currentTitle = message.data.getString("title")
                            supportActionBar?.title = currentTitle
                            currentLat = message.data.getDouble("lat")
                            currentLon = message.data.getDouble("lon")
                            loadAllFragments(currentLat, currentLon)
                        } else {
                            showErrorDialog(message.data.getString("ERROR"))
                        }
                    }
                    LocaterService.REQUESTEDLOCATION -> {
                        if (message.data.getBoolean("STATUS")) {
                            val addresses = message.data.get("LOCATION") as Array<Address>
                            if (addresses.size == 1) {
                                currentTitle = message.data.getString("title")
                                supportActionBar?.title = currentTitle
                                currentLat = message.data.getDouble("lat")
                                currentLon = message.data.getDouble("lon")
                                loadAllFragments(currentLat, currentLon)
                            } else {
                                location_cb.updateComboBoxSelections(addresses)
                            }
                        } else {
                            showErrorDialog(message.data.getString("ERROR"))
                        }
                    }
                    LocaterService.REQUESTEDCURRENTWEATHERCURRENTLOCATION -> {
                        if (message.data.getBoolean("STATUS")) {
                            currentTitle = message.data.getString("title")
                            supportActionBar?.title = currentTitle
                            currentLat = message.data.getDouble("lat")
                            currentLon = message.data.getDouble("lon")
                            loadAllFragments(currentLat, currentLon)
                        } else {
                            showErrorDialog(message.data.getString("ERROR"))
                        }
                    }
                    LocaterService.REQUESTEDCURRENTWEATHERWITHLOCATION -> {
                        if (message.data.getBoolean("STATUS") && message.data.get("LOCATION") != null) {
                            val addresses = message.data.get("LOCATION") as Array<Address>
                            if (addresses.size == 1) {
                                currentTitle = message.data.getString("title")
                                supportActionBar?.title = currentTitle
                                currentLat = message.data.getDouble("lat")
                                currentLon = message.data.getDouble("lon")
                                loadAllFragments(currentLat, currentLon)
                            } else {
                                location_cb.updateComboBoxSelections(addresses)
                            }
                        } else {
                            showErrorDialog(message.data.getString("ERROR"))
                        }
                    }
                    else -> {
                    }
                }

            }
        }
    }

    override fun onAttachFragment(fragment: android.support.v4.app.Fragment?) {
        super.onAttachFragment(fragment)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.let {
            outState.putDouble("lat", currentLat)
            outState.putDouble("lon", currentLon)
            outState.putString("title", currentTitle)
            outState.putBoolean("loadcurrentweather", loadCurrentWeather)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        onRestore = true
    }

    private fun loadAllFragments(lat: Double = 0.0, lon: Double = 0.0) {
        val fragTM = supportFragmentManager.beginTransaction()
        if (isTwoPane) {
            if (loadCurrentWeather) {
                if (location_cb.visibility != View.VISIBLE) {
                    doSlideAnimation(location_cb, SlideMotion.SLIDEDOWNIN, {
                        fragTM.replace(R.id.weather_container, CurrentWeatherFragment.getInstance(lat, lon), CurrentWeatherFragment.TAG)
                                .commit()
                    })
                } else {
                    fragTM.replace(R.id.weather_container, CurrentWeatherFragment.getInstance(lat, lon), CurrentWeatherFragment.TAG)
                            .commit()
                }
            } else {
                doSlideAnimation(location_cb, SlideMotion.SLIDEUPOUT, {
                    fragTM.replace(R.id.extended_weather_container, WeatherListFragment.getInstance(lat, lon), WeatherListFragment.TAG)
                            .addToBackStack(WeatherListFragment.TAG)
                            .commit()
                })
            }
        } else {
            if (loadCurrentWeather) {
                if (location_cb.visibility != View.VISIBLE) {
                    doSlideAnimation(location_cb, SlideMotion.SLIDEDOWNIN, {
                        fragTM.replace(R.id.weather_container, CurrentWeatherFragment.getInstance(lat, lon), CurrentWeatherFragment.TAG)
                                .commit()
                    })
                } else {
                    fragTM.replace(R.id.weather_container, CurrentWeatherFragment.getInstance(lat, lon), CurrentWeatherFragment.TAG)
                            .commit()
                }
            } else {
                doSlideAnimation(location_cb, SlideMotion.SLIDEUPOUT, {
                    fragTM.replace(R.id.weather_container, WeatherListFragment.getInstance(lat, lon), WeatherListFragment.TAG)
                            .addToBackStack(WeatherListFragment.TAG)
                            .commit()
                })
            }
        }
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
        if (location_cb.visibility != View.VISIBLE) {
            doSlideAnimation(location_cb, SlideMotion.SLIDEDOWNIN)
        }
        loadCurrentWeather = true
    }


    private fun getNetworkConnectionWarning(message: String? = getString(R.string.no_network_warning), listener: DialogInterface.OnClickListener? = null): AlertDialog {
        return AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, listener)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .create()
    }

    private fun getErrorDialog(message: String? = getString(R.string.default_error_dialog_msg), listener: DialogInterface.OnClickListener? = null): AlertDialog {
        return AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, listener)
                .setIcon(android.R.drawable.stat_notify_error)
                .create()


    }

    private fun showErrorDialog(message: String?) {
        val dialog = message?.let {
            getErrorDialog(it)
        } ?: getErrorDialog()
        dialog.show()
    }

    // load list
    @Subscribe
    fun CurrentWeatherSelected(event: CurrentWeatherSelectedEvent) {
        loadCurrentWeather = false
        location_cb.callOnClick()
    }

    @Subscribe
    fun MapClicked(event: MapClickedEvent) {
        if (location_cb.visibility != View.VISIBLE) {
            doSlideAnimation(location_cb, SlideMotion.SLIDEINDOWNRIGHT)
        }
    }
}