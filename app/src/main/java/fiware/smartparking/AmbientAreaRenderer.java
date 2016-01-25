package fiware.smartparking;

import android.graphics.Color;
import android.graphics.PointF;
import android.speech.tts.TextToSpeech;

import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoPolygon;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapOverlayType;
import com.here.android.mpa.mapping.MapPolygon;

import java.util.Arrays;
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
    private AmbientRenderListener listener;

    public AmbientAreaRenderer(Map hereMap, TextToSpeech tts, Entity ent) {
        this.hereMap = hereMap;
        this.ambientArea = ent;
        this.tts = tts;
        this.polygon = (GeoPolygon)ent.attributes.get("polygon");
    }

    @Override
    public void onCityDataReady(List<Entity> data) {
        MapPolygon polygon = doRender(null);

        listener.onRendered(null,polygon);
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

    public void render(AmbientRenderListener listener) {
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