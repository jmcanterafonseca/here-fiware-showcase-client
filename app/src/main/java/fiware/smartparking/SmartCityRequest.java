package fiware.smartparking;

import android.speech.tts.TextToSpeech;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapPolygon;

import java.util.List;

/**
 *   This class holds data for the SmartCityHandler which will launch
 *   the rendering process
 *
 */
public class SmartCityRequest {
    public Map map;
    public TextToSpeech tts;
    public GeoCoordinate loc;
    public java.util.Map renderedEntities;
    public List<Entity> data;
    public String context;
}
