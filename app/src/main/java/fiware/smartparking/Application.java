package fiware.smartparking;

import com.here.android.mpa.mapping.MapObject;

import java.util.ArrayList;
import java.util.List;

/**
 *   Global Application object for common context and constants
 *
 *
 */
public class Application {
    public static MainActivity mainActivity = null;

    public static int THRESHOLD_DISTANCE = 4000;
    public static int DEFAULT_RADIUS = 400;

    // Parking mode is on from 700 ms far away from destination
    public static int PARKING_DISTANCE = 700;

    public static List<MapObject> mapObjects = new ArrayList<MapObject>();

    public static String TAG = "FIWARE-HERE";
    public static String TRANSFER_RESULT = "Transfer-Result";

    // We will look for parkings not further than 3km
    public static int MAX_PARKING_DISTANCE = 3000;

    // Average radius for Ambient areas
    public static int AMBIENT_AREA_RADIUS = 1100;

    /* Every 250 meters we ask about changes on ambient area */
    public static int DISTANCE_FREQ_AMBIENT_AREA = 250;

    /* NGSI types used for querying data */
    public static String AMBIENT_OBSERVED_TYPE = "AmbientObserved";
    public static String AMBIENT_AREA_TYPE     = "AmbientArea";
    public static String PARKING_TYPE          = "Parking";
    public static String STREET_PARKING_TYPE   = "StreetParking";
    public static String PARKING_LOT_TYPE      = "ParkingLot";
    public static String WEATHER_FORECAST_TYPE = "WeatherForecast";

    /* 14 m/s == 50 kms/h */
    public static int DEFAULT_SPEED = 14;
}
