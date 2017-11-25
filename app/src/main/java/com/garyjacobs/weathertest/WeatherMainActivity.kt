package com.garyjacobs.weathertest

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.*
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.weather_panel_layout.view.*
import widgets.ComboBox

/**
 * Created by garyjacobs on 11/24/17.
 */
class WeatherMainActivity : WeatherActivity() {
    private val inboundMessenger = Messenger(InboundHandler())
    private lateinit var outboundMessenger: Messenger
    private var locaterServiceBound = false
    private var isTwoPane = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        location_cb.setClientClickListener(locationClickListener)
        val view = layoutInflater.inflate(R.layout.weather_panel_layout, null)
        main_frame_layout.addView(view)
        isTwoPane = view.weather_details_container == null
    }

    val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            supportActionBar?.title = "SERVICE DISCONNECTED"
            locaterServiceBound = false
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            outboundMessenger = Messenger(service)
            outboundMessenger.sendMessage(LocaterService.REQUESTCURRENTLOCATION)
            locaterServiceBound = true
        }
    }
    private val locationClickListener = object : View.OnClickListener {
        override fun onClick(v: View?) {
            val comboBox = v as ComboBox
            comboBox.getCurrentText()?.let {
                if (it.isNotEmpty()) {
                    val bundle = Bundle()
                    bundle.putString("LOCATION", it)
                    outboundMessenger.sendMessage(LocaterService.REQUESTLOCATION, bundle)
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
                        outboundMessenger.sendMessage(LocaterService.REQUESTCURRENTLOCATION)
                    }
                    LocaterService.REQUESTEDCURRENTLOCATION -> {
                        val address = message.data.get("LOCATION") as Address
                        supportActionBar?.title = address.locality
                        progress_bar.visibility = View.INVISIBLE
                        location_cb.visibility = View.VISIBLE
                    }
                    LocaterService.REQUESTEDLOCATION -> {
                        val address = message.data.get("LOCATION") as Address
                        supportActionBar?.title = "${address.locality} ${address.postalCode}"
                    }
                }
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
}