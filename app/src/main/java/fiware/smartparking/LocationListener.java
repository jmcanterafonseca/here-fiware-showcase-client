package fiware.smartparking;

import com.here.android.mpa.common.GeoCoordinate;

/**
 * Created by jmcf on 9/11/15.
 */
public interface LocationListener {
    public void onLocationReady(GeoCoordinate coord);
}
