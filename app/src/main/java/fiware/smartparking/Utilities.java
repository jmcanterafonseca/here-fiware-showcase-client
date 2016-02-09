package fiware.smartparking;

import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.mapping.MapMarker;

import java.io.IOException;
import java.util.Map;

/**
 *  Utilities class
 *
 *
 */
public class Utilities {
    public static class AirQualityData {
        public String asString;
        public java.util.Map worstIndex;
    }

    public static AirQualityData getAirQualityData(java.util.Map<String, java.util.Map> result) {
        AirQualityData out = new AirQualityData();

        int maxIndex = -1;
        // Lets see what is the greatest AQI and then paint accordingly
        java.util.Map<String,Object> targetAqi = null;

        StringBuffer markerOut = new StringBuffer();
        for (String aqiInfo : result.keySet()) {
            java.util.Map<String,Object> indexData = result.get(aqiInfo);

            int value = (Integer)indexData.get("value");
            String levelName = (String)indexData.get("name");

            markerOut.append(aqiInfo).append(": ").
                    append(value).append(". ").append(levelName).append("\n");

            if (value > maxIndex) {
                maxIndex = value;
                targetAqi = indexData;
            }
        }

        out.asString = markerOut.toString();
        out.worstIndex = targetAqi;

        return out;
    }

    public static MapMarker buildSensorMarker(GeoCoordinate coords, String title, String desc) {
        Image sensorImg = new Image();
        try {
            sensorImg.setImageResource(R.drawable.sensor2);
        }
        catch(IOException e) {
            Log.e(Application.TAG, "Cannot load image: " + e);
        }
        MapMarker marker = new MapMarker(coords, sensorImg);

        marker.setTitle(title);
        marker.setDescription(desc);

        return marker;
    }

    public static void updateWeather(Map<String, Object> data, View v) {
        v.setVisibility(RelativeLayout.VISIBLE);

        Map maximumValues = (Map)data.get(WeatherAttributes.MAXIMUM);

        if (maximumValues != null) {
            Double maxTemp = (Double)maximumValues.get(WeatherAttributes.TEMPERATURE);

            if (maxTemp != null) {
                TextView tv = (TextView)v.findViewById(R.id.maxTemperature);
                tv.setText(formatDouble(Math.floor(maxTemp)));
            }

            Double maxH = (Double)maximumValues.get(WeatherAttributes.R_HUMIDITY);

            if (maxH != null) {
                v.findViewById(R.id.forecastedHumidity).setVisibility(RelativeLayout.VISIBLE);
                TextView tv = (TextView)v.findViewById(R.id.maxHumidity);
                tv.setText((long)(maxH * 100) + "%");
            }
        }

        Map minimumValues = (Map)data.get(WeatherAttributes.MINIMUM);

        if (minimumValues != null) {
            Double minTemp = (Double)minimumValues.get(WeatherAttributes.TEMPERATURE);

            if (minTemp != null) {
                TextView tv = (TextView)v.findViewById(R.id.minTemperature);
                tv.setText(formatDouble(Math.floor(minTemp)));
            }

            Double minH = (Double)minimumValues.get(WeatherAttributes.R_HUMIDITY);

            if (minH != null) {
                TextView tv = (TextView)v.findViewById(R.id.minHumidity);
                tv.setText((long)(minH * 100) + "%");
            }
        }

        Double temperature = (Double)data.get(WeatherAttributes.TEMPERATURE);

        if (temperature != null) {
            TextView tv = (TextView)v.findViewById(R.id.currentTemperature);
            tv.setText((long)temperature.doubleValue() + "ºC");
        }

        Double humidity = (Double)data.get(WeatherAttributes.R_HUMIDITY);

        if (humidity != null) {
            TextView tv = (TextView)v.findViewById(R.id.currentHumidity);
            tv.setText((long)(humidity * 100) + "%");
        }

        Double windSpeed = (Double)data.get(WeatherAttributes.WIND_SPEED);

        if (windSpeed != null) {
            TextView tv = (TextView)v.findViewById(R.id.windSpeed);
            tv.setText((long)windSpeed.doubleValue() + "Km/h");
        }

        String windDirection = (String)data.get(WeatherAttributes.WIND_DIRECTION);
        if (windDirection != null) {
            TextView tv = (TextView)v.findViewById(R.id.windDirection);
            tv.setText(windDirection);
        }

        Double pop = (Double)data.get(WeatherAttributes.POP);
        if (pop != null) {
            v.findViewById(R.id.forecastedPrecipitation).setVisibility(RelativeLayout.VISIBLE);
            TextView tv = (TextView)v.findViewById(R.id.pop);
            tv.setText((long)(pop * 100) + "%");
        }

        String weatherType = (String)data.get(WeatherAttributes.WEATHER_TYPE);
        Log.d(Application.TAG, "Weather Type: " + weatherType);
        String icon = WeatherTypes.getIcon(weatherType);
        int id = Application.mainActivity.getResources().getIdentifier(icon, "drawable",
                Application.mainActivity.getPackageName());

        if ( id != 0) {
            ((ImageView)v.findViewById(R.id.forecastedWeatherType)).setImageResource(id);
        }
    }

    public static void updateAirPollution(Map<String, Map> data, LinearLayout parent) {
        parent.removeAllViews();

        // Now all the views are re-created
        for (String pollutant: data.keySet()) {
            LinearLayout container = new LinearLayout(Application.mainActivity);

            container.setOrientation(LinearLayout.HORIZONTAL);
            container.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

            TextView label = new TextView(Application.mainActivity);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0.25f);
            labelParams.setMargins(1, 1, 1, 1);
            label.setLayoutParams(labelParams);

            label.setPadding(1, 1, 1, 1);
            label.setText(pollutant);
            label.setTextColor(Application.mainActivity.getResources().getColor(R.color.blue_text_oasc));
            label.setTypeface(null, Typeface.BOLD);

            container.addView(label);

            TextView value = new TextView(Application.mainActivity);
            LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0.75f);
            valueParams.setMargins(1,1,1,1);
            value.setLayoutParams(valueParams);
            value.setPadding(3, 3, 3, 3);
            value.setGravity(Gravity.CENTER_HORIZONTAL);

            container.addView(value);

            Map indexData = data.get(pollutant);
            value.setText((String) indexData.get("description"));
            value.setTextColor(Application.mainActivity.getResources().getColor(R.color.white));

            value.setBackgroundColor(Color.parseColor(
                    AmbientAreaRenderer.AREA_COLORS.get(indexData.get("name"))));
            value.setTypeface(null, Typeface.BOLD);

            parent.addView(container);
        }
    }

    public static String formatDouble(Double d) {
        double dd = d.doubleValue();

        String out;

        if (dd == (long) dd) {
            out = String.format("%d", (long) dd);
        }
        else {
            out = String.format("%s",dd);
        }

        return out;
    }
}
