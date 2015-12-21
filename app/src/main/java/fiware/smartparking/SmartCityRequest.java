package fiware.smartparking;

import android.speech.tts.TextToSpeech;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.mapping.Map;

import java.util.List;

/**
 * Created by jmcf on 12/11/15.
 */
public class SmartCityRequest {
    public Map map;
    public TextToSpeech tts;
    public GeoCoordinate loc;
    public java.util.Map renderedEntities;
    public List<Entity> data;
    public String context;

    public RelativeLayout dataContainer;
    public TextView scityData;
}
