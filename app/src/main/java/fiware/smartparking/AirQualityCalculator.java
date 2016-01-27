package fiware.smartparking;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 *   Calculates air quality according to the concentration values
 *   measured by sensors
 *
 */
public class AirQualityCalculator extends AsyncTask<Map, Integer, Map> {
    private static String THRESHOLDS_URL = "http://130.206.83.68:1026/v2/entities";

    // Cache to hold the data corresponding to thresholds
    private Map<String,List> thresholdData = new HashMap<>();
    private Map<String,Map>  indexClasses =  new HashMap<>();

    private boolean dataLoaded = false;

    ResultListener<Map> listener;

    protected Map doInBackground(Map... request) {
        Map params = request[0];

        Map out = null;

        try {
            out = getAirQualityIndexes(params);
        }
        catch(Exception e) {
            Log.e(Application.TAG, "Error while calculating air quality index: " + e);
        }

        return out;
    }

    public void setListener(ResultListener<Map> listener) {
        this.listener = listener;
    }

    protected void onPostExecute(Map data) {
        listener.onResultReady(data);
    }

    private JSONArray getNgsiv2Data(String type) throws Exception {
        String urlString = THRESHOLDS_URL + "?" + "type" + "=" + type;

        URL url = new URL(urlString);

        Log.d(Application.TAG, "URL: " + urlString);

        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestProperty("User-Agent", "FIWARE-HERE-Navigator");
        connection.setRequestMethod("GET");
        connection.setDoInput(true);
        connection.connect();

        InputStream inputStream = connection.getInputStream();

        StringBuffer output = new StringBuffer();
        BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = rd.readLine()) != null) {
            output.append(line);
        }

        Log.d(Application.TAG, "Response: " + output.toString());
        JSONArray array = new JSONArray(output.toString());

        return array;
    }

    public AirQualityCalculator() {

    }

    private String getValue(JSONObject obj, String property) throws Exception {
        return obj.getJSONObject(property).getString("value");
    }

    private synchronized void loadData() throws Exception {
        if(!dataLoaded) {
            doLoadData();
        }
        dataLoaded = true;
    }

    private void doLoadData() throws Exception {
        JSONArray data = getNgsiv2Data("AirQualityIndexClass");

        for (int j = 0; j < data.length(); j++) {
            JSONObject obj = data.getJSONObject(j);
            String className = getValue(obj, "name");
            Map<String, Object> classData = new HashMap<>();
            classData.put("description", getValue(obj, "description"));
            classData.put("startValue", new Integer(getValue(obj, "startValue")));
            classData.put("endValue", new Integer(getValue(obj, "endValue")));

            indexClasses.put(className, classData);
        }

        data = getNgsiv2Data("AirQualityThreshold");

        for (int j = 0; j < data.length(); j++) {
            JSONObject obj = data.getJSONObject(j);
            String pollutant = getValue(obj, "pollutant");

            List<Map> pollutantData = thresholdData.get(pollutant);
            if(pollutantData == null) {
                pollutantData = new ArrayList<Map>();
                thresholdData.put(pollutant, pollutantData);
            }

            Map<String,Object> newThreshold = new HashMap<>();
            newThreshold.put("indexClass", getValue(obj, "indexClass"));
            newThreshold.put("minConcentration", new Integer(getValue(obj, "minConcentration")));
            newThreshold.put("maxConcentration", new Integer(getValue(obj, "maxConcentration")));

            pollutantData.add(newThreshold);
        }
    }

    /**
     *   Obtains the air quality indexes for different pollutants
     *
     *   A map of values which represent an hourly-based value is passed as parameter
     *
     *   @param attributes
     *   @return
     *
     */
    private Map getAirQualityIndexes(Map<String, Double> attributes) throws Exception {
        loadData();

        Map<String, Map> out = new HashMap<>();

        for (String pollutant : attributes.keySet()) {
            double measuredConcentration = attributes.get(pollutant);
            List<Map> pollutantData = thresholdData.get(pollutant);

            if(pollutantData != null) {
                String clazz = null;
                double maxConcentration = -1;
                double minConcentration = -1;

                for (Map values : pollutantData) {
                    minConcentration = ((Integer)values.get("minConcentration")).doubleValue();
                    maxConcentration = ((Integer)values.get("maxConcentration")).doubleValue();
                    if (measuredConcentration >= minConcentration &&
                            measuredConcentration <= maxConcentration) {
                        clazz = (String)values.get("indexClass");
                        break;
                    }
                }

                if (clazz != null) {
                    // Now the air quality index can be calculated
                    Map indexCharacteristics = indexClasses.get(clazz);

                    double iStart = ((Integer)indexCharacteristics.get("startValue")).doubleValue();
                    double iEnd = ((Integer)indexCharacteristics.get("endValue")).doubleValue();

                    double index = ((iEnd - iStart) / (maxConcentration - minConcentration)) *
                            (measuredConcentration - minConcentration) + iStart;

                    Map<String,Object> result = new HashMap<>();
                    result.put("value", new Integer((int)Math.round(index)));
                    result.put("class", clazz);
                    result.put("description", indexCharacteristics.get("description"));

                    out.put(pollutant, result);
                }

            }
        }

        return out;
    }
}
