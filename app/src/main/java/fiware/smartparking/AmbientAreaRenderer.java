package fiware.smartparking;

import android.graphics.Color;
import android.graphics.PointF;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPolygon;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapOverlayType;
import com.here.android.mpa.mapping.MapPolygon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 *   Renders an AmbientArea by creating a polygon and querying for sensor data on that area
 *   and knowing about overall air quality there
 *
 */
public class AmbientAreaRenderer implements CityDataListener {

    private Map hereMap;
    private TextToSpeech tts;
    private Entity ambientArea;
    private GeoPolygon polygon;
    private AmbientAreaRenderListener listener;

    private static java.util.Map<String,String> AREA_COLORS= new HashMap<>();

    static {
        int index = 0;
        for(String pollutionLevel : Application.POLLUTION_LEVELS) {
            AREA_COLORS.put(pollutionLevel,Application.POLLUTION_COLORS[index++]);
        }

    }

    public AmbientAreaRenderer(Map hereMap, TextToSpeech tts, Entity ent) {
        this.hereMap = hereMap;
        this.ambientArea = ent;
        this.tts = tts;
        this.polygon = (GeoPolygon)ent.attributes.get("polygon");
    }

    @Override
    public void onCityDataReady(java.util.Map<String, List<Entity>> data) {
        // When city data is ready, obtain data from all the sensors
        // and then pass the ball to the AirQualityCalculator
        java.util.Map<String,List<Double>> pollutants = new HashMap<>();

        for (Entity ent: data.get(Application.RESULT_SET_KEY)) {
            if(!ent.type.equals(Application.AMBIENT_OBSERVED_TYPE)) {
                continue;
            }

            java.util.Map<String,Object> attributes = ent.attributes;

            for (String pollutant : Application.POLLUTANTS) {
                if (attributes.get(pollutant) != null) {
                    List<Double> accumulated = pollutants.get(pollutant);
                    if(accumulated == null) {
                        accumulated = new ArrayList<>();
                        pollutants.put(pollutant, accumulated);
                    }
                    accumulated.add((Double)attributes.get(pollutant));
                }
            }
        }

        java.util.Map<String, Double> finalValues = new HashMap<>();

        for(String pollutant : pollutants.keySet()) {
            List<Double> values = pollutants.get(pollutant);
            double average = 0;
            for(double value : values) {
                average += value;
            }
            average /= values.size();

            finalValues.put(pollutant, average);
        }

        AirQualityCalculator calculator = new AirQualityCalculator();
        calculator.setListener(new ResultListener<java.util.Map<String,java.util.Map>>() {
            @Override
            public void onResultReady(java.util.Map<String,java.util.Map> result) {
                if (result != null && result.size() > 0) {
                    Utilities.AirQualityData data = Utilities.getAirQualityData(result);

                    String aqiLevelName = (String)data.worstIndex.get("name");

                    MapPolygon polygon = doRender(AREA_COLORS.get(aqiLevelName));
                    tts.speak("You have entered an area with "
                                    + data.worstIndex.get("description") + " pollution",
                            TextToSpeech.QUEUE_ADD, null, "AmbientArea" + '_' + ambientArea.id);

                    tts.playSilentUtterance(100, TextToSpeech.QUEUE_ADD, "Entity_End");

                    GeoCoordinate coords = new GeoCoordinate(ambientArea.location[0],
                            ambientArea.location[1]);

                    MapMarker marker = Utilities.buildSensorMarker(coords, "Air Quality",
                            data.asString);

                    hereMap.addMapObject(marker);
                    marker.showInfoBubble();

                    Application.mapObjects.add(marker);

                    listener.onRendered(aqiLevelName, polygon);
                }
                else {
                    Log.w(Application.TAG, "Air quality calculator returned empty object");
                }
            }
        });

        calculator.execute(finalValues);
    }

    private void getDataFromSensors() {
        CityDataRequest req = new CityDataRequest();
        req.geometry = "Polygon";
        req.polygon = polygon;
        req.types = Arrays.asList(Application.AMBIENT_OBSERVED_TYPE);

        CityDataRetriever retriever = new CityDataRetriever();
        retriever.setListener(this);

        retriever.execute(req);
    }

    public void render(AmbientAreaRenderListener listener) {
        this.listener = listener;
        polygon = (GeoPolygon)ambientArea.attributes.get("polygon");
        getDataFromSensors();
    }


    private MapPolygon doRender(String targetColor) {
        MapPolygon ambientAreaPolygon = new MapPolygon(polygon);
        ambientAreaPolygon.setLineColor(Color.parseColor(targetColor));
        ambientAreaPolygon.setFillColor(Color.parseColor(targetColor));

        ambientAreaPolygon.setOverlayType(MapOverlayType.BACKGROUND_OVERLAY);

        hereMap.addMapObject(ambientAreaPolygon);

        GeoBoundingBox bb = hereMap.getBoundingBox();
        GeoBoundingBox box = polygon.getBoundingBox();
        if(!bb.contains(box)) {
            Map.PixelResult pr = hereMap.projectToPixel(box.getCenter());
            PointF point = pr.getResult();
            hereMap.setZoomLevel(hereMap.getZoomLevel() - 3,  point, Map.Animation.LINEAR);
        }

        return ambientAreaPolygon;
    }
}