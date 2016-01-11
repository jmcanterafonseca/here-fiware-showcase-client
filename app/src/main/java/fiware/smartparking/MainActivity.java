package fiware.smartparking;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v7.app.AppCompatActivity;
import android.transition.Scene;
import android.transition.TransitionManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.here.android.mpa.ar.ARObject;
import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.IconCategory;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.common.Version;
import com.here.android.mpa.common.ViewObject;
import com.here.android.mpa.common.ViewRect;
import com.here.android.mpa.guidance.NavigationManager;
import com.here.android.mpa.guidance.VoiceCatalog;
import com.here.android.mpa.guidance.VoiceSkin;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapGesture;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapObject;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.mapping.MapState;
import com.here.android.mpa.routing.Maneuver;
import com.here.android.mpa.routing.Route;
import com.here.android.mpa.routing.RouteManager;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteTta;
import com.here.android.mpa.search.Address;
import com.here.android.mpa.search.ErrorCode;
import com.here.android.mpa.search.GeocodeRequest;
import com.here.android.mpa.search.ImageMedia;
import com.here.android.mpa.search.Location;
import com.here.android.mpa.search.ResultListener;
import com.here.android.mpa.search.ReverseGeocodeRequest2;
import com.here.android.mpa.search.TextSuggestionRequest;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity implements LocationListener, CityDataListener {
    private List<MapObject> mapObjects = Application.mapObjects;

    private double currentZoomLevel;

    private boolean loopMode = false;

    private String state = "";
    private boolean inParkingMode = false;
    private boolean parkingFound, pendingParkingRequest = false;

    private int parkingRadius;

    private PopupMenu popupMenu;

    private ProgressDialog locationProgress;

    private LinearLayout dataContainer;

    // map embedded in the map fragment
    private Map map = null;
    // map fragment embedded in this activity
    private MapFragment mapFragment = null;

    private ImageButton locationButton, menuButton;
    private ImageView fiwareImage;

    // Oporto downtown
    public static GeoCoordinate DEFAULT_COORDS;

    private GeoCoordinate lastKnownPosition;

    private NavigationManager navMan;
    private PositioningManager posMan;

    private static String destination;

    private TextView nextRoad, currentSpeed, ETA, distance, currentRoad, nextManouverDistance;
    private ImageView turnNavigation;

    private TextView parkingData;
    private ImageView parkingSign;

    private RouteActivity routeWizard;

    private RouteData routeData;

    private boolean underSimulation = false;

    private VoiceSkin voiceSkin;

    private long previousDistance;

    private TextToSpeech tts;

    private java.util.Map<String, String> renderedEntities = new HashMap<String, String>();

    private TextView scityData;

    public void onCityDataReady(List<Entity> data) {
       if(data.size() == 0) {
           return;
       }

       SmartCityRequest req = new SmartCityRequest();
       req.map = map;
       req.data = data;
       req.tts = tts;
       req.context = "Environment";
       req.renderedEntities = renderedEntities;

       Toast.makeText(getApplicationContext(), "SmartCity data on route",
               Toast.LENGTH_LONG).show();

       SmartCityHandler sch = new SmartCityHandler();
       currentZoomLevel = map.getZoomLevel();
       sch.setListener(new RenderListener() {
           @Override
           public void onRendered(Object data, int num) {
           }
       });

       sch.execute(req);
    }

    public void setVoiceSkin(VoiceSkin vs) {
        voiceSkin = vs;

        if(underSimulation) {
            if(navMan != null) {
                navMan.setVoiceSkin(voiceSkin);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                routeWizard.back();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void goHome() {
        // Set the map center to Oporto center
        goTo(map, DEFAULT_COORDS, Map.Animation.LINEAR);
    }



    /**
     * Stops navigation manager.
     */
    private void stopNavigationManager() {
        if (navMan == null) {
            return;
        }

        if (navMan.getRunningState() != NavigationManager.NavigationState.IDLE) {
            navMan.stop();
        }
    }

    private void goTo(Map map, GeoCoordinate coordinates, Map.Animation animation) {
        map.setCenter(coordinates, animation, map.getMaxZoomLevel() - 7, 0, map.getMaxTilt() / 2);
        map.setMapScheme(Map.Scheme.CARNAV_DAY);

        lastKnownPosition = coordinates;
    }

    private MapGesture.OnGestureListener gestureListener = new MapGesture.OnGestureListener() {
        @Override
        public void onPanStart() {

        }

        @Override
        public void onPanEnd() {

        }

        @Override
        public void onMultiFingerManipulationStart() {

        }

        @Override
        public void onMultiFingerManipulationEnd() {

        }

        @Override
        public boolean onMapObjectsSelected(List<ViewObject> list) {
            return false;
        }

        @Override
        public boolean onTapEvent(PointF pointF) {
            return false;
        }

        @Override
        public boolean onDoubleTapEvent(PointF pointF) {
            return false;
        }

        @Override
        public void onPinchLocked() {

        }

        @Override
        public boolean onPinchZoomEvent(float v, PointF pointF) {
            return false;
        }

        @Override
        public void onRotateLocked() {

        }

        @Override
        public boolean onRotateEvent(float v) {
            return false;
        }

        @Override
        public boolean onTiltEvent(float v) {
            return false;
        }

        @Override
        public boolean onLongPressEvent(PointF pointF) {
            return false;
        }

        @Override
        public void onLongPressRelease() {

        }

        @Override
        public boolean onTwoFingerTapEvent(PointF pointF) {
            return false;
        }
    };

    private void showPopupMenu(ImageButton b) {
        popupMenu.show();//showing popup menu
    }

    private void addMapWidgets() {
        FrameLayout container = (FrameLayout)findViewById(R.id.mainFrame);
        RelativeLayout rl2 = new RelativeLayout(this);
        RelativeLayout.LayoutParams relativeLayoutParams =
                new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT);

        container.addView(rl2, relativeLayoutParams);


        /*
        <ImageButton
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:adjustViewBounds="true"
        android:cropToPadding="false"
        android:scaleType="fitXY"
        android:id="@+id/currentLocButton"
        android:src="@drawable/current_location2" /> */

        locationButton = new ImageButton(this);
        locationButton.setAdjustViewBounds(true);
        locationButton.setCropToPadding(false);
        locationButton.setScaleType(ImageView.ScaleType.FIT_XY);
        locationButton.setImageDrawable(getResources().getDrawable(R.drawable.current_location2));
        locationButton.setBackground(null);

        menuButton = new ImageButton(this);
        menuButton.setAdjustViewBounds(true);
        menuButton.setCropToPadding(false);
        menuButton.setScaleType(ImageView.ScaleType.FIT_XY);
        menuButton.setImageDrawable(getResources().getDrawable(R.drawable.menu));
        menuButton.setBackground(null);

        Resources r = getResources();
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100,
                r.getDisplayMetrics());
        int px2 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60,
                r.getDisplayMetrics());
        RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(px, px);
        RelativeLayout.LayoutParams params3 = new RelativeLayout.LayoutParams(px2, px2);

        DisplayMetrics metrics = r.getDisplayMetrics();
        params2.leftMargin = metrics.widthPixels - px;
        params2.topMargin = metrics.heightPixels - px;
        rl2.addView(locationButton, params2);

        params3.leftMargin = 0;
        params3.topMargin =  metrics.heightPixels - px2;
        rl2.addView(menuButton, params3);

        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                locationProgress = ProgressDialog.show(Application.mainActivity, "FIWARE-HERE",
                        "Obtaining current location", true);
                calculateCurrentPosition(Application.mainActivity);
            }
        });

        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupMenu(menuButton);
            }
        });

        TextView scityTitle = new TextView(this);
        scityTitle.setBackgroundColor(Color.argb(80, 0, 0, 0));
        scityTitle.setText("Smart City");

        scityData = new TextView(this);

        scityData.setTextColor(Color.argb(80, 0, 0, 0));

        dataContainer = new LinearLayout(this);
        dataContainer.setBackgroundResource(R.drawable.rounded);
        dataContainer.setOrientation(LinearLayout.VERTICAL);

        RelativeLayout.LayoutParams paramsContainer = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        paramsContainer.leftMargin = 10;
        paramsContainer.topMargin = 100;
        dataContainer.addView(scityTitle);
        dataContainer.addView(scityData);
        // Not needed now
        // rl2.addView(dataContainer, paramsContainer);
        dataContainer.setVisibility(RelativeLayout.GONE);

        fiwareImage = new ImageView(this);
        fiwareImage.setAdjustViewBounds(true);
        fiwareImage.setCropToPadding(false);
        fiwareImage.setScaleType(ImageView.ScaleType.FIT_XY);
        fiwareImage.setImageDrawable(getResources().getDrawable(R.drawable.fiware));
        fiwareImage.setBackground(null);

        RelativeLayout.LayoutParams paramsLogo = new RelativeLayout.LayoutParams(160, 35);

        paramsLogo.leftMargin = 10;
        paramsLogo.topMargin = 45;
        rl2.addView(fiwareImage,paramsLogo);
        fiwareImage.setVisibility(RelativeLayout.GONE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // remove title
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);
        Application.mainActivity = this;

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.main);

        ViewGroup rootContainer = (ViewGroup)findViewById(R.id.mainFrame);

        getLayoutInflater().inflate(R.layout.activity_main, rootContainer);

        addMapWidgets();

        parkingData = (TextView)findViewById(R.id.parkingData);
        parkingSign = (ImageView)findViewById(R.id.parkingSign);

        popupMenu = new PopupMenu(MainActivity.this, menuButton);
        popupMenu.getMenuInflater().inflate(R.menu.menu_main, popupMenu.getMenu());
        popupMenu.getMenu().setGroupVisible(R.id.restartGroup, false);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.action_simulate) {
                    if (routeWizard == null) {
                        loopMode = false;
                        getDirections(null);
                    }
                } else if (item.getItemId() == R.id.action_home) {
                    ((RelativeLayout) findViewById(R.id.routePlanningLayout)).
                                                                setVisibility(RelativeLayout.GONE);
                    goHome();
                } else if (item.getItemId() == R.id.action_pause) {
                    pauseSimulation();
                } else if (item.getItemId() == R.id.action_terminate) {
                    loopMode = false;
                    terminateSimulation();
                } else if (item.getItemId() == R.id.action_restart) {
                    clearMap();
                    showRoute();
                } else if (item.getItemId() == R.id.action_loop) {
                    if (routeWizard == null) {
                        loopMode = true;
                        getDirections(null);
                    }
                }
                return true;
            }
        });

        nextRoad = (TextView)findViewById(R.id.nextRoad);
        currentSpeed = (TextView)findViewById(R.id.currentSpeed);
        distance = (TextView)findViewById(R.id.distance2);
        ETA = (TextView)findViewById(R.id.eta);
        currentRoad = (TextView)findViewById(R.id.currentRoad);
        nextManouverDistance = (TextView)findViewById(R.id.manouver);

        turnNavigation = (ImageView)findViewById(R.id.nextTurn);

        hideNavigationUI();

        // Search for the map fragment to finish setup by calling init().
        mapFragment = (MapFragment)getFragmentManager().findFragmentById(R.id.mapfragment);

        mapFragment.init(new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {
                if (error == OnEngineInitListener.Error.NONE) {
                    Log.d("FIWARE-HERE", "Version: " + Version.SDK_API_INT);
                    mapFragment.getMapGesture().addOnGestureListener(gestureListener);

                    // retrieve a reference of the map from the map fragment
                    map = mapFragment.getMap();
                    // Oporto downtown
                    DEFAULT_COORDS = new GeoCoordinate(41.162142, -8.621953);
                    goTo(map, DEFAULT_COORDS, Map.Animation.NONE);

                    map.setExtrudedBuildingsVisible(true);
                    map.getPositionIndicator().setVisible(true);
                    map.setLandmarksVisible(true);
                    map.setCartoMarkersVisible(true);
                    // map.setVisibleLayers(Map.LayerCategory.)

                    map.addTransformListener(transformListener);

                    posMan = PositioningManager.getInstance();
                    posMan.start(PositioningManager.LocationMethod.GPS_NETWORK);

                    VoiceNavigation.downloadTargetVoiceSkin(VoiceCatalog.getInstance());

                } else {
                    Log.e("FIWARE HERE", "ERROR: Cannot initialize Map Fragment");
                }
            }
        });

        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                tts.setLanguage(Locale.ENGLISH);
            }
        });

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {

            }

            @Override
            public void onDone(String utteranceId) {
                if(utteranceId.equals("Entity_End")) {
                    map.setZoomLevel(currentZoomLevel, map.projectToPixel(map.getCenter()).getResult(),
                            Map.Animation.LINEAR);
                }
            }

            @Override
            public void onError(String utteranceId) {

            }
        });
    }

    public void calculateCurrentPosition(LocationListener callback) {
        posMan = PositioningManager.getInstance();
        boolean startResult = true;
        if (!posMan.isActive()) {
            startResult = posMan.start(PositioningManager.LocationMethod.GPS_NETWORK);
        }

        if(startResult == true) {
            try {
                LocationTask lt = new LocationTask();
                lt.setListener(callback);
                lt.execute(posMan);
            }
            catch(Exception e) {
                Log.e("FIWARE", "Error while obtaining location");
            }
        }
        else {
            Toast.makeText(getApplicationContext(),
                    "Location services not yet available", Toast.LENGTH_LONG).show();
        }
        /*
        PositioningManager.OnPositionChangedListener posManListener =
                new PositioningManager.OnPositionChangedListener() {
                    public void onPositionUpdated(PositioningManager.LocationMethod method,
                                                  GeoPosition position, boolean isMapMatched) {
                        if (!underSimulation) {
                            goTo(mapFragment.getMap(), position.getCoordinate(), Map.Animation.BOW);
                        }
                    }

                    public void onPositionFixChanged(PositioningManager.LocationMethod method,
                                                     PositioningManager.LocationStatus status) {
                    }
                };
        posMan.addListener(new WeakReference<PositioningManager.OnPositionChangedListener>(posManListener));
        */
    }

    public void onLocationReady(GeoCoordinate coords) {
        locationProgress.dismiss();
        goTo(mapFragment.getMap(), coords, Map.Animation.BOW);
    }

    @Override
    public void onPause() {
        detachNavigationListeners();
        if(posMan != null && posMan.isActive()) {
            posMan.stop();
        }

        if (navMan != null && navMan.getRunningState() == NavigationManager.NavigationState.RUNNING) {
            navMan.pause();
        }

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        if(posMan != null) {
            posMan.start(PositioningManager.LocationMethod.GPS_NETWORK);
        }

        if (navMan != null && navMan.getRunningState() == NavigationManager.NavigationState.PAUSED) {
            attachNavigationListeners();

            NavigationManager.Error error = navMan.resume();

            if (error != NavigationManager.Error.NONE) {
                Toast.makeText(getApplicationContext(),
                        "NavigationManager resume failed: " + error.toString(), Toast.LENGTH_LONG).show();
                return;
            }
        }
    }

    /**
     * Attaches listeners to navigation manager.
     */
    private void attachNavigationListeners() {
        if (navMan != null) {
            navMan.addPositionListener(
                    new WeakReference<NavigationManager.PositionListener>(m_navigationPositionListener));

            navMan.addNavigationManagerEventListener(
                    new WeakReference<NavigationManager.NavigationManagerEventListener>(m_navigationListener));
        }
    }

    /**
     * Detaches listeners from navigation manager.
     */
    private void detachNavigationListeners() {
        if (navMan != null) {
            navMan.removeNavigationManagerEventListener(m_navigationListener);
            navMan.removePositionListener(m_navigationPositionListener);
        }
    }

    public void terminateSimulation() {
        navMan.stop();
        doTerminateSimulation();
    }


    public void pauseSimulation() {
        if(navMan != null) {
           if (navMan.getRunningState() == NavigationManager.NavigationState.RUNNING) {
               popupMenu.getMenu().findItem(R.id.action_pause).setTitle("Resume Simulation");
               navMan.pause();
           }
           else if (navMan.getRunningState() == NavigationManager.NavigationState.PAUSED) {
               popupMenu.getMenu().findItem(R.id.action_pause).setTitle("Pause Simulation");
               navMan.resume();
           }
        }
    }

    // Functionality for taps of the "Get Directions" button
    public void getDirections(View view) {
        clearMap();

        popupMenu.getMenu().setGroupVisible(R.id.restartGroup, false);
        routeWizard = new RouteActivity(getApplicationContext());

        routeWizard.start();
    }

    public void onRouteReady(RouteData r) {
        ViewGroup rootContainer = (ViewGroup)findViewById(R.id.mainFrame);
        rootContainer.removeViewAt(2);

        routeData = r;
        routeWizard = null;

        showRoute();
    }

    public void onRouteCanceled() {
        ViewGroup rootContainer = (ViewGroup)findViewById(R.id.mainFrame);

        routeWizard = null;
        rootContainer.removeViewAt(2);

        InputMethodManager mgr = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);
    }

    private void clearMap() {
        map.removeMapObjects(mapObjects);
        mapObjects.clear();
    }

    private void showRoute() {
        GeoBoundingBox gbb = routeData.route.getBoundingBox();

        map.zoomTo(gbb, Map.Animation.LINEAR, Map.MOVE_PRESERVE_ORIENTATION);

        GeoCoordinate start = routeData.route.getStart();
        Image startImg = new Image();
        try {
            startImg.setImageResource(R.drawable.start);
        }
        catch(IOException e) {
            System.err.println("Cannot load image");
        }
        MapMarker startMarker = new MapMarker(start, startImg);
        map.addMapObject(startMarker);

        Image car = new Image();
        try {
            car.setImageResource(R.drawable.car);
        }
        catch(IOException e) {
            System.err.println("Cannot load image");
        }

        GeoCoordinate end = routeData.route.getDestination();
        Image endImg = new Image();
        try {
            endImg.setImageResource(R.drawable.end);
        }
        catch(IOException e) {
            System.err.println("Cannot load image");
        }
        MapMarker endMarker = new MapMarker(end, endImg);
        map.addMapObject(endMarker);

        MapRoute route = new MapRoute(routeData.route);

        map.addMapObject(route);

        state = "zoomToRouteBB";

        mapObjects.add(startMarker);
        mapObjects.add(endMarker);
        mapObjects.add(route);

        if(loopMode) {
            final int interval2 = 7000; // 7 Second
            Handler handler2 = new Handler();
            Runnable runnable2 = new Runnable() {
                public void run() {
                   startSimulation(null);
                }
            };
            handler2.postDelayed(runnable2, interval2);
        }
    }


    // Called on UI thread
    private final NavigationManager.PositionListener
                        m_navigationPositionListener = new NavigationManager.PositionListener() {
        @Override
        public void onPositionUpdated(final GeoPosition loc) {
            updateNavigationInfo(loc);
        }
    };

    private void showParkingData(String text) {
        parkingData.setText(text);
        parkingData.setVisibility(RelativeLayout.VISIBLE);
        parkingSign.setVisibility(RelativeLayout.VISIBLE);
        nextRoad.setLayoutParams(new LinearLayout.LayoutParams(0,
                RelativeLayout.LayoutParams.MATCH_PARENT, 50));
    }

    private void hideParkingData() {
        parkingData.setText("");
        parkingData.setVisibility(RelativeLayout.GONE);
        parkingSign.setVisibility(RelativeLayout.GONE);
        nextRoad.setLayoutParams(new LinearLayout.LayoutParams(0,
                RelativeLayout.LayoutParams.MATCH_PARENT, 100));
    }

    private void handleParkingMode(final GeoCoordinate coord, long distance) {
        if(!parkingFound && !pendingParkingRequest) {
            CityDataRequest reqData = new CityDataRequest();
            reqData.radius = parkingRadius / 2;
            reqData.coordinates = new double[]{
                    routeData.destinationCoordinates.getLatitude(),
                    routeData.destinationCoordinates.getLongitude()
            };

            reqData.types = routeData.parkingCategory;
            if(reqData.types.size() == 0) {
                reqData.types.add("Parking");
            }

            Log.d("FIWARE-HERE", "Going to retrieve parking data ...");
            CityDataRetriever retriever = new CityDataRetriever();
            retriever.setListener(new CityDataListener() {
                @Override
                public void onCityDataReady(List<Entity> data) {
                    Log.d("FIWARE-HERE", "Parking data available: " + data.size());
                    ParkingRenderer.render(getApplicationContext(), map, data);

                    if(data.size() > 0) {
                        if(data.get(0).type.equals("StreetParking")) {
                            ReverseGeocodeRequest2 req = new ReverseGeocodeRequest2(
                                    new GeoCoordinate( data.get(0).location[0],data.get(0).location[1]));
                            req.execute(new ResultListener<Location>() {
                                @Override
                                public void onCompleted(Location location, ErrorCode errorCode) {
                                    ParkingRenderer.announceParking(tts, location.getAddress().getStreet());
                                    showParkingData(location.getAddress().getStreet());
                                }
                            });
                        }
                        else {
                            // First parking is taken unfortunately they are not ordered by distance
                            String parkingName = (String)data.get(0).attributes.get("name");
                            ParkingRenderer.announceParking(tts, parkingName);
                            showParkingData(parkingName);
                        }

                        ParkingRouteCalculator parkingRoute = new ParkingRouteCalculator();
                        ParkingRouteData prd = new ParkingRouteData();
                        prd.origin = coord;
                        prd.parkingDestination = new GeoCoordinate(
                                data.get(0).location[0],data.get(0).location[1]);

                        parkingRoute.setListener(new RouteCalculationListener() {
                            @Override
                            public void onRouteReady(Route r) {
                                MapRoute mr = new MapRoute(r);
                                mr.setColor(Color.parseColor("#73C2FB"));
                                map.addMapObject(mr);
                                navMan.setRoute(r);
                                mapObjects.add(mr);
                            }
                        });
                        parkingRoute.execute(prd);

                        parkingFound = true;
                    }
                    else {
                        parkingRadius += 100;
                    }
                    pendingParkingRequest = false;
                }
            });
            pendingParkingRequest = true;
            Log.d("FIWARE-HERE", "Asking parking data in a radius of: " + parkingRadius);
            retriever.execute(reqData);
        }
    }

    private void updateNavigationInfo(final GeoPosition loc) {
        // Update the average speed
        int avgSpeed = (int) loc.getSpeed();
        currentSpeed.setText(String.format("%d km/h", (int) (avgSpeed * 3.6)));

        // Update ETA
        SimpleDateFormat sdf = new SimpleDateFormat("k:mm", Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+1"));

        Date ETADate = navMan.getEta(true, Route.TrafficPenaltyMode.DISABLED);
        ETA.setText(sdf.format(ETADate));

        distance.setText(String.format("%d m", navMan.getDestinationDistance()));
        Maneuver nextManeuver = navMan.getNextManeuver();

        if (nextManeuver != null) {
            currentRoad.setText(nextManeuver.getRoadName());
            nextRoad.setText(nextManeuver.getNextRoadName());
            nextManouverDistance.setText(String.format("%d m",
                    navMan.getNextManeuverDistance()));

            int id = getResources().getIdentifier(nextManeuver.getTurn().name().toLowerCase(),
                    "drawable", getPackageName());
            // Returns 0 if not found
            if(id != 0) {
                turnNavigation.setImageResource(id);
            }

        }

        handleSmartCity(loc);
    }

    private void handleSmartCity(GeoPosition loc) {
        long currentDistance = navMan.getDestinationDistance();

        if(currentDistance <= Application.PARKING_DISTANCE) {
            if(!inParkingMode) {
                ParkingRenderer.announceParkingMode(tts);
                parkingRadius = routeData.parkingDistance;
                showParkingData("Searching ... ");
            }
            inParkingMode = true;
        }

        if(inParkingMode) {
            handleParkingMode(loc.getCoordinate(), currentDistance);
        }

        if(currentDistance < Application.THRESHOLD_DISTANCE &&
                (previousDistance  - currentDistance > (Application.DEFAULT_RADIUS - 100)
                        || previousDistance == 0)) {
            previousDistance = currentDistance;

            CityDataRequest reqData = new CityDataRequest();
            reqData.radius = Application.DEFAULT_RADIUS;
            reqData.coordinates = new double[]{loc.getCoordinate().getLatitude(),
                    loc.getCoordinate().getLongitude()};

            reqData.types.add("EnvironmentEvent");
            reqData.types.add("Parking");

            Log.d("FIWARE-HERE", "Going to retrieve data ...");
            CityDataRetriever retriever = new CityDataRetriever();
            retriever.setListener(this);
            retriever.execute(reqData);
        }
    }

    private void doTerminateSimulation() {
        underSimulation = false;

        detachNavigationListeners();

        navMan.setMapUpdateMode(NavigationManager.MapUpdateMode.POSITION);
        navMan.setTrafficAvoidanceMode(NavigationManager.TrafficAvoidanceMode.DISABLE);
        navMan.setMap(null);

        hideNavigationUI();
        // Allow to play the same route again
        popupMenu.getMenu().setGroupVisible(R.id.restartGroup, true);

        previousDistance = 0;
        if(loopMode) {
            final int interval2 = 7000; // 7 Second
            Handler handler2 = new Handler();
            Runnable runnable2 = new Runnable() {
                public void run() {
                    clearMap();
                    showRoute();
                }
            };
            handler2.postDelayed(runnable2, interval2);
        }
    }

    // Called on UI thread
    private final NavigationManager.NavigationManagerEventListener m_navigationListener =
                                            new NavigationManager.NavigationManagerEventListener() {
        @Override
        public void onEnded(final NavigationManager.NavigationMode mode) {
            // NOTE: this method is called in both cases when destination
            // is reached and when NavigationManager is stopped.
            Toast.makeText(getApplicationContext(),
                    "Destination reached!", Toast.LENGTH_LONG).show();

            doTerminateSimulation();

            doTransferRoute();
        }

        private void doTransferRoute() {
            RouteTransfer transferTask = new RouteTransfer();
            Handler handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    int result = msg.getData().getInt(Application.TRANSFER_RESULT);
                    String text = "";
                    if(result == 0) {
                        text = "Route transferred OK";
                    }
                    else {
                        text = "Route transfer error";
                    }
                    Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
                }
            };

            transferTask.setHandler(handler);
            transferTask.execute(routeData);
        }

        @Override
        public void onRouteUpdated(final Route updatedRoute) {
        }
    };

    private void showNavigationUI() {
        findViewById(R.id.routePlanningLayout).setVisibility(RelativeLayout.GONE);

        findViewById(R.id.nextRoadLayout).setVisibility(RelativeLayout.VISIBLE);
        findViewById(R.id.navigationLayout).setVisibility(RelativeLayout.VISIBLE);

        LinearLayout.LayoutParams layoutParams1 = new LinearLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, 0, 0.82f);

        RelativeLayout mapLayout = (RelativeLayout)findViewById(R.id.mainMapLayout);
        mapLayout.setLayoutParams(layoutParams1);

        RelativeLayout innerMapLayout = (RelativeLayout)findViewById(R.id.innerMapLayout);
        LinearLayout.LayoutParams layoutParamsInner = new LinearLayout.LayoutParams(
                 0, RelativeLayout.LayoutParams.MATCH_PARENT, 0.90f);
        innerMapLayout.setLayoutParams(layoutParamsInner);
        ((RelativeLayout)findViewById(R.id.oascDataLayout)).setVisibility(RelativeLayout.VISIBLE);

        popupMenu.getMenu().setGroupVisible(R.id.simulationGroup, true);
        popupMenu.getMenu().setGroupVisible(R.id.initialGroup, false);

        fiwareImage.setVisibility(RelativeLayout.VISIBLE);
    }

    private void hideNavigationUI() {
        locationButton.setVisibility(RelativeLayout.VISIBLE);
        fiwareImage.setVisibility(RelativeLayout.GONE);

        findViewById(R.id.nextRoadLayout).setVisibility(RelativeLayout.GONE);
        findViewById(R.id.navigationLayout).setVisibility(RelativeLayout.GONE);

        LinearLayout.LayoutParams layoutParams1 = new LinearLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);

        RelativeLayout mapLayout = (RelativeLayout)findViewById(R.id.mainMapLayout);
        mapLayout.setLayoutParams(layoutParams1);
        mapLayout.requestLayout();

        RelativeLayout innerMapLayout = (RelativeLayout)findViewById(R.id.innerMapLayout);
        LinearLayout.LayoutParams layoutParamsInner = new LinearLayout.LayoutParams(
                0, RelativeLayout.LayoutParams.MATCH_PARENT, 1.0f);
        innerMapLayout.setLayoutParams(layoutParamsInner);
        ((RelativeLayout)findViewById(R.id.oascDataLayout)).setVisibility(RelativeLayout.GONE);

        if(popupMenu != null) {
            popupMenu.getMenu().setGroupVisible(R.id.initialGroup, true);
            popupMenu.getMenu().setGroupVisible(R.id.simulationGroup, false);
        }

        hideParkingData();
    }

    private void showRoutePlanningUI() {
        locationButton.setVisibility(RelativeLayout.GONE);

        ((RelativeLayout)findViewById(R.id.routePlanningLayout)).setVisibility(RelativeLayout.VISIBLE);

        LinearLayout.LayoutParams layoutParams1 = new LinearLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, 0, 0.88f);

        RelativeLayout mapLayout = (RelativeLayout)findViewById(R.id.mainMapLayout);
        mapLayout.setLayoutParams(layoutParams1);
        mapLayout.requestLayout();
        mapLayout.getParent().requestLayout();

        RouteTta tta = routeData.route.getTta(Route.TrafficPenaltyMode.DISABLED, 0);
        int duration = tta.getDuration();
        Date date = new Date();
        date.setTime(date.getTime() + duration * 1000);
        SimpleDateFormat sdf = new SimpleDateFormat("k:mm", Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+1"));

        ((TextView) findViewById(R.id.routeSummary)).setText(String.format("Distance: %dm. Arrival: %s",
                routeData.route.getLength(), sdf.format(date)));

        ((TextView) findViewById(R.id.routeDestination)).setText(routeData.destination);

        state = "";
    }

    public void startSimulation(View v) {
        map.setCenter(routeData.route.getStart(), Map.Animation.BOW, map.getMaxZoomLevel() - 2.5,
                map.getOrientation(), map.getTilt());

        state = "GoingToDeparture";
    }

    /**
     *   Starts guidance simulation.
     *
     */
    private void startGuidance(Route route) {
        state = "";
        inParkingMode = false;
        parkingFound = false;

        renderedEntities.clear();

        showNavigationUI();

        if (navMan == null) {
            // Setup navigation manager
            navMan = NavigationManager.getInstance();
        }

        attachNavigationListeners();

        navMan.setMap(map);

        navMan.setMapUpdateMode(NavigationManager.MapUpdateMode.POSITION_ANIMATION);

        if(voiceSkin != null) {
            navMan.setVoiceSkin(voiceSkin);
        }

        // Start navigation simulation
        NavigationManager.Error error = navMan.simulate(route, 14);
        if (error != NavigationManager.Error.NONE) {
            Toast.makeText(getApplicationContext(),
                     "Failed to start navigation. Error: " + error, Toast.LENGTH_LONG).show();
            navMan.setMap(null);
            underSimulation = false;
            return;
        }

        // Allow to play the same route again
        popupMenu.getMenu().setGroupVisible(R.id.restartGroup, false);

        underSimulation = true;
        navMan.setNaturalGuidanceMode(
                EnumSet.of(NavigationManager.NaturalGuidanceMode.JUNCTION));
    }

    private Map.OnTransformListener transformListener = new Map.OnTransformListener() {
        @Override
        public void onMapTransformStart() {

        }

        public void onMapTransformEnd(MapState mapState) {
            if(state.equals("zoomToRouteBB")) {
                showRoutePlanningUI();
                // Workaround to deal with resize problems
                map.pan(new PointF(200,200), new PointF(210,150));
            }
            else if(state.equals("GoingToDeparture")) {
                startGuidance(routeData.route);
            }
        }
    };
}