package fiware.smartparking;

import android.graphics.PointF;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapMarker;


/**
 *
 *  Helper class for rendering city data
 *
 */
public class CityDataRenderer {
    private StringBuffer str;

    public String renderData(final Map hereMap, final TextToSpeech tts, final Entity ent) {
        str = new StringBuffer();
        GeoCoordinate coords = new GeoCoordinate(ent.location[0], ent.location[1]);

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

        AirQualityCalculator calculator = new AirQualityCalculator();
        calculator.execute(ent.attributes);
        calculator.setListener(new ResultListener<java.util.Map<String, java.util.Map>>() {
            @Override
            public void onResultReady(java.util.Map<String, java.util.Map> result) {
                Utilities.AirQualityData data = null;
                if (result != null) {
                    data = Utilities.getAirQualityData(result);

                    if (data.worstIndex != null) {
                        tts.speak("Pollution level is " + data.worstIndex.get("description"),
                                TextToSpeech.QUEUE_ADD, null, ent.id + "_" + "AirQuality");
                        tts.playSilentUtterance(500, TextToSpeech.QUEUE_ADD, ent.id + "_"
                                + "AirQuality_");
                    } else {
                        Log.d(Application.TAG, "Air quality index not found: " + ent.id);
                    }
                }

                tts.playSilentUtterance(100, TextToSpeech.QUEUE_ADD, "Entity_End");

                if(data.asString != null) {
                    if (str.length() > 0) {
                        str.append("\n");
                    }
                    str.append(data.asString);
                }

                GeoCoordinate coords = new GeoCoordinate(ent.location[0], ent.location[1]);
                MapMarker marker = Utilities.buildSensorMarker(coords, "Smart City",
                        str.toString());

                hereMap.addMapObject(marker);
                marker.showInfoBubble();
                Application.mapObjects.add(marker);
            }
        });

        GeoBoundingBox bb = hereMap.getBoundingBox();
        if(!bb.contains(coords)) {
            Map.PixelResult pr = hereMap.projectToPixel(coords);
            PointF point = pr.getResult();
            final double currentZoom = hereMap.getZoomLevel();
            hereMap.setZoomLevel(hereMap.getZoomLevel() - 2,
                    hereMap.projectToPixel(hereMap.getCenter()).getResult(),
                                                    Map.Animation.LINEAR);
        }

        return str.toString();
    }
}
