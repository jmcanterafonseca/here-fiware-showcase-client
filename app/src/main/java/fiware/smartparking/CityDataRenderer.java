package fiware.smartparking;

import android.graphics.PointF;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapMarker;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 *
 *  Helper class for rendering city data
 *
 */
public class CityDataRenderer {
    private StringBuffer str;
    private List<SpeechMessage> msgs = new ArrayList<>();

    public String renderData(final Map hereMap, final TextToSpeech tts, final Entity ent) {
        String created = (String)ent.attributes.get("created");
        String sensorType = (String)ent.attributes.get("sensorType");

        if (sensorType != null && sensorType.equals("mobile")) {
            if (created != null) {
                // Filter out any kind of measurement which is old
                DateTimeFormatter parser    = ISODateTimeFormat.dateTimeParser();
                DateTime dateCreated = parser.parseDateTime(created);
                DateTime now = DateTime.now();
                if (now.getMillis() - dateCreated.getMillis() > (2 * 60 *  60 * 1000)) {
                    Log.d(Application.TAG, ent.id +
                            ": Not rendered as it is a very old measurement");
                    return "";
                }
            }
        }

        str = new StringBuffer();
        final GeoCoordinate coords = new GeoCoordinate(ent.location[0], ent.location[1]);

        Double temperature = (Double)ent.attributes.get(WeatherAttributes.TEMPERATURE);

        if(temperature != null) {
            msgs.add(new SpeechMessage("Temperature: " + temperature,
                    500, ent.id + "_" + "Temperature"));
            str.append("Temperature: " + temperature);
        }

        Double humidity = (Double)ent.attributes.get(WeatherAttributes.R_HUMIDITY);
        if(humidity != null) {
            msgs.add(new SpeechMessage("Humidity: " + humidity + "%",
                    500, ent.id + "_" + "Humidity"));
            if(str.length() > 0) {
                str.append("\n");
            }
            str.append("Humidity: " + humidity + "%");
        }

        if(ent.attributes.get("noiseLevel") != null) {
            double noiseLevel = ((Double)ent.attributes.get("noiseLevel")).doubleValue();
            if(str.length() > 0) {
                str.append("\n");
            }
            String text = null;

            if(noiseLevel > 65) {
                text = "Noise level is high";
            }
            else {
                text = "Noise level is normal";
            }

            str.append(text);
            msgs.add(new SpeechMessage(text, -1,  ent.id + "_" + "NoiseLevel"));
        }

        AirQualityCalculator calculator = new AirQualityCalculator();
        // Filter out only pollutants
        java.util.Map<String, Double> pollutantData = new HashMap<>();
        for (String pollutant: Application.POLLUTANTS) {
            Double value = (Double)ent.attributes.get(pollutant);
            if (value != null) {
                pollutantData.put(pollutant, value);
            }
        }

        calculator.execute(pollutantData);
        calculator.setListener(new ResultListener<java.util.Map<String, java.util.Map>>() {
            @Override
            public void onResultReady(java.util.Map<String, java.util.Map> result) {
                Utilities.AirQualityData data = null;
                if (result != null) {
                    data = Utilities.getAirQualityData(result);

                    if (data.worstIndex != null) {
                        msgs.add(new SpeechMessage("Pollution level is " +
                                data.worstIndex.get("description"),
                                500, ent.id + "_" + "AirQuality"));

                    } else {
                        Log.d(Application.TAG, "Air quality index not found: " + ent.id);
                    }
                }

                Utilities.speak(tts, msgs);

                if(data.asString != null) {
                    if (str.length() > 0) {
                        str.append("\n");
                    }
                    str.append(data.asString);

                    // We need to update the air quality data levels
                }

                MapMarker marker = Utilities.buildSensorMarker(coords, "Ambient Data",
                        str.toString());

                if (str.length() > 0) {
                    hereMap.addMapObject(marker);
                    marker.showInfoBubble();
                    Application.mapObjects.add(marker);
                }
            }
        });

        GeoBoundingBox bb = hereMap.getBoundingBox();
        if (false && !bb.contains(coords)) {
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
