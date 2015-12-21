package fiware.smartparking;

import java.util.List;

/**
 * Created by jmcf on 9/11/15.
 */
public interface CityDataListener {
    public void onCityDataReady(List<Entity> data);
}
