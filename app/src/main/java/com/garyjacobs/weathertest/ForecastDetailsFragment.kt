package com.garyjacobs.weathertest


import android.os.Bundle
import android.app.Fragment
import android.content.Context
import android.location.Address
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.forecast_details.map
import kotlinx.android.synthetic.main.forecast_details.temperature_values
import model.Forecast
import model.ForecastDetails
import model.Temperature
import model.Weather


/**
 * A simple [Fragment] subclass.
 * Use the [ForecastDetailsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ForecastDetailsFragment : Fragment() {

    private lateinit var myActivity: WeatherActivity
    private lateinit var location: Address
    private var index: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        myActivity = activity as WeatherActivity
        arguments?.let {
            index = arguments.getInt(ARG_INDEX)
            location = arguments.getParcelable<Address>("LOCATION")
        }
    }


    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {

        return inflater!!.inflate(R.layout.forecast_details, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        val forecast = myActivity.weatherApplication.forecast
        val temperature = forecast!!.list[index].temp
        temperature_values.text = getString(R.string.temprature_string, temperature.min, temperature.max, temperature.day, temperature.night, temperature.eve, temperature.morn)
        map.onCreate(savedInstanceState)
        map.getMapAsync(mapReadyImp)
    }

    val mapReadyImp = object : OnMapReadyCallback {
        override fun onMapReady(googleMap: GoogleMap?) {
            googleMap?.let {
                val latlon = LatLng(location.latitude, location.longitude)
                it.moveCamera(CameraUpdateFactory.newLatLngZoom(latlon, 10.toFloat()))
                it.addMarker(MarkerOptions()
                        .position(latlon))
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        map?.onSaveInstanceState(outState)
    }

    override fun onPause() {
        super.onPause()
        map?.onPause()
    }

    override fun onResume() {
        super.onResume()
        map?.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        map?.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        map?.onLowMemory()
    }

    companion object {

        private val ARG_INDEX = "index"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment ForecastDetailsFragment.
         */
        // TODO: Rename and change types and number of parameters
        fun newInstance(index: Int, location: Address): ForecastDetailsFragment {
            val fragment = ForecastDetailsFragment()
            val args = Bundle()
            args.putInt(ARG_INDEX, index)
            args.putParcelable("LOCATION", location)
            fragment.arguments = args
            return fragment
        }
    }


}// Required empty public constructor
