package fiware.smartparking;

import com.here.android.mpa.common.GeoCoordinate;

/**
 *
 *  Listens for new locations available
 *
 */
public interface LocationListener {
    void onLocationReady(GeoCoordinate coord);
}
