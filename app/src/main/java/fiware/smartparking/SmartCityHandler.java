package fiware.smartparking;

import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.here.android.mpa.mapping.MapPolygon;

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
    private int renderedEntities;

    protected Map<String,Object> doInBackground(SmartCityRequest... request) {
        SmartCityRequest input = request[0];
        List<Entity> data = input.data;

        Log.d(Application.TAG, "Smart-City Onroute data found: " + input.data.size());

        String str = null;
        List<Entity> parkings = new ArrayList<Entity>();
        List<Entity> environment = new ArrayList<Entity>();

        Map<String,Object> output = new HashMap<String, Object>();

        for (Entity ent : data) {
            // Avoid rendering the same info two times
           if (input.renderedEntities.get(ent.id) != null) {
               continue;
           }

           if (ent.type.equals(Application.AMBIENT_OBSERVED_TYPE)) {
               environment.add(ent);
               renderedEntities++;
           }
           else if (ent.type.equals(Application.PARKING_LOT_TYPE) ||
                   ent.type.equals(Application.STREET_PARKING_TYPE)) {
                parkings.add(ent);
           }
           input.renderedEntities.put(ent.id, ent.id);
        }

        ParkingRenderer.render( Application.mainActivity.getApplicationContext(),
                                input.map, parkings);

        if(environment.size() > 0) {
            input.tts.speak("Smart City Data", TextToSpeech.QUEUE_ADD, null, "AnnounceCity");
            for(int j = 0; j < environment.size(); j++) {
                new CityDataRenderer().renderData(input.map, input.tts, environment.get(j));
            }
        }

        return output;
    }

    protected void onPostExecute(Map<String, Object> out) {
        listener.onRendered(out, renderedEntities);
    }

    public void setListener(RenderListener list) {
        this.listener = list;
    }
}