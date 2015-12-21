package fiware.smartparking;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.Scene;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.routing.RouteManager;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.search.Address;
import com.here.android.mpa.search.ErrorCode;
import com.here.android.mpa.search.GeocodeRequest;
import com.here.android.mpa.search.ResultListener;
import com.here.android.mpa.search.ReverseGeocodeRequest;
import com.here.android.mpa.search.TextSuggestionRequest;
import com.here.android.mpa.search.Location;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jmcf on 23/10/15.
 */
public class RouteActivity implements LocationListener {
    private ProgressDialog progress, locationProgress;

    private AutoCompleteTextView origin, destination, city, originCity;
    private Button nextButton;

    private ArrayAdapter<String> originAdapter;
    private ArrayAdapter<String> destinationAdapter;
    private List<String> optionList1 = new ArrayList<String>();
    private List<String> optionList2 = new ArrayList<String>();
    private static String[] CITIES = new String[] {
            "Porto",
            "Guadalajara",
            "Valencia",
            "Barcelona",
            "Aveiro",
            "Amsterdam"
    };
    private static double[][] CITY_COORDS = new double[][] {
            { 41.14946, -8.61031 },
            { 40.63018, -3.16446 },
            { 39.46868,-0.37691 },
            { 41.38561, 2.16873 },
            { 40.64123, -8.65391 },
            { 52.3731, 4.89329 }
    };

    private static Map<String, double[]> cityCoords = new HashMap<String, double[]>();

    static {
        for(int j = 0; j < CITIES.length; j++) {
          cityCoords.put(CITIES[j], CITY_COORDS[j]);
        }
    }

    private List<String> cityList = Arrays.asList(CITIES);

    private Drawable x, y;

    private String currentStep = "Origin";
    private RouteData routeData = new RouteData();

    private static Activity activity;

    private Context context;

    public RouteActivity(Context ctx) {
        context = ctx;
        activity = Application.mainActivity;

        x = activity.getResources().getDrawable(R.drawable.clear);
        x.setBounds(0, 0,50, 50);

        y = activity.getResources().getDrawable(R.drawable.search);
        y.setBounds(0, 0, 50, 50);
    }

    public void start() {
        ViewGroup rootContainer = (ViewGroup) activity.findViewById(R.id.mainFrame);
        activity.getLayoutInflater().inflate(R.layout.route, rootContainer);

        goToOriginStep();
    }

    private void goToOriginStep() {
        ViewGroup routeContainer = (ViewGroup) activity.findViewById(R.id.frameRoute);
        Scene scene1 = Scene.getSceneForLayout(routeContainer, R.layout.activity_route, activity);

        TransitionManager.go(scene1);

        setupNextEventHandler();
        setupHeader(1);
        setAutoCompleteHandlerOrigin();
    }

    private void goToDestinationStep() {
        ViewGroup routeContainer = (ViewGroup) activity.findViewById(R.id.frameRoute);
        Scene scene1 = Scene.getSceneForLayout(routeContainer, R.layout.activity_route_2, activity);
        TransitionManager.go(scene1);

        setupNextEventHandler();
        setupHeader(2);
        setAutoCompleteHandlerDestination();
    }

    private void goToParkingStep() {
        ViewGroup routeContainer = (ViewGroup) activity.findViewById(R.id.frameRoute);
        Scene scene1 = Scene.getSceneForLayout(routeContainer, R.layout.activity_route_3, activity);
        TransitionManager.go(scene1);

        Spinner sp = (Spinner)activity.findViewById(R.id.parkingDistance);
        int position = (routeData.parkingDistance - 400) / 100;
        sp.setSelection(position);

        CheckBox cb = (CheckBox)activity.findViewById(R.id.chkIndoor);
        CheckBox cb2 = (CheckBox)activity.findViewById(R.id.chkOutdoor);

        if(routeData.parkingCategory.contains("StreetParking")) {
            cb2.setChecked(true);
        }

        if(routeData.parkingCategory.contains("ParkingLot")) {
            cb.setChecked(true);
        }

        setupAutoComplateHandlerParking();

        setupNextEventHandler();
        setupHeader(3);
    }

    private void setupHeader(int step) {
        Toolbar myToolbar = (Toolbar) activity.findViewById(R.id.my_toolbar);
        ((AppCompatActivity)activity).setSupportActionBar(myToolbar);

        ((AppCompatActivity)activity).getSupportActionBar().setTitle("Route Planning " + step + "/3");
        ((AppCompatActivity)activity).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void setupNextEventHandler() {
        nextButton = ((Button)activity.findViewById(R.id.nextButton1));
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextStep(v);
            }
        });
    }

    private AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick (AdapterView<?> parent, View view, int position, long id) {
            InputMethodManager mgr = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            mgr.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }
    };

    private void setupAutoComplateHandlerParking() {
        AutoCompleteTextView vehicle = (AutoCompleteTextView)activity.findViewById(R.id.vehicleInput);

        String[] vehicles = activity.getResources().getStringArray(R.array.Vehicles);
        ArrayAdapter adapter = new ArrayAdapter<String>(activity,
                android.R.layout.simple_list_item_1, vehicles);
        vehicle.setAdapter(adapter);

        vehicle.addTextChangedListener(new MyTextWatcher(vehicle, adapter));

        vehicle.setCompoundDrawables(y, null, null, null);
        vehicle.setOnTouchListener(new MyTouchListener(vehicle));

        vehicle.setOnItemClickListener(itemClickListener);

        vehicle.setText(routeData.vehicle);
    }

    private void setAutoCompleteHandlerOrigin() {
        origin = (AutoCompleteTextView)activity.findViewById(R.id.editText);
        originAdapter = new ArrayAdapter<String>(activity,
                android.R.layout.simple_dropdown_item_1line, optionList1);
        origin.setAdapter(originAdapter);
        origin.addTextChangedListener(new MyTextWatcher(origin, originAdapter));

        origin.setCompoundDrawables(y, null, null, null);
        origin.setOnTouchListener(new MyTouchListener(origin));

        origin.setText(routeData.origin);

        originCity = (AutoCompleteTextView)activity.findViewById(R.id.originCityInput);
        originCity.setCompoundDrawables(y, null, null, null);
        ArrayAdapter originCityAdapter = new ArrayAdapter<String>(activity,
                android.R.layout.simple_dropdown_item_1line, cityList);
        originCity.setAdapter(originCityAdapter);
        originCity.setOnTouchListener(new MyTouchListener(originCity));
        originCity.addTextChangedListener(new MyTextWatcher(originCity, originCityAdapter));

        originCity.setText(routeData.originCity);

        origin.setOnItemClickListener(itemClickListener);

        if (routeData.originCity.equals("")) {
            originCity.requestFocus();
        }

        originCity.setOnItemClickListener(itemClickListener);

        ((ImageButton) activity.findViewById(R.id.currentLocButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateCurrentposition();
            }
        });

        checkNextButton();
    }

    private void setAutoCompleteHandlerDestination() {
        destination = (AutoCompleteTextView)activity.findViewById(R.id.editText2);
        destinationAdapter = new ArrayAdapter<String>(activity,
                android.R.layout.simple_dropdown_item_1line, optionList2);
        destination.setAdapter(destinationAdapter);
        destination.addTextChangedListener(new MyTextWatcher(destination, destinationAdapter));

        destination.setCompoundDrawables(y, null, null, null);
        destination.setOnTouchListener(new MyTouchListener(destination));

        destination.setOnItemClickListener(itemClickListener);

        city = (AutoCompleteTextView)activity.findViewById(R.id.cityInput);
        ArrayAdapter cityAdapter = new ArrayAdapter<String>(activity,
                android.R.layout.simple_dropdown_item_1line, cityList);
        city.setAdapter(cityAdapter);

        city.setCompoundDrawables(y, null, null, null);
        city.setOnTouchListener(new MyTouchListener(city));
        city.addTextChangedListener(new MyTextWatcher(city, cityAdapter));

        city.setOnItemClickListener(itemClickListener);

        city.setText(routeData.city);
        destination.setText(routeData.destination);

        if (routeData.city.equals("")) {
            city.requestFocus();
        }

        checkNextButton();
    }

    public void back() {
        if(currentStep.equals("Origin")) {
            currentStep = "";
            ((MainActivity)activity).onRouteCanceled();
        }
        else if(currentStep.equals("Destination")) {
            routeData.city = city.getText().toString();
            routeData.destination = destination.getText().toString();
            currentStep = "Origin";
            goToOriginStep();
        }
        else if(currentStep.equals("Parking")) {
            currentStep = "Destination";

            fillParkingPreferences();
            goToDestinationStep();
        }
    }

    private void nextStep(View v) {
        if (currentStep.equals("Origin")) {
            routeData.origin = origin.getText().toString();
            routeData.originCity = originCity.getText().toString();
            currentStep = "Destination";
            goToDestinationStep();
        }
        else if(currentStep.equals("Destination")) {
            routeData.destination = destination.getText().toString();
            routeData.city = city.getText().toString();
            currentStep = "Parking";
            goToParkingStep();
        }
        else if(currentStep.equals("Parking")) {
             fillParkingPreferences();

             progress = ProgressDialog.show(activity, "Route Calculation",
                     "We are calculating a route", true);
            calculateRoute();
        }
    }

    private void fillParkingPreferences() {
        routeData.parkingCategory.clear();

        Spinner sp = (Spinner)activity.findViewById(R.id.parkingDistance);
        int selectedVal = activity.getResources().
                getIntArray(R.array.distance_array_values)[sp.getSelectedItemPosition()];

        routeData.parkingDistance = selectedVal;

        CheckBox cb = (CheckBox)activity.findViewById(R.id.chkIndoor);
        if(cb.isChecked()) {
            routeData.parkingCategory.add("ParkingLot");
        }
        CheckBox cb2 = (CheckBox)activity.findViewById(R.id.chkOutdoor);
        if(cb2.isChecked()) {
            routeData.parkingCategory.add("StreetParking");
        }

        AutoCompleteTextView vehicle = (AutoCompleteTextView)activity.findViewById(R.id.vehicleInput);
        routeData.vehicle = vehicle.getText().toString();
    }

    private void calculateCurrentposition() {
        locationProgress = ProgressDialog.show(Application.mainActivity, "FIWARE-HERE",
                "Obtaining current location", true);

        Application.mainActivity.calculateCurrentPosition(this);
    }

    public void onLocationReady(GeoCoordinate coords) {
        locationProgress.dismiss();

        if(coords != null) {
            ReverseGeocodeRequest req = new ReverseGeocodeRequest(coords);
            req.execute(new ResultListener<Address>() {
                @Override
                public void onCompleted(Address address, ErrorCode errorCode) {
                    originCity.setText(address.getCity());
                    origin.setText(address.getText());
                    origin.setEnabled(true);
                }
            });
        }
    }

    private class MyTouchListener implements View.OnTouchListener {
        private AutoCompleteTextView field;

        public MyTouchListener(AutoCompleteTextView f) {
            field = f;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (field.getCompoundDrawables()[2] == null) {
                return false;
            }
            if (event.getAction() != MotionEvent.ACTION_UP) {
                return false;
            }
            if (event.getX() > field.getWidth() - field.getPaddingRight() - x.getIntrinsicWidth()) {
                field.setText("");
                field.setCompoundDrawables(y, null, null, null);
            }
            return false;
        }
    };

    private void doCalculateRoute(GeoCoordinate start, GeoCoordinate end) {
        // Initialize RouteManager
        RouteManager routeManager = new RouteManager();

        // 3. Select routing options via RoutingMode
        RoutePlan routePlan = new RoutePlan();
        RouteOptions routeOptions = new RouteOptions();
        routeOptions.setTransportMode(RouteOptions.TransportMode.CAR);
        routeOptions.setRouteType(RouteOptions.Type.FASTEST);
        routePlan.setRouteOptions(routeOptions);

        routePlan.addWaypoint(start);
        routePlan.addWaypoint(end);

        // Retrieve Routing information via RouteManagerListener
        RouteManager.Error error =
                routeManager.calculateRoute(routePlan, new RouteManager.Listener() {
                    @Override
                    public void onCalculateRouteFinished(RouteManager.Error errorCode, List<RouteResult> result) {
                        if (errorCode == RouteManager.Error.NONE && result.get(0).getRoute() != null) {
                            routeData.route = result.get(0).getRoute();
                            progress.dismiss();
                            ((MainActivity)activity).onRouteReady(routeData);
                        }
                        else {
                            Alert.show(activity.getApplicationContext(),"Error while obtaining route");
                        }
                    }

                    public void onProgress(int progress) {

                    }
                });

        if (error != RouteManager.Error.NONE) {
            Log.e("FIWARE-HERE", "Error while obtaining route: " + error);
        }
    }

    private void calculateRoute() {
        final GeoCoordinate originCoordinates = getCoordForCity(routeData.originCity);
        final GeoCoordinate destCoordinates = getCoordForCity(routeData.city);

        String originStr = routeData.origin;
        if(originStr.indexOf(routeData.originCity) == -1) {
            originStr += "," + routeData.originCity;
        }

        GeocodeRequest req1 = new GeocodeRequest(originStr);
        req1.setSearchArea(originCoordinates, 10000);
        req1.execute(new ResultListener<List<Location>>() {
            public void onCompleted(List<Location> data, ErrorCode error) {
                if(error == ErrorCode.NONE && data != null && data.size() > 0) {
                    final GeoCoordinate geoOrigin = data.get(0).getCoordinate();
                    routeData.originCoordinates = geoOrigin;

                    String destinationStr = routeData.destination;
                    if(destinationStr.indexOf(routeData.city) == -1) {
                        destinationStr += "," + routeData.city;
                    }

                    GeocodeRequest req2 = new GeocodeRequest(destinationStr);
                    req2.setSearchArea(destCoordinates, 10000);
                    req2.execute(new ResultListener<List<Location>>() {
                        public void onCompleted(List<Location> data, ErrorCode error) {
                            if(error == ErrorCode.NONE && data != null && data.size() > 0) {
                                GeoCoordinate geoDestination = data.get(0).getCoordinate();
                                routeData.destinationCoordinates = geoDestination;
                                doCalculateRoute(geoOrigin, geoDestination);
                            }
                            else {
                                Alert.show(Application.mainActivity, "Error while Geocoding locations");
                            }
                        }
                    });
                }
                else {
                    Alert.show(Application.mainActivity, "Error while Geocoding locations");
                }
            }
        });
    }

    private void checkNextButton() {
        if (currentStep.equals("Origin")) {
            if(origin.getText().length() > 0 && originCity.getText().length() > 0) {
                nextButton.setEnabled(true);
            }
            else {
                nextButton.setEnabled(false);
            }
        }
        else if(currentStep.equals("Destination")) {
            if(city.getText().length() > 0 && destination.getText().length() > 0) {
                nextButton.setEnabled(true);
            }
            else {
                nextButton.setEnabled(false);
            }
        }
    }

    private GeoCoordinate getCoordForCity(String city) {
        // If city is not known, for the moment we return default coords
        double[] coords = cityCoords.get(city);
        GeoCoordinate geoCoordinates = MainActivity.DEFAULT_COORDS;

        if(coords != null) {
            geoCoordinates = new GeoCoordinate(coords[0], coords[1]);
        }

        return geoCoordinates;
    }

    private class MyTextWatcher implements TextWatcher {
        private AutoCompleteTextView view;
        private ArrayAdapter<String> adapter;
        // Previous text
        private CharSequence prevText = "";
        private boolean pendingRequest = false;

        private void checkRemoveButton() {
            view.setCompoundDrawables(y, null,
                    view.getText().toString().equals("") ? null : x, null);
        }

        public MyTextWatcher(AutoCompleteTextView v, ArrayAdapter<String> a) {
            view = v;
            adapter = a;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            checkRemoveButton();

            checkNextButton();

            String tag = (String)view.getTag();
            if(tag == null || tag.indexOf("Address") == -1) {
                return;
            }

            // Nothing is done if text refines previous text
            if(pendingRequest || start == prevText.length() &&
                    s.length() > 4 && adapter.getCount() > 0) {
                prevText = new StringBuilder(s);
                return;
            }

            // Starting with 4 chars is when we query
            if(s.length() >= 4 && prevText.length() < 4 ||
                    (s.length() >=4 && s.toString().indexOf(prevText.toString()) == -1 &&
                            prevText.toString().indexOf(s.toString()) == -1)) {
                TextSuggestionRequest req = new TextSuggestionRequest(s.toString());

                GeoCoordinate searchCenter = MainActivity.DEFAULT_COORDS;
                if(view.getTag() != null && view.getTag().equals("originAddress")) {
                    searchCenter = getCoordForCity(originCity.getText().toString());
                }
                else {
                    if(view.getTag() != null && view.getTag().equals("destAddress")) {
                        searchCenter = getCoordForCity(city.getText().toString());
                    }
                }

                req.setSearchCenter(searchCenter);
                pendingRequest = true;
                req.execute(new ResultListener<List<String>>() {
                    @Override
                    public void onCompleted(List<String> strings, ErrorCode errorCode) {
                        pendingRequest = false;
                        adapter = new ArrayAdapter<String>(activity,
                                android.R.layout.simple_dropdown_item_1line, strings);
                        view.setAdapter(adapter);
                        adapter.notifyDataSetChanged();
                    }
                });
            }
            else if(prevText.length() >= 4 && s.length() < 4){
                adapter = new ArrayAdapter<String>(activity,
                        android.R.layout.simple_dropdown_item_1line, new ArrayList<String>());
                view.setAdapter(adapter);
                view.dismissDropDown();
                adapter.notifyDataSetChanged();
            }

            prevText = new StringBuilder(s);
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };
}
