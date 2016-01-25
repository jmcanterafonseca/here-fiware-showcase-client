package fiware.smartparking;

import com.here.android.mpa.mapping.MapPolygon;

/**
 *   Render listener for AmbientArea
 *
 */
public interface AmbientRenderListener {
    public void onRendered(String level, MapPolygon polygon);
}
