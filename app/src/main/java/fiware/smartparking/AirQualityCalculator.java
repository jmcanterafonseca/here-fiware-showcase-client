package fiware.smartparking;

import java.util.HashMap;
import java.util.Map;

/**
 *   Calculates air quality according to the concentration values
 *   measured by sensors
 *
 */
public class AirQualityCalculator {
    public static String getAirQualityIndex(Map attributes) {
        String out = null;

        if(attributes.get("O3") != null) {
            double ozoneLevel = ((Double) attributes.get("O3")).doubleValue();

            if (ozoneLevel < 50) {
                out = GOOD;
            } else if (ozoneLevel < 100) {
                out = MODERATE;
            } else if (ozoneLevel < 150) {
                out = USG;
            } else if (ozoneLevel < 200) {
                out = UNHEALTHY;
            } else if (ozoneLevel < 300) {
                out = VERY_UNHEALTHY;
            } else {
                out = HAZARDOUS;
            }
        }

        return out;
    }

    public static String GOOD           = "Good";
    public static String MODERATE       = "Moderate";
    public static String USG            = "USG";
    public static String UNHEALTHY      = "Unhealthy";
    public static String VERY_UNHEALTHY = "Very_Unhealthy";
    public static String HAZARDOUS      = "Hazardous";

    public static String USG_DESC = "Unhealthy for Sensitive Groups";
    public static String VERY_UNHEALTHY_DESC = "Very Unhealthy";

    public static String[] STATES = new String[]{
            GOOD, MODERATE, USG, UNHEALTHY, VERY_UNHEALTHY, HAZARDOUS };

    public static String[] DESCRIPTIONS = new String[] {
         GOOD, MODERATE, USG_DESC, UNHEALTHY, VERY_UNHEALTHY_DESC, HAZARDOUS
    };

    public static Map<String, String> mapDescriptions = new HashMap<String, String>();

    static {
        for(int j = 0; j < STATES.length; j++) {
            mapDescriptions.put(STATES[j], DESCRIPTIONS[j]);
        }
    }
}
