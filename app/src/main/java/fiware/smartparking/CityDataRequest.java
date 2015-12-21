package fiware.smartparking;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jmcf on 9/11/15.
 */
public class CityDataRequest {
    public List<String> types = new ArrayList<String>();
    public double[] coordinates;
    public int radius;
}
