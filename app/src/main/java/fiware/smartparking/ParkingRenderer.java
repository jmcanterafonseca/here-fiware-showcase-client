package fiware.smartparking;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.speech.tts.TextToSpeech;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPolygon;

import com.here.android.mpa.common.Image;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapCircle;

import com.here.android.mpa.mapping.MapMarker;

import com.here.android.mpa.mapping.MapOverlayType;
import com.here.android.mpa.mapping.MapPolygon;

import java.util.List;


/**
 *
 * Renders parking entities
 *
 */
public class ParkingRenderer {

    private static Image loadParkingIcon() {
        Image parkingIcon = new Image();

        try {
            parkingIcon.setImageResource(R.mipmap.parking);
        } catch (Exception e) {
            parkingIcon = null;
        }

        return parkingIcon;
    }

    private static Image parkingIcon;

    static {
        parkingIcon = loadParkingIcon();
    }

    public static void render(Context ctx, Map map, List<Entity> parkings) {
        for (Entity parking: parkings) {
            if(Application.renderedEntities.get(parking.id) != null) {
                continue;
            }

            if (Application.PARKING_LOT_TYPE.equals(parking.type) ||
                    Application.PARKING_LOT_ZONE_TYPE.equals(parking.type)) {
                renderParkingLot(ctx, map, parking);
            } else if (parking.type.equals(Application.STREET_PARKING_TYPE)) {
                renderStreetParking(ctx, map, parking);
            }

            Application.renderedEntities.put(parking.id, parking.id);
        }
    }

    private static void renderStreetParking(Context ctx, Map map, Entity ent) {
        GeoCoordinate coords = new GeoCoordinate(ent.location[0], ent.location[1]);

        String available =  ent.attributes.get("availableSpotNumber").toString();

        if(available.equals("0")) {
            return;
        }

        String total = ent.attributes.get("totalSpotNumber").toString();

        List<GeoPolygon> polygons = (List<GeoPolygon>)ent.attributes.get("polygon");
        for(int j = 0; j < polygons.size(); j++) {
            GeoPolygon polygon = polygons.get(j);
            MapPolygon streetPolygon = new MapPolygon(polygon);
            streetPolygon.setLineColor(Color.parseColor("#FF0000FF"));
            streetPolygon.setFillColor(Color.parseColor("#770000FF"));


            RenderStyle style = new RenderStyle();

            MapMarker mapMarker = new MapMarker(coords, RenderUtilities.createLabeledIcon(ctx,
                    available, style, R.mipmap.parking));
            mapMarker.setOverlayType(MapOverlayType.FOREGROUND_OVERLAY);

            map.addMapObject(streetPolygon);
            map.addMapObject(mapMarker);

            Application.mapObjects.add(streetPolygon);
            Application.mapObjects.add(mapMarker);
        }
    }

    public static void render(Map map, Entity ent) {

    }

    private static void renderParkingLot(Context ctx, Map map, Entity ent) {
        GeoCoordinate coords = new GeoCoordinate(ent.location[0], ent.location[1]);

        Integer nAvailable = (Integer)ent.attributes.get("availableSpotNumber");

        String available = "?";
        if (nAvailable != null) {
            available = nAvailable.toString();
        }

        String total = "?";
                Integer nTotal = (Integer)ent.attributes.get("totalSpotNumber");
        if(nTotal != null) {
            total = nTotal.toString();
        }

        String label = available + "/" + total;

        RenderStyle style = new RenderStyle();

        MapMarker mapMarker = new MapMarker(coords,
                                            RenderUtilities.createLabeledIcon(ctx,
                                                    label, style, R.mipmap.parking));
        mapMarker.setOverlayType(MapOverlayType.FOREGROUND_OVERLAY);
        map.addMapObject(mapMarker);

        //Creating a default circle with 10 meters radius
        MapCircle circle = new MapCircle(10, coords);
        circle.setLineColor(Color.parseColor("#FF0000FF")); //(Color.GREEN);
        circle.setFillColor(Color.parseColor("#770000FF"));
        map.addMapObject(circle);

        Application.mapObjects.add(circle);
        Application.mapObjects.add(mapMarker);
    }

    public static void announceParkingMode(TextToSpeech tts) {
        tts.playEarcon("parking_mode",TextToSpeech.QUEUE_ADD, null, "ParkingMode");
        tts.speak("We are close to the destination. Parking mode is on",
                TextToSpeech.QUEUE_ADD, null, "ParkingMode");
    }

    public static void announceParking(TextToSpeech tts, String name) {
        tts.speak("Heading to parking: " + name, TextToSpeech.QUEUE_ADD, null, "ParkingFound");
    }
}
