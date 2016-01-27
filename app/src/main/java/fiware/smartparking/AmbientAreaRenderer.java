package fiware.smartparking;

import android.graphics.Color;
import android.graphics.PointF;
import android.speech.tts.TextToSpeech;

import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoPolygon;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapOverlayType;
import com.here.android.mpa.mapping.MapPolygon;

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
    public void onCityDataReady(List<Entity> data) {
        // When city data is ready, obtain data from all the sensors
        // and then pass the ball to the AirQualityCalculator
        java.util.Map<String,List<Double>> pollutants = new HashMap<>();

        for (Entity ent: data) {
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
        calculator.setListener(new ResultListener<java.util.Map>() {
            @Override
            public void onResultReady(java.util.Map result) {
                if (result != null) {
                    // Lets see what is the greatest AQI and then paint accordingly
                    for (java.util.Map aqiInfo : result.keySet()) {
                        
                    }
                    MapPolygon polygon = doRender(null);
                    listener.onRendered(null, polygon);
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
        ambientAreaPolygon.setLineColor(Color.parseColor("#8080CBC4"));
        ambientAreaPolygon.setFillColor(Color.parseColor("#80B2DFDB"));

        ambientAreaPolygon.setOverlayType(MapOverlayType.BACKGROUND_OVERLAY);

        hereMap.addMapObject(ambientAreaPolygon);

        tts.speak("You have entered an area with good air quality",
                TextToSpeech.QUEUE_ADD, null, "AmbientArea" + '_' + ambientArea.id);

        GeoBoundingBox bb = hereMap.getBoundingBox();
        GeoBoundingBox box = polygon.getBoundingBox();
        if(!bb.contains(box)) {
            Map.PixelResult pr = hereMap.projectToPixel(box.getCenter());
            PointF point = pr.getResult();
            hereMap.setZoomLevel(hereMap.getZoomLevel() - 3,  point, Map.Animation.LINEAR);
        }

        tts.playSilentUtterance(100, TextToSpeech.QUEUE_ADD, "Entity_End");

        return ambientAreaPolygon;
    }
}