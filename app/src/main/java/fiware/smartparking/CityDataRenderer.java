package fiware.smartparking;

import android.graphics.PointF;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapMarker;

import java.io.IOException;
import java.util.List;

/**
 * Created by jmcf on 10/11/15.
 */
public class CityDataRenderer {
    public static String renderData(final Map map, TextToSpeech tts, Entity ent) {
        GeoCoordinate coords = new GeoCoordinate(ent.location[0], ent.location[1]);

        Image sensorImg = new Image();
        try {
            sensorImg.setImageResource(R.drawable.sensor2);
        }
        catch(IOException e) {
            Log.e("FIWARE-HERE", "Cannot load image: " + e);
        }
        MapMarker marker = new MapMarker(coords, sensorImg);
        marker.setTitle("Smart City");
        map.addMapObject(marker);

        StringBuffer str = new StringBuffer();

        Double temperature = (Double)ent.attributes.get("temperature");
        if(temperature != null) {
            tts.speak("Temperature: " + temperature, TextToSpeech.QUEUE_ADD, null,
                    ent.id + "_" + "Temperature");
            tts.playSilentUtterance(500, TextToSpeech.QUEUE_ADD, ent.id + "_" + "Temperature_");
            str.append("Temperature: " + temperature);
        }

        Double humidity = (Double)ent.attributes.get("humidity");
        if(humidity != null) {
            tts.speak("Humidity: " + humidity + "%", TextToSpeech.QUEUE_ADD, null,
                    ent.id + "_" + "Humidity");
            if(str.length() > 0) {
                str.append("\n");
            }
            str.append("Humidity: " + humidity + "%");
            tts.playSilentUtterance(500, TextToSpeech.QUEUE_ADD, ent.id + "_" + "Humidity_");
        }

        if(ent.attributes.get("processedOzone") != null) {
            String qualityId = AirQualityCalculator.getAirQualityIndex(ent.attributes);
            String quality = AirQualityCalculator.mapDescriptions.get(qualityId);
            tts.speak("Air Quality is " + quality,
                    TextToSpeech.QUEUE_ADD, null, ent.id + "_" + "AirQuality");
            tts.playSilentUtterance(500, TextToSpeech.QUEUE_ADD, ent.id + "_" + "AirQuality_");
            if(str.length() > 0) {
                str.append("\n");
            }
            str.append("Air Quality is " + quality);
        }

        if(ent.attributes.get("noiseLevel") != null) {
            double noiseLevel = ((Double)ent.attributes.get("noiseLevel")).doubleValue();
            if(str.length() > 0) {
                str.append("\n");
            }
            if(noiseLevel > 65) {
                tts.speak("Noise level is high",
                        TextToSpeech.QUEUE_ADD, null, ent.id + "_" + "NoiseLevel");
                str.append("Noise level is high");
            }
            else {
                tts.speak("Noise level is normal",
                        TextToSpeech.QUEUE_ADD, null, ent.id + "_" + "NoiseLevel");
                str.append("Noise level is normal");
            }
        }

        tts.playSilentUtterance(100, TextToSpeech.QUEUE_ADD, "Entity_End");

        marker.setDescription(str.toString());
        marker.showInfoBubble();

        Application.mapObjects.add(marker);

        GeoBoundingBox bb = map.getBoundingBox();
        if(!bb.contains(coords)) {
            Map.PixelResult pr = map.projectToPixel(coords);
            PointF point = pr.getResult();
            final double currentZoom = map.getZoomLevel();
            map.setZoomLevel(map.getZoomLevel() - 2, map.projectToPixel(map.getCenter()).getResult(),
                                                    Map.Animation.LINEAR);
        }

        return str.toString();
    }
}
