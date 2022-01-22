package com.garyjacobs.weathertest


import Events.ForecastListSelectedEvent
import Events.getLongPressObservable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.gms.maps.model.UrlTileProvider
import kotlinx.android.synthetic.main.weather_list.*
import java.text.SimpleDateFormat
import java.util.Calendar
import model.Forecast
import kotlinx.android.synthetic.main.weather_list_item.view.*
import model.getWindDirection
import widgets.SlideMotion
import widgets.doSlideAnimation
import java.net.URL

/**
 * A simple [Fragment] subclass.
 */
class WeatherListFragment : Fragment() {


    private lateinit var myActivity: WeatherActivity
    var lat: Double = 0.0
    var lon: Double = 0.0

    companion object {
        val TAG = WeatherListFragment::class.java.simpleName
        var me: WeatherListFragment? = null
        fun getInstance(lat: Double, lon: Double): WeatherListFragment {
            val bundle = Bundle()
            bundle.putDouble("lat", lat)
            bundle.putDouble("lon", lon)
            me = WeatherListFragment()
            me!!.arguments = bundle
            return me!!
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.weather_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        myActivity = activity as WeatherActivity
        arguments?.let {
            lat = it.getDouble("lat")
            lon = it.getDouble("lon")
        }
//        savedInstanceState?.let {
//            lat = it.getDouble("lat")
//            lon = it.getDouble("lon")
//        }
        weather_list!!.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        val myRecyclerViewAdapter = MyRecyclerViewAdapter(myActivity.weatherApplication.forecast!!, View.OnClickListener { v -> onItemClick(weather_list!!.getChildAdapterPosition(v)) })
        weather_list!!.adapter = myRecyclerViewAdapter
        // handle map
        // setup up map in background
        extended_map.onCreate(savedInstanceState)
        extended_map.getMapAsync(object : OnMapReadyCallback {

            override fun onMapReady(googleMap: GoogleMap) {
                googleMap.let {
                    val latlon = LatLng(lat, lon)
                    it.moveCamera(CameraUpdateFactory.newLatLngZoom(latlon, 0.toFloat()))
                    it.addMarker(MarkerOptions()
                            .position(latlon))
                    val tileOverlayOptions = TileOverlayOptions()
                            .tileProvider(object : UrlTileProvider(256, 256) {
                                override fun getTileUrl(x: Int, y: Int, zoom: Int): URL {
                                    return URL(getString(R.string.openweathermap_tile_url, "temp_new", zoom, x, y, getString(R.string.openweathermap_appid)))
                                }
                            })
                    // hold off ... to many api calls
                    it.addTileOverlay(tileOverlayOptions)
                    googleMap.setOnMapClickListener {
                        if (weather_list.visibility != View.VISIBLE)
                            doSlideAnimation(weather_list, SlideMotion.SLIDEINDOWNRIGHT)
                    }
                }
            }
        })

        getLongPressObservable(weather_list)
                .subscribe {
                    doSlideAnimation(weather_list, SlideMotion.SLIDEOUTUPLEFT)
                }
    }


    override fun onStart() {
        super.onStart()
        extended_map?.onStart()
    }

    override fun onResume() {
        super.onResume()
        extended_map?.onResume()
    }

    override fun onPause() {
        super.onPause()
        extended_map?.onPause()
    }

    override fun onStop() {
        super.onStop()
        extended_map?.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        extended_map?.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        extended_map?.onLowMemory()
    }

    private inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var date = itemView.forecast_date
        var description = itemView.forecast_description
        var tempMorn = itemView.temp_morn
        var tempDay = itemView.temp_day
        var tempNight = itemView.temp_night
        var tempMax = itemView.temp_max
        var tempMin = itemView.temp_min
        var humidity = itemView.weather_humidity
        var wind = itemView.forecast_wind
        var weatherIcon = itemView.weather_icon

    }

    private fun onItemClick(position: Int) = myActivity.weatherApplication.bus.post(ForecastListSelectedEvent(position))


    private inner class MyRecyclerViewAdapter(internal var forecast: Forecast, internal var onClickListener: View.OnClickListener) : RecyclerView.Adapter<MyViewHolder>() {


        override fun onCreateViewHolder(parent: ViewGroup, i: Int): MyViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.weather_list_item, parent, false)
            view.setOnClickListener(onClickListener)
            return MyViewHolder(view)
        }

        override fun onBindViewHolder(myViewHolder: MyViewHolder, position: Int) {
            val forecastDetails = forecast.list[position]
            val weather = forecastDetails.weather[0]
            val temp = forecastDetails.temp

            // calculate date
            val calendar = Calendar.getInstance()
            var date = calendar.time
            val newTimeMillis = date.time + position * 24 * 60 * 60 * 1000
            calendar.timeInMillis = newTimeMillis
            date = calendar.time
            val simpleDateFormat = SimpleDateFormat(getString(R.string.dayofweek_month_dayofmonth))
            myViewHolder.date.text = simpleDateFormat.format(date)
            myViewHolder.description.text = weather.description
            myViewHolder.tempMorn.text = getString(R.string.temp_morn, temp.morn.toInt())
            myViewHolder.tempDay.text = getString(R.string.temp_day, temp.day.toInt())
            myViewHolder.tempNight.text = getString(R.string.temp_night, temp.night.toInt())
            myViewHolder.tempMax.text = getString(R.string.temp_max, temp.max.toInt())
            myViewHolder.tempMin.text = getString(R.string.temp_min, temp.min.toInt())
            myViewHolder.humidity.text = getString(R.string.humidity, forecastDetails.humidity)
            myViewHolder.wind.text = getString(R.string.current_wind, forecastDetails.speed.toInt(), getWindDirection(forecastDetails.deg))
            myActivity.weatherApplication.imageManager.setImage(weather.icon, myViewHolder.weatherIcon)
            //            String url = getString(R.string.openweathermap_base_url) + "/img/w/" + forecastDetailsList.get(position).getWeather().get(0).getIcon();
            //            Picasso.with(myActivity)
            //                    .load(url)
            //                    .networkPolicy(NetworkPolicy.NO_CACHE)
            //                    .into(myViewHolder.weatherIcon);
        }

        override fun getItemCount(): Int {
            return forecast.cnt
        }


    }
}// Required empty public constructor
