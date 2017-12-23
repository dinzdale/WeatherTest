package com.garyjacobs.weathertest


import Events.ForecastListSelectedEvent
import android.os.Bundle
import android.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.support.v7.widget.RecyclerView
import android.widget.ImageView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import model.Forecast
import kotlinx.android.synthetic.main.weather_list.*

/**
 * A simple [Fragment] subclass.
 */
class WeatherListFragment : Fragment() {


    private lateinit var myActivity: WeatherActivity


    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater!!.inflate(R.layout.weather_list, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        myActivity = activity as WeatherActivity
        weather_list!!.layoutManager = LinearLayoutManager(activity,LinearLayoutManager.HORIZONTAL,false)
        val myRecyclerViewAdapter = MyRecyclerViewAdapter(myActivity.weatherApplication.forecast!!, View.OnClickListener { v -> onItemClick(weather_list!!.getChildAdapterPosition(v)) })
        weather_list!!.adapter = myRecyclerViewAdapter
    }

    private inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tempDay: TextView
        var humidity: TextView
        var description: TextView
        var weatherIcon: ImageView
        var dayOfWeek_Month_DayOfMonth: TextView


        init {
            tempDay = itemView.findViewById(R.id.temp_day) as TextView
            humidity = itemView.findViewById(R.id.humidity) as TextView
            description = itemView.findViewById(R.id.description) as TextView
            weatherIcon = itemView.findViewById(R.id.weather_icon) as ImageView
            dayOfWeek_Month_DayOfMonth = itemView.findViewById(R.id.dayofweek_month_dayofmonth) as TextView

        }


    }

    private fun onItemClick(position: Int) = myActivity!!.weatherApplication.bus.post(ForecastListSelectedEvent(position))


    private inner class MyRecyclerViewAdapter(internal var forecast: Forecast, internal var onClickListener: View.OnClickListener) : RecyclerView.Adapter<MyViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, i: Int): MyViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.weather_list_item, parent, false)
            view.setOnClickListener(onClickListener)
            return MyViewHolder(view)
        }

        override fun onBindViewHolder(myViewHolder: MyViewHolder, position: Int) {
            val forecastDetailsList = forecast.list
            myViewHolder.tempDay.text = forecastDetailsList[position].temp.day.toInt().toString()
            myViewHolder.humidity.text = forecastDetailsList[position].humidity.toInt().toString()
            myViewHolder.description.text = forecastDetailsList[position].weather[0].description
            // set weekday

            val calendar = Calendar.getInstance()
            var date = calendar.time
            val newTimeMillis = date.time + position * 24 * 60 * 60 * 1000
            calendar.timeInMillis = newTimeMillis
            date = calendar.time
            val simpleDateFormat = SimpleDateFormat(getString(R.string.dayofweek_month_dayofmonth))
            myViewHolder.dayOfWeek_Month_DayOfMonth.text = simpleDateFormat.format(date)
            myActivity!!.weatherApplication.imageManager.setImage(forecastDetailsList[position].weather[0].icon, myViewHolder.weatherIcon)
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
