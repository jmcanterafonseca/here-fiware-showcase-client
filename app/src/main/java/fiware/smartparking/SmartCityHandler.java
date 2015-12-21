package fiware.smartparking;

import android.os.AsyncTask;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jmcf on 12/11/15.
 */
public class SmartCityHandler extends AsyncTask<SmartCityRequest, Integer, String> {

    private RenderListener listener;
    private int renderedEntities;

    protected String doInBackground(SmartCityRequest... request) {
        SmartCityRequest input = request[0];
        List<Entity> data = input.data;

        Log.d("FIWARE-HERE", "Smart-City Onroute data found: " + input.data.size());

        String str = null;
        List<Entity> parkings = new ArrayList<Entity>();
        List<Entity> environment = new ArrayList<Entity>();

        for(int j = 0; j < data.size(); j++) {
            Entity ent = data.get(j);

            // Avoid rendering the same info two times
            if (input.renderedEntities.get(ent.id) != null) {
                continue;
            }

            if(ent.type.equals("EnvironmentEvent")) {
                environment.add(ent);
                renderedEntities++;
            }
            else {
               parkings.add(ent);
            }

            input.renderedEntities.put(ent.id, ent.id);
        }

        ParkingRenderer.render(Application.mainActivity.getApplicationContext(), input.map,
                                                                                        parkings);

        if(environment.size() > 0) {
            input.tts.speak("Smart City Data", TextToSpeech.QUEUE_ADD, null, "AnnounceCity");
            for(int j = 0; j < environment.size(); j++) {
                str = CityDataRenderer.renderData(input.map,input.tts,environment.get(j));
            }
        }

        return str;
    }

    protected void onPostExecute(String str) {
        listener.onRendered(str, renderedEntities);
    }

    public void setListener(RenderListener list) {
        this.listener = list;
    }
}