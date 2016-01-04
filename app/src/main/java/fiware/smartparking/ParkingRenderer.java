package fiware.smartparking;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPolygon;
import com.here.android.mpa.common.GeoPolyline;
import com.here.android.mpa.common.IconCategory;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapCircle;
import com.here.android.mpa.mapping.MapContainer;
import com.here.android.mpa.mapping.MapLabeledMarker;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapObject;
import com.here.android.mpa.mapping.MapOverlayType;
import com.here.android.mpa.mapping.MapPolygon;
import com.here.android.mpa.mapping.MapPolyline;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;



/**
 * Created by Ulpgc on 04/11/2015.
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
        for (int j = 0; j < parkings.size(); j++) {
            Entity parking = parkings.get(j);
            if (parking.type.equals("ParkingLot")) {
                renderParkingLot(ctx, map, parking);
            } else if (parking.type.equals("StreetParking")) {
                renderStreetParking(ctx, map, parking);
            }
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

            MapMarker mapMarker = new MapMarker(coords, createLabeledIcon(ctx,
                   available, 16, Color.BLACK));
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

        String available =  ent.attributes.get("availableSpotNumber").toString();
        String total = ent.attributes.get("totalSpotNumber").toString();
        MapMarker mapMarker = new MapMarker(coords,
                                            createLabeledIcon(ctx, available , 16, Color.BLACK));
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
        tts.speak("We are close to the destination. Parking mode is on",
                TextToSpeech.QUEUE_ADD, null, "ParkingMode");
    }

    public static void announceParking(TextToSpeech tts, String name) {
        tts.speak("Heading to parking: " + name, TextToSpeech.QUEUE_ADD, null, "ParkingFound");
    }

    private static Image createLabeledIcon(Context ctx, String text1, float textSize, int textColor) {
        try {
            Bitmap iconBitmap = BitmapFactory.decodeResource(ctx.getResources(), R.mipmap.parking);
            Paint paint = createPaint(ctx, textSize, textColor);
            float baseline = -paint.ascent();
            int textWidth = (int) (paint.measureText(text1) + 0.5f);
            int textHeight = (int) (baseline + paint.descent() + 0.5f);

            int width = Math.max(iconBitmap.getWidth(), textWidth);
            int height = iconBitmap.getHeight() + textHeight;
            Bitmap resBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas resCanvas = new Canvas(resBitmap);
            resCanvas.drawBitmap(iconBitmap, calculateLeft(width, iconBitmap.getWidth()), 0, null);
            resCanvas.drawText(text1, calculateLeft(width, textWidth),
                    baseline + iconBitmap.getHeight(), paint);

            Image resImage = new Image();
            resImage.setBitmap(resBitmap);
            return resImage;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Paint createPaint(Context ctx, float textSize,int textColor){
        Paint paint = new Paint();
        paint.setTextSize(dipToPixels(ctx, textSize));
        paint.setColor(textColor);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        return paint;
    }

    private static float dipToPixels(Context ctx, float dip){
        final float scale = ctx.getResources().getDisplayMetrics().density;
        return (dip * scale);
    }

    private static int calculateLeft (int globalWidth, int elementWidth){
        return (globalWidth - elementWidth)/2;
    }
}
