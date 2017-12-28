package com.garyjacobs.weathertest

import Events.CurrentWeatherSelectedEvent
import android.Manifest
import android.app.AlertDialog
import android.app.Fragment
import android.app.FragmentTransaction
import android.app.ProgressDialog
import android.content.*
import android.content.pm.PackageManager
import android.location.Address
import android.os.*
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.View
import com.squareup.otto.Subscribe
import kotlinx.android.synthetic.main.activity_main.*
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
    private lateinit var pleaseWaitDialog: ProgressDialog
    private lateinit var networkWarning: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        location_cb.setOnClickListener(locationClickListener)
        val view = layoutInflater.inflate(R.layout.weather_panel_layout, null)
        main_frame_layout.addView(view)
        isTwoPane = view.extended_weather_container != null
        pleaseWaitDialog = ProgressDialog(this)
        pleaseWaitDialog.setCancelable(false)
        pleaseWaitDialog.setMessage(getString(R.string.please_wait))
        networkWarning = getNetworkConnectionWarning(listener = object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                pleaseWaitDialog.show()
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
            outboundMessenger.sendMessage(LocaterService.REQUESTCURRENTWEATHERCURRENTLOCATION)
            locaterServiceBound = true
        }
    }
    private val locationClickListener = object : View.OnClickListener {
        override fun onClick(v: View?) {
            val comboBox = v as ComboBox
            comboBox.getCurrentText()?.let {
                pleaseWaitDialog.show()
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
                    LocaterService.NOINTERENT -> {
                        if (pleaseWaitDialog.isShowing) pleaseWaitDialog.hide()
                        networkWarning.show()
                    }
                    LocaterService.CONNECTING -> {
                        supportActionBar?.title = "Connecting..."
                        outboundMessenger.sendMessage(LocaterService.REQUESTCURRENTWEATHERCURRENTLOCATION)
                    }
                    LocaterService.REQUESTEDCURRENTLOCATION -> {
                        pleaseWaitDialog.hide()
                        location_cb.visibility = View.VISIBLE
                        if (message.data.getBoolean("STATUS")) {
                            val addresses = message.data.get("LOCATION") as Array<Address>
                            supportActionBar?.title = addresses[0].formatAddress()
                            loadAllFragments()
                        } else {
                            showErrorDialog(message.data.getString("ERROR"))
                        }
                    }
                    LocaterService.REQUESTEDLOCATION -> {
                        pleaseWaitDialog.hide()
                        location_cb.visibility = View.VISIBLE
                        if (message.data.getBoolean("STATUS")) {
                            val addresses = message.data.get("LOCATION") as Array<Address>
                            if (addresses.size == 1) {
                                supportActionBar?.title = addresses[0].formatAddress()
                                loadAllFragments()
                            } else {
                                location_cb.updateComboBoxSelections(addresses)
                            }
                        } else {
                            showErrorDialog(message.data.getString("ERROR"))
                        }
                    }
                    LocaterService.REQUESTEDCURRENTWEATHERCURRENTLOCATION -> {
                        pleaseWaitDialog.hide()
                        location_cb.visibility = View.VISIBLE
                        if (message.data.getBoolean("STATUS")) {
                            val addresses = message.data.get("LOCATION") as Array<Address>
                            supportActionBar?.title = addresses[0].formatAddress()
                            loadAllFragments()
                        } else {
                            showErrorDialog(message.data.getString("ERROR"))
                        }
                    }
                    LocaterService.REQUESTEDCURRENTWEATHERWITHLOCATION -> {
                        pleaseWaitDialog.hide()
                        location_cb.visibility = View.VISIBLE
                        if (message.data.getBoolean("STATUS")) {
                            val addresses = message.data.get("LOCATION") as Array<Address>
                            if (addresses.size == 1) {
                                supportActionBar?.title = addresses[0].formatAddress()
                                loadAllFragments()
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

    private fun loadAllFragments() {
        val fragTM: FragmentTransaction = fragmentManager.beginTransaction()
        if (isTwoPane) {
            if (loadCurrentWeather) {
                location_cb.visibility = View.VISIBLE
                fragTM.replace(R.id.weather_container, CurrentWeatherFragment(), CurrentWeatherFragment.TAG)
                        .commit()
            } else {
                location_cb.visibility = View.GONE
                fragTM.replace(R.id.extended_weather_container, WeatherListFragment(), WeatherListFragment.TAG)
                        .addToBackStack(WeatherListFragment.TAG)
                        .commit()
            }
        } else {
            if (loadCurrentWeather) {
                location_cb.visibility = View.VISIBLE
                fragTM.replace(R.id.weather_container, CurrentWeatherFragment(), CurrentWeatherFragment.TAG)
                        .commit()
            } else {
                location_cb.visibility = View.GONE
                fragTM.replace(R.id.weather_container, WeatherListFragment(), WeatherListFragment.TAG)
                        .addToBackStack(WeatherListFragment.TAG)
                        .commit()
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
        pleaseWaitDialog.show()
    }

    override fun onPause() {
        super.onPause()
        weatherApplication.bus.unregister(this)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        location_cb.visibility = View.VISIBLE
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
    // get selections from list
    //@Subscribe
    //fun ForcastSelected(event: ForecastListSelectedEvent) = LoadForecastDetailsFragment(event.itemSelected)

    // load list
    @Subscribe
    fun CurrentWeatherSelected(event: CurrentWeatherSelectedEvent) {
        loadCurrentWeather = false
        pleaseWaitDialog.show()
        location_cb.callOnClick()
    }
}