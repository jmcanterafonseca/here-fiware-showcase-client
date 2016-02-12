package fiware.smartparking;

import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *   Handles data coming from the smart city
 *
 *
 */
public class SmartCityHandler extends AsyncTask<SmartCityRequest, Integer, Map<String,Object>> {
    private RenderListener listener;

    protected Map<String,Object> doInBackground(SmartCityRequest... request) {
        SmartCityRequest input = request[0];
        Map<String, List<Entity>> data = input.data;

        Log.d(Application.TAG, "SmartCity on route data found: " + input.data.size());

        List<Entity> parkings = data.get(Application.PARKING_LOT_TYPE);
        if (parkings == null) {
            parkings = new ArrayList<>();
        }

        List<Entity> streetParkings = data.get(Application.STREET_PARKING_TYPE);
        if (streetParkings != null) {
            parkings.addAll(streetParkings);
        }

        List<Entity> environment = data.get(Application.AMBIENT_OBSERVED_TYPE);
        List<Entity> weather = data.get(Application.WEATHER_FORECAST_TYPE);

        Map<String,Object> output = new HashMap<String, Object>();

        ParkingRenderer.render(Application.mainActivity.getApplicationContext(),
                                input.map, parkings);

        if (environment != null && environment.size() > 0) {
            input.tts.playEarcon("smart_city", TextToSpeech.QUEUE_ADD, null, "AnnounceCity");
            for (Entity ent : environment) {
                if(Application.renderedEntities.get(ent.id) == null) {
                    new CityDataRenderer().renderData(input.map, input.tts, ent);
                    Application.renderedEntities.put(ent.id, ent.id);
                }
            }
        }

        if (weather != null && weather.size() > 0) {
            // Here Weather is processed
            Entity forecast = null;

            //  Obtain current WeatherForecast
            long accuracy = Long.MAX_VALUE;
            long after = Long.MAX_VALUE;
            DateTime now = new DateTime();
            for(Entity ow: weather) {
                Map<String, String> valid = (Map<String, String>)ow.attributes.get("validity");
                if (valid != null) {
                    String from = valid.get("from");
                    String to = valid.get("to");

                    DateTimeFormatter parser    = ISODateTimeFormat.dateTimeParser();
                    DateTime dateFrom = parser.parseDateTime(from);
                    DateTime dateTo = parser.parseDateTime(to);

                    if (dateTo.isAfterNow()) {
                        long aux = dateTo.getMillis() - now.getMillis();
                        long aux2 = dateTo.getMillis() - dateFrom.getMillis();
                        if (aux2 <= accuracy && aux <= after) {
                            accuracy = aux2;
                            after = aux;
                            forecast = ow;
                        }
                    }
                }
            }

            if (forecast != null) {
                Log.d("Weather forecast: ", forecast.id);
                output.put(Application.WEATHER_FORECAST_ENTITY, forecast);
            }
        }

        ExtraObjRenderer.render(Application.mainActivity.getApplicationContext(),
                input.map, data);

        return output;
    }

    protected void onPostExecute(Map<String, Object> out) {
        // Rendered entities not used. TODO: Refactoring
        listener.onRendered(out, -1);
    }

    public void setListener(RenderListener list) {
        this.listener = list;
    }
}