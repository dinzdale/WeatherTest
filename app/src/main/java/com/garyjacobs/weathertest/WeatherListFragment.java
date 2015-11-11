package com.garyjacobs.weathertest;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v7.widget.RecyclerView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import Events.ForecastListSelected;
import model.Forecast;
import model.ForecastDetails;

/**
 * A simple {@link Fragment} subclass.
 */
public class WeatherListFragment extends Fragment {

    private RecyclerView recyclerView;
    private WeatherActivity myActivity;

    public WeatherListFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myActivity = (WeatherActivity)getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        ViewGroup viewGroup = (ViewGroup) inflater.inflate(R.layout.weather_list, container, false);
        recyclerView = (RecyclerView) viewGroup.findViewById(R.id.weather_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        MyRecyclerViewAdapter myRecyclerViewAdapter = new MyRecyclerViewAdapter(((WeatherTestApplication) getActivity().getApplication()).getForecast(), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemClick(recyclerView.getChildAdapterPosition(v));
            }
        });
        recyclerView.setAdapter(myRecyclerViewAdapter);

        return viewGroup;
    }

    private class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView tempDay;
        public TextView humidity;
        public TextView description;
        public ImageView weatherIcon;

        public MyViewHolder(View itemView) {
            super(itemView);
            tempDay = (TextView) itemView.findViewById(R.id.temp_day);
            humidity = (TextView) itemView.findViewById(R.id.humidity);
            description = (TextView) itemView.findViewById(R.id.description);
            weatherIcon = (ImageView) itemView.findViewById(R.id.weather_icon);

        }


    }

    private void onItemClick(int position) {
        myActivity.getWeatherApplication().getBus().post(new ForecastListSelected(position));
    }

    private class MyRecyclerViewAdapter extends RecyclerView.Adapter<MyViewHolder> {
        Forecast forecast;
        View.OnClickListener onClickListener;

        public MyRecyclerViewAdapter(Forecast forecast, View.OnClickListener onClickListener) {
            this.forecast = forecast;
            this.onClickListener = onClickListener;

        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int i) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.weather_list_item, parent, false);
            view.setOnClickListener(onClickListener);
            MyViewHolder viewHolder = new MyViewHolder(view);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(MyViewHolder myViewHolder, int position) {
            List<ForecastDetails> forecastDetailsList = forecast.getList();
            myViewHolder.tempDay.setText(Float.toString(forecastDetailsList.get(position).getTemp().getDay()));
            myViewHolder.humidity.setText(Float.toString(forecastDetailsList.get(position).getHumidity()));
            myViewHolder.description.setText(forecastDetailsList.get(position).getWeather().get(0).getDescription());
            myActivity.getWeatherApplication().getImageManager().setImage(forecastDetailsList.get(position).getWeather().get(0).getIcon(), myViewHolder.weatherIcon);

        }

        @Override
        public int getItemCount() {
            return forecast.getCnt();
        }


    }
}
