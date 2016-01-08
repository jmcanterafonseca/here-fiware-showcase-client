package fiware.smartparking;

import android.app.Activity;

import com.here.android.mpa.mapping.MapObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jmcf on 4/11/15.
 */
public class Application {
    public static MainActivity mainActivity = null;

    public static int THRESHOLD_DISTANCE = 4000;
    public static int DEFAULT_RADIUS = 400;

    public static int PARKING_DISTANCE = 700;

    public static List<MapObject> mapObjects = new ArrayList<MapObject>();

    public static String TAG = "FIWARE-HERE";
    public static String TRANSFER_RESULT = "Transfer-Result";
}
