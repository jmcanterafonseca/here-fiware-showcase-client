package fiware.smartparking;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapCircle;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapOverlayType;

import java.util.List;

/**
 *
 * Renders extra objects such as parking restrictions, gas stations or garages
 *
 *
 */
public class ExtraObjRenderer {
    public static void render(Context ctx, Map map, java.util.Map<String, List<Entity>> entities) {
        List<Entity> gasStations = entities.get(Application.GAS_STATION_TYPE);

        if (gasStations != null) {
            for (Entity gasStation : gasStations) {
                if(Application.renderedEntities.get(gasStation.id) != null) {
                    continue;
                }

                renderGasStation(ctx, map, gasStation);
            }
        }
    }

    public static void renderGasStation(Context ctx, Map map, Entity ent) {
        GeoCoordinate coords = new GeoCoordinate(ent.location[0], ent.location[1]);

        MapMarker mapMarker = new MapMarker(coords,
                RenderUtilities.createLabeledIcon(ctx,
                        (String) ent.attributes.get("name"), 16, Color.BLACK, R.drawable.gas_station));

        mapMarker.setOverlayType(MapOverlayType.FOREGROUND_OVERLAY);
        map.addMapObject(mapMarker);

        //Creating a default circle with 10 meters radius
        MapCircle circle = new MapCircle(10, coords);
        circle.setLineColor(Color.parseColor("#FF0000FF")); //(Color.GREEN);
        circle.setFillColor(Color.parseColor("#770000FF"));
        map.addMapObject(circle);
    }
}
