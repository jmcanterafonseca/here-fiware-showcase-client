package fiware.smartparking;

import android.os.AsyncTask;
import android.util.Log;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPolygon;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 *   Retrieves data from the city by calling FIWARE-HERE Adaptor
 *
 *
 */
public class CityDataRetriever extends AsyncTask<CityDataRequest, Integer, List<Entity>> {
    private CityDataListener listener;

    private static String SERVICE_URL = "http://130.206.83.68:7007/v2/entities";

    protected List<Entity> doInBackground(CityDataRequest... request) {
        String urlString = createRequestURL(request[0]);

        StringBuffer output = new StringBuffer("");
        List<Entity> out = new ArrayList<Entity>();

        /*
        Entity ent = new Entity();

        ent.id = "12345";
        ent.type = "EnvironmentEvent";
        ent.attributes = new HashMap<String, Object>();
        ent.location = new double[] { 41.1500167847, -8.60708522797 };
        ent.attributes.put("temperature", new Double(22.5));

        out.add(ent); */

        try{
            URL url = new URL(urlString);

            Log.d(Application.TAG, "URL: " + urlString);

            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestProperty("User-Agent", "FIWARE-HERE-Navigator");
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.connect();

            InputStream inputStream = connection.getInputStream();

            BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = rd.readLine()) != null) {
                output.append(line);
            }

            Log.d(Application.TAG, "Response: " + output.toString());
            JSONArray array = new JSONArray(output.toString());

            for(int j = 0; j < array.length(); j++) {
                Entity ent = new Entity();
                JSONObject obj = array.getJSONObject(j);

                ent.id = obj.getString("id");
                ent.type = obj.getString("type");
                ent.attributes = new HashMap<String, Object>();

                JSONObject location;
                try {
                    location = obj.getJSONObject("centroid");
                }
                catch(JSONException jse) {
                    location = obj.getJSONObject("location");
                }

                String locationValue = location.getString("value");
                String[] coordinates = locationValue.split(",");

                ent.location = new double[]{ Double.parseDouble(coordinates[0]),
                                                            Double.parseDouble(coordinates[1]) };

                fillAttributes(obj, ent.type, ent.attributes);
                out.add(ent);
            }
        } catch (Exception e) {
            Log.e(Application.TAG, "While obtaining data: " + e.toString());
        }

        return out;
    }

    private void fillAmbientObserved(JSONObject obj, String type, Map<String, Object> attrs) throws Exception {
        getDoubleJSONAttr("temperature", obj, "temperature", attrs);
        getDoubleJSONAttr("humidity", obj, "humidity", attrs);
        getDoubleJSONAttr("noiseLevel", obj, "noiseLevel", attrs);
        try {
            JSONObject pollutants = obj.getJSONObject("pollutants");
            for(String pollutant : Application.POLLUTANTS) {
                try {
                    double value =
                            pollutants.getJSONObject(pollutant).getDouble("concentration");
                    attrs.put(pollutant, value);
                }
                catch(JSONException jsoe) { }
            }
        }
        catch(JSONException jsoe) {

        }
    }

    private void fillParking(JSONObject obj, String type,
                             Map<String, Object> attrs) throws Exception {
        getIntegerJSONAttr("availableSpotNumber", obj, "availableSpotNumber", attrs);
        getIntegerJSONAttr("totalSpotNumber", obj, "totalSpotNumber", attrs);
        getIntegerJSONAttr("capacity", obj, "totalSpotNumber", attrs);
        getIntegerJSONAttr("parking_disposition", obj, "parkingDisposition", attrs);
        getStringJSONAttr("name", obj, "name", attrs);
        getStringJSONAttr("description", obj, "description", attrs);

        if(type.equals("StreetParking")) {
            boolean isArray = true;
            JSONArray polygons = null;

            List<GeoPolygon> location = new ArrayList<GeoPolygon>();
            try {
                polygons = obj.getJSONArray("location").getJSONArray(0);
            }
            catch(JSONException jsoe) {
                isArray = false;
            }
            if(isArray == true) {
                int total = polygons.length();
                for (int j = 0; j < total; j++) {
                    JSONArray polygon = polygons.getJSONArray(j);
                    List<GeoCoordinate> geoPolygon = new ArrayList<GeoCoordinate>();
                    for (int x = 0; x < polygon.length(); x++) {
                        float lat = Float.parseFloat(polygon.getJSONArray(x).getString(0));
                        float lon = Float.parseFloat(polygon.getJSONArray(x).getString(1));
                        geoPolygon.add(new GeoCoordinate(lat, lon));
                    }
                    location.add(new GeoPolygon(geoPolygon));
                }
            }
            else {
                location.add(getPolygon(obj.getString("location")));
            }
            attrs.put("polygon", location);
        }
    }

    private void fillAttributes(JSONObject obj, String type,
                                Map<String, Object> attrs) throws Exception {
        if(type.equals("TrafficEvent")) {

        }
        else if(type.equals("AmbientObserved")) {
            fillAmbientObserved(obj,type,attrs);
        }
        else if(type.equals("ParkingLot") || type.equals("StreetParking")) {
           fillParking(obj, type, attrs);
        }
        else if(type.equals("CityEvent")) {

        }
        else if(type.equals("AmbientArea")) {
            fillAmbientArea(obj,type,attrs);
        }
    }

    private void fillAmbientArea(JSONObject obj, String type,
                                 Map<String, Object> attrs) throws Exception {

        attrs.put("polygon", getPolygon(obj.getString("location")));
    }

    private GeoPolygon getPolygon(String coords) {
        String[] polygonCoords = coords.split(",");
        List<GeoCoordinate> geoPolygon = new ArrayList<GeoCoordinate>();
        for(int j = 0; j < polygonCoords.length; j+=2) {
            float lat = Float.parseFloat(polygonCoords[j]);
            float lon = Float.parseFloat(polygonCoords[j + 1]);
            geoPolygon.add(new GeoCoordinate(lat, lon));
        }

        return new GeoPolygon(geoPolygon);
    }

    private String getTypes(List<String> types) {
        StringBuffer out = new StringBuffer();

        for(int j = 0; j < types.size(); j++) {
            out.append(types.get(j));
            if(j + 1 < types.size()) {
                out.append(",");
            }
        }

        return out.toString();
    }

    private String createRequestURL(CityDataRequest req) {
        String geometry = req.geometry;
        if(geometry == null) {
            geometry = "Circle";
        }

        String radiusStr = "";
        if(req.radius != -1) {
            radiusStr = "&radius=" + req.radius;
        }

        String coords = "";
        if(req.coordinates != null) {
            coords = req.coordinates[0] + "," + req.coordinates[1];
        }
        else if (req.polygon != null) {
            for (int j = 0; j < req.polygon.getNumberOfPoints(); j++) {
                GeoCoordinate point = req.polygon.getPoint(j);
                coords += point.getLatitude() + "," + point.getLongitude();
                coords += ",";
            }
            coords = coords.substring(0,coords.length() - 1);
        }

        return SERVICE_URL + "?" + "coords=" + coords
                + radiusStr + "&type=" + getTypes(req.types) + "&geometry=" + geometry;
    }


    private void getDoubleJSONAttr(String attr, JSONObject obj, String mappedAttr,
                                     Map<String, Object> attrs) {
        Double out = null;
        try {
            out = obj.getDouble(attr);
            attrs.put(mappedAttr, out);
        }
        catch(JSONException e) { }
    }

    private void getIntegerJSONAttr(String attr, JSONObject obj, String mappedAttr,
                                    Map<String, Object> attrs) {
        Integer out = null;
        try {
            out = obj.getInt(attr);
            attrs.put(mappedAttr, out);
        }
        catch(JSONException e) { }
    }

    private void getStringJSONAttr(String attr, JSONObject obj, String mappedAttr,
                                    Map<String, Object> attrs) {
        String out = null;
        try {
            out = obj.getString(attr);
            attrs.put(mappedAttr, out);
        }
        catch(JSONException e) { }
    }


    public void setListener(CityDataListener listener) {
        this.listener = listener;
    }

    protected void onPostExecute(List<Entity> data) {
        listener.onCityDataReady(data);
    }
}
