package com.example.tristan.wake_me_go;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.geojson.BoundingBox;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.MapboxMapOptions;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Projection;
import com.mapbox.mapboxsdk.maps.SupportMapFragment;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Wake_me_go extends AppCompatActivity implements
        OnMapReadyCallback, PermissionsListener, LocationEngineListener, MapboxMap.OnMapClickListener{


    private PermissionsManager permissionsManager;
    private MapboxMap mapboxMap;
    private MapView mapView;
    MapboxNavigation navigation;
    private DirectionsRoute currentRoute;
    private Button startButton;
    private static final String TAG = "DirectionsActivity";
    private NavigationMapRoute navigationMapRoute;
    private Point originPosition;
    private Point destinationPosition;
    private Marker destinationMarker;
    private Location originLocation;
    //for searchbar
    private static final int REQUEST_CODE_AUTOCOMPLETE = 1;
    private CarmenFeature home;
    private CarmenFeature work;
    private String geojsonSourceLayerId = "geojsonSourceLayerId";
    private String symbolIconId = "symbolIconId";
    public static final String PREFS_NAME = "MyApp_Settings";

    private double longitude;
    private double latitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

// Mapbox access token is configured here. This needs to be called either in your application
// object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, "pk.eyJ1IjoiYXlhemtoYWxpZDcwIiwiYSI6ImNqajZ2NThlYjBsa20zdm8za3JvdXhkN3AifQ.-Qehv6U-VawnhkcjKNUK2Q");
         navigation = new MapboxNavigation(this, "pk.eyJ1IjoiYXlhemtoYWxpZDcwIiwiYSI6ImNqajZ2NThlYjBsa20zdm8za3JvdXhkN3AifQ.-Qehv6U-VawnhkcjKNUK2Q");
// This contains the MapView in XML and needs to be called after the access token is configured.
        setContentView(R.layout.activity_wake_me_go);

        mapView = findViewById(R.id.mapView);
        startButton = findViewById(R.id.startButton);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

//        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
//
//         //Writing data to SharedPreferences
//        SharedPreferences.Editor editor = settings.edit();
//        editor.putString("longitude", String.valueOf(destinationPosition.longitude()));
//        editor.putString("latitude", String.valueOf(destinationPosition.latitude()));
//        editor.commit();
//
//        // Reading from SharedPreferences
//        String value = settings.getString("key", "");
//        Log.d(TAG, value);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                boolean simulateRoute = true;

// Create a NavigationLauncherOptions object to package everything together
                NavigationLauncherOptions options = NavigationLauncherOptions.builder()
                        .directionsRoute(currentRoute)
                        .shouldSimulateRoute(simulateRoute)
                        .build();

// Call this method with Context from within an Activity
                NavigationLauncher.startNavigation(Wake_me_go.this, options);

            }
        });



    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        mapboxMap.addOnMapClickListener(this);
        enableLocationComponent();

        //for searchbar
        initSearchFab();
        addUserLocations();
/*
        Bitmap icon = BitmapFactory.decodeResource(
                Wake_me_go.this.getResources(), R.drawable.blue_marker_view);
        mapboxMap.addImage(symbolIconId, icon);*/

// Create an empty GeoJSON source using the empty feature collection
        setUpSource();

// Set up a new symbol layer for displaying the searched location's feature coordinates
        setupLayer();

    }

    //for search bar
    private void initSearchFab() {
        FloatingActionButton searchFab = findViewById(R.id.fab_location_search);
        searchFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new PlaceAutocomplete.IntentBuilder()
                        .accessToken(Mapbox.getAccessToken())
                        .placeOptions(PlaceOptions.builder()
                                .backgroundColor(Color.parseColor("#EEEEEE"))
                                .limit(10)
                                .addInjectedFeature(home)
                                .addInjectedFeature(work)
                                .build(PlaceOptions.MODE_CARDS))
                        .build(Wake_me_go.this);
                startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE);
            }
        });
    }

    //for searchbar
    private void addUserLocations() {
        home = CarmenFeature.builder().text("Mapbox SF Office")
                .geometry(Point.fromLngLat(-122.399854, 37.7884400))
                .placeName("85 2nd St, San Francisco, CA")
                .id("mapbox-sf")
                .properties(new JsonObject())
                .build();

        work = CarmenFeature.builder().text("Mapbox DC Office")
                .placeName("740 15th Street NW, Washington DC")
                .geometry(Point.fromLngLat(-77.0338348, 38.899750))
                .id("mapbox-dc")
                .properties(new JsonObject())
                .build();
    }

    //for searchbar
    private void setUpSource() {
        GeoJsonSource geoJsonSource = new GeoJsonSource(geojsonSourceLayerId);
        mapboxMap.addSource(geoJsonSource);
    }

    private void setupLayer() {
        SymbolLayer selectedLocationSymbolLayer = new SymbolLayer("SYMBOL_LAYER_ID", geojsonSourceLayerId);
        selectedLocationSymbolLayer.withProperties(PropertyFactory.iconImage(symbolIconId));
        mapboxMap.addLayer(selectedLocationSymbolLayer);
    }

    //for searchbar
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE) {

// Retrieve selected location's CarmenFeature
            CarmenFeature selectedCarmenFeature = PlaceAutocomplete.getPlace(data);

// Create a new FeatureCollection and add a new Feature to it using selectedCarmenFeature above
            FeatureCollection featureCollection = FeatureCollection.fromFeatures(
                    new Feature[]{Feature.fromJson(selectedCarmenFeature.toJson())});

// Retrieve and update the source designated for showing a selected location's symbol layer icon
            GeoJsonSource source = mapboxMap.getSourceAs(geojsonSourceLayerId);
            if (source != null) {
                source.setGeoJson(featureCollection);
            }

// Move map camera to the selected location
            CameraPosition newCameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(((Point) selectedCarmenFeature.geometry()).latitude(),
                            ((Point) selectedCarmenFeature.geometry()).longitude()))
                    .zoom(14)
                    .build();
            mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(newCameraPosition), 4000);

            if (destinationMarker != null){
                mapboxMap.removeMarker(destinationMarker);
            }

            destinationMarker = mapboxMap.addMarker(new MarkerOptions().position(new LatLng(((Point) selectedCarmenFeature.geometry()).latitude(),
                    ((Point) selectedCarmenFeature.geometry()).longitude())));

            destinationPosition = Point.fromLngLat(((Point) selectedCarmenFeature.geometry()).latitude(),  ((Point) selectedCarmenFeature.geometry()).longitude());
            originPosition = Point.fromLngLat(originLocation.getLongitude(), originLocation.getLatitude());

            Point origin = Point.fromLngLat(originLocation.getLongitude(), originLocation.getLatitude());
            //    Point destination = Point.fromLngLat(120.9589699, 14.2990183);

            NavigationRoute.builder(Wake_me_go.this)
                    .accessToken(Mapbox.getAccessToken())
                    .origin(origin)
                    .destination(destinationPosition)
                    .build()
                    .getRoute(new Callback<DirectionsResponse>() {
                        @Override
                        public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                            Toast.makeText(Wake_me_go.this, "success", Toast.LENGTH_SHORT).show();
                            Log.d("route", "Response code: " + response.code());
                            if (response.body() == null) {
                                Log.e("route", "No routes found, make sure you set the right user and access token.");
                                return;
                            } else if (response.body().routes().size() < 1) {
                                Log.e("route", "No routes found");
                                return;
                            }

                            currentRoute = response.body().routes().get(0);

                            // Draw the route on the map
                            if (navigationMapRoute != null) {
                                navigationMapRoute.removeRoute();
                            } else {
                                navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);
                            }
                            navigationMapRoute.addRoute(currentRoute);
                        }

                        @Override
                        public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                            Log.e("route_error", t.getMessage());
                            Toast.makeText(Wake_me_go.this, "Failure", Toast.LENGTH_SHORT).show();
                        }
                    });

            startButton.setEnabled(true);
            startButton.setBackgroundResource(R.color.mapbox_blue);
        }
    }
    @SuppressWarnings({"MissingPermission"})
    private void enableLocationComponent() {
// Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            LocationComponentOptions options = LocationComponentOptions.builder(this)
                    .trackingGesturesManagement(true)
                    .accuracyColor(ContextCompat.getColor(this, R.color.colorAccent))
                    .build();

// Get an instance of the component
            LocationComponent locationComponent = mapboxMap.getLocationComponent();

// Activate with options
            locationComponent.activateLocationComponent(this, options);

// Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);
// Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);
            locationComponent.setRenderMode(RenderMode.COMPASS);
            LocationEngine locationEngine = new LocationEngineProvider(this).obtainBestLocationEngineAvailable();
            locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
            locationEngine.setInterval(100);
            locationEngine.activate();
            Location location = locationEngine.getLastLocation();
            if (location!=null){
                originLocation=location;
                mapboxMap.setCameraPosition(new CameraPosition.Builder()
                        .target(new LatLng(location.getLatitude(), location.getLongitude()))
                        .zoom(12)
                        .build());
                Toast.makeText(this, "Getting Location", Toast.LENGTH_SHORT).show();
            }

    /*    if (location!= null) {

            } else {
            Toast.makeText(this, "location null", Toast.LENGTH_SHORT).show();
                permissionsManager = new PermissionsManager(this);
                permissionsManager.requestLocationPermissions(this);
            }
*/

        }
        }

    @Override
    public void onMapClick(@NonNull LatLng point) {
        Toast.makeText(this, "getting routes", Toast.LENGTH_SHORT).show();
        if (originLocation!=null){

            if (destinationMarker != null){
                mapboxMap.removeMarker(destinationMarker);
            }
            destinationMarker = mapboxMap.addMarker(new MarkerOptions().position(point));

            destinationPosition = Point.fromLngLat(point.getLongitude(), point.getLatitude());
            originPosition = Point.fromLngLat(originLocation.getLongitude(), originLocation.getLatitude());

            Point origin = Point.fromLngLat(originLocation.getLongitude(), originLocation.getLatitude());
            //    Point destination = Point.fromLngLat(120.9589699, 14.2990183);

            NavigationRoute.builder(Wake_me_go.this)
                    .accessToken(Mapbox.getAccessToken())
                    .origin(origin)
                    .destination(destinationPosition)
                    .build()
                    .getRoute(new Callback<DirectionsResponse>() {
                        @Override
                        public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                            Toast.makeText(Wake_me_go.this, "success", Toast.LENGTH_SHORT).show();
                            Log.d("route", "Response code: " + response.code());
                            if (response.body() == null) {
                                Log.e("route", "No routes found, make sure you set the right user and access token.");
                                return;
                            } else if (response.body().routes().size() < 1) {
                                Log.e("route", "No routes found");
                                return;
                            }

                            currentRoute = response.body().routes().get(0);

                            // Draw the route on the map
                            if (navigationMapRoute != null) {
                                navigationMapRoute.removeRoute();
                            } else {
                                navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);
                            }
                            navigationMapRoute.addRoute(currentRoute);
                        }

                        @Override
                        public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                            Log.e("route_error", t.getMessage());
                            Toast.makeText(Wake_me_go.this, "Failure", Toast.LENGTH_SHORT).show();
                        }
                    });

            startButton.setEnabled(true);
            startButton.setBackgroundResource(R.color.mapbox_blue);
        }else {
            Toast.makeText(this, "Can't get Location", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, "Permission required", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationComponent();
        } else {
            Toast.makeText(this, "not granted", Toast.LENGTH_LONG).show();
            finish();
        }
    }



    @Override
    @SuppressWarnings( {"MissingPermission"})
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onConnected() {

    }

    @Override
    public void onLocationChanged(Location location) {
        Toast.makeText(this, "Location Changed", Toast.LENGTH_SHORT).show();
        originLocation=location;
  /*      mapboxMap.setCameraPosition(new CameraPosition.Builder()
                .target(new LatLng(location.getLatitude(), location.getLongitude()))
                .zoom(14)
                .build());
        Point origin = Point.fromLngLat(location.getLongitude(), location.getLatitude());
        Point destination = Point.fromLngLat(120.9589699, 14.2990183);

        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        Toast.makeText(Wake_me_go.this, "success", Toast.LENGTH_SHORT).show();
                        Log.d("route", "Response code: " + response.code());
                        if (response.body() == null) {
                            Log.e("route", "No routes found, make sure you set the right user and access token.");
                            return;
                        } else if (response.body().routes().size() < 1) {
                            Log.e("route", "No routes found");
                            return;
                        }

                        currentRoute = response.body().routes().get(0);

                        // Draw the route on the map
                        if (navigationMapRoute != null) {
                            navigationMapRoute.removeRoute();
                        } else {
                            navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);
                        }
                        navigationMapRoute.addRoute(currentRoute);
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                        Log.e("route_error", t.getMessage());
                        Toast.makeText(Wake_me_go.this, "Failure", Toast.LENGTH_SHORT).show();
                    }
                });
        Toast.makeText(this, "Location component running", Toast.LENGTH_SHORT).show();*/

    }
}