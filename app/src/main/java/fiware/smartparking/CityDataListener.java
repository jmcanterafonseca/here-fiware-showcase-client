package fiware.smartparking;

import java.util.List;

/**
 *  Interface for listeners when city data retrieval tasks finish
 *
 *
 */
public interface CityDataListener {
    public void onCityDataReady(List<Entity> data);
}
