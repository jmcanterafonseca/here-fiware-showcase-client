package fiware.smartparking;

import android.util.Log;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.mapping.MapMarker;

import java.io.IOException;

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
}
