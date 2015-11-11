package com.garyjacobs.weathertest;


import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import model.Forecast;
import model.ForecastDetails;
import model.Temperature;
import model.Weather;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ForecastDetailsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ForecastDetailsFragment extends Fragment {

    private static final String ARG_INDEX = "index";

    private WeatherActivity myActivity;

    int index;
    TextView temperature_values;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ForecastDetailsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ForecastDetailsFragment newInstance(int index) {
        ForecastDetailsFragment fragment = new ForecastDetailsFragment();
        Bundle args = new Bundle();

        args.putInt(ARG_INDEX, index);
        fragment.setArguments(args);
        return fragment;
    }

    public ForecastDetailsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myActivity = (WeatherActivity) getActivity();
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_INDEX);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        ViewGroup viewGroup = (ViewGroup) inflater.inflate(R.layout.forecast_details, container, false);
        temperature_values = (TextView) viewGroup.findViewById(R.id.temperature_values);
        Forecast forecast = ((WeatherTestApplication) getActivity().getApplication()).getForecast();
        Temperature temperature = forecast.getList().get(index).getTemp();
        temperature_values.setText(getString(R.string.temprature_string, temperature.getMin(), temperature.getMax(), temperature.getDay(), temperature.getNight(), temperature.getEve(), temperature.getMorn()));
        return viewGroup;
    }


}
