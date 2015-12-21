package fiware.smartparking;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jmcf on 9/11/15.
 */
public class Entity {
    public String id;
    public String type = "";
    public Map<String, Object> attributes = new HashMap<String, Object>();
    public double[] location;
}
